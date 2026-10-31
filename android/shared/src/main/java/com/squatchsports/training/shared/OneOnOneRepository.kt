package com.squatchsports.training.shared

import org.json.JSONArray
import org.json.JSONObject

interface MatchStorage {
    fun read(): String?
    /** Must persist the entire value atomically before returning (including processed command IDs). */
    fun write(value: String)
}

data class OneOnOneData(
    val playerName: String = "You",
    val friends: List<MatchFriend> = emptyList(),
    val matches: List<OneOnOneMatch> = emptyList(),
    val activeMatch: OneOnOneMatch? = null,
    val processedCommandIds: Set<String> = emptySet(),
    val revision: Long = 0,
    val nearbySession: NearbyMatchSession? = null,
    val closedNearbyMatchIds: Set<String> = emptySet(),
) {
    val overallRecord: HeadToHeadRecord get() = HeadToHeadRecord.forMatches(matches)
    fun recordAgainst(friendId: String): HeadToHeadRecord =
        HeadToHeadRecord.forMatches(matches.filter { it.opponentId == friendId })

    val watchUpdate: WatchMatchUpdate
        get() = WatchMatchUpdate(revision, activeMatch?.let { match ->
            WatchMatchSnapshot(
                match.id, match.userSide, playerName, match.opponentName, match.settings, match.state,
                match.events.lastOrNull()?.event?.summary { match.name(it, playerName) }, match.events.isNotEmpty(),
            )
        }, processedCommandIds)
}

/** Single authoritative phone store, also used by the background wearable service. Call on one thread. */
class OneOnOneRepository(private val storage: MatchStorage) {
    // A fresh install must also supersede a scoreboard cached by an already-paired watch.
    var data: OneOnOneData = storage.read()?.let(OneOnOnePersistence::decode)
        ?: OneOnOneData(revision = System.currentTimeMillis())
        private set
    var onChange: ((OneOnOneData) -> Unit)? = null

    fun setPlayerName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) commit(data.copy(playerName = trimmed))
    }

    fun addFriend(name: String): MatchFriend? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || data.friends.any { it.name.equals(trimmed, ignoreCase = true) }) return null
        val friend = MatchFriend(trimmed)
        commit(data.copy(friends = data.friends + friend))
        return friend
    }

    fun removeFriend(id: String) { commit(data.copy(friends = data.friends.filterNot { it.id == id })) }

    fun startMatch(opponentId: String, userSide: PlayerSide, settings: MatchSettings): Boolean {
        if (data.activeMatch != null) return false
        val opponent = data.friends.firstOrNull { it.id == opponentId } ?: return false
        commit(data.copy(
            activeMatch = OneOnOneMatch(opponent.id, opponent.name, userSide, settings),
            processedCommandIds = emptySet(),
            nearbySession = null,
        ))
        return true
    }

    fun record(event: MatchEvent) { commit(data.copy(activeMatch = data.activeMatch?.record(event))) }
    fun undo() { commit(data.copy(activeMatch = data.activeMatch?.undo())) }

    /** Only finished games enter the record. An accidental premature watch Save cannot discard a game. */
    fun finishMatch() { commit(finished(data)) }
    fun cancelMatch() { commit(cancelled(data)) }

    /** Explicit local escape hatch if a guest can no longer contact its host; does not cancel remotely. */
    fun abandonNearbyMatch() {
        if (data.nearbySession?.role == NearbyMatchRole.GUEST && data.activeMatch != null) commit(cancelled(data))
    }

    fun startNearbyMatch(peer: NearbyPlayerIdentity, side: PlayerSide, settings: MatchSettings): Boolean {
        if (data.activeMatch != null || peer.name.isBlank()) return false
        val friend = data.friends.firstOrNull { it.name.equals(peer.name.trim(), ignoreCase = true) }
            ?: MatchFriend(peer.name.trim())
        val match = OneOnOneMatch(friend.id, friend.name, side, settings)
        commit(data.copy(activeMatch = match, processedCommandIds = emptySet(),
            friends = if (friend in data.friends) data.friends else data.friends + friend,
            nearbySession = NearbyMatchSession(peer.id, match.id, NearbyMatchRole.HOST, data.playerName, friend.name)))
        return true
    }

    /** Guest phone and watch commands stay durable until the host acknowledges them. */
    fun queueNearbyCommands(commands: List<WatchMatchCommand>) {
        val session = data.nearbySession ?: return
        if (session.role != NearbyMatchRole.GUEST || session.status != NearbyMatchStatus.ACTIVE) return
        val known = data.processedCommandIds + session.pendingCommands.map { it.id }
        val additions = commands.filter { it.matchId == session.matchId && it.id !in known }.distinctBy { it.id }
        if (additions.isNotEmpty()) commit(data.copy(nearbySession = session.copy(pendingCommands = session.pendingCommands + additions)))
    }

    /** Import without echoing plays, retaining this phone's friend IDs and opposite player perspective. */
    fun applyNearbySnapshot(peerId: String, value: NearbyGameSnapshot): Boolean {
        val session = data.nearbySession
        val sameGame = session?.peerId == peerId && session.matchId == value.match.id && session.role == NearbyMatchRole.GUEST
        if (value.match.id in data.closedNearbyMatchIds) return false
        if (sameGame && value.revision <= session!!.lastSnapshotRevision) return false
        if (!sameGame && (data.activeMatch != null || value.status != NearbyMatchStatus.ACTIVE)) return false
        if (value.hostName.isBlank() || value.guestName.isBlank()) return false
        if (value.status == NearbyMatchStatus.FINISHED && !value.match.state.isOver) return false
        val friend = data.friends.firstOrNull { it.name.equals(value.hostName.trim(), ignoreCase = true) }
            ?: MatchFriend(value.hostName.trim())
        val localMatch = value.match.copy(opponentId = friend.id, opponentName = friend.name, userSide = value.match.userSide.opponent)
        val pending = if (sameGame && value.status == NearbyMatchStatus.ACTIVE)
            session!!.pendingCommands.filterNot { it.id in value.acknowledgedIds } else emptyList()
        commit(data.copy(
            friends = if (friend in data.friends) data.friends else data.friends + friend,
            activeMatch = localMatch.takeIf { value.status == NearbyMatchStatus.ACTIVE },
            matches = if (value.status == NearbyMatchStatus.FINISHED)
                listOf(localMatch) + data.matches.filterNot { it.id == localMatch.id } else data.matches,
            processedCommandIds = (if (sameGame) data.processedCommandIds else emptySet()) + value.acknowledgedIds,
            nearbySession = NearbyMatchSession(peerId, localMatch.id, NearbyMatchRole.GUEST, value.hostName, value.guestName,
                value.status, value.revision, pending, localMatch.takeIf { value.status != NearbyMatchStatus.ACTIVE }),
            closedNearbyMatchIds = if (value.status != NearbyMatchStatus.ACTIVE)
                data.closedNearbyMatchIds + localMatch.id else data.closedNearbyMatchIds,
        ))
        return true
    }

    /** A batch, its effects, and its deduplication IDs are committed together, surviving process death. */
    fun handleCommands(commands: List<WatchMatchCommand>) {
        var next = data
        commands.forEach { command ->
            if (command.id in next.processedCommandIds || command.matchId != next.activeMatch?.id) return@forEach
            next = when (command.action) {
                MatchCommandAction.RECORD -> next.copy(activeMatch = next.activeMatch?.record(command.event!!))
                MatchCommandAction.UNDO -> next.copy(activeMatch = next.activeMatch?.undo())
                MatchCommandAction.FINISH -> finished(next)
                MatchCommandAction.CANCEL -> cancelled(next)
            }
            next = next.copy(processedCommandIds = next.processedCommandIds + command.id)
        }
        commit(next)
    }

    private fun finished(value: OneOnOneData): OneOnOneData {
        val match = value.activeMatch ?: return value
        if (!match.state.isOver) return value
        return value.copy(matches = listOf(match) + value.matches, activeMatch = null,
            nearbySession = value.nearbySession?.copy(status = NearbyMatchStatus.FINISHED, terminalMatch = match, pendingCommands = emptyList()),
            closedNearbyMatchIds = value.closedNearbyMatchIds + listOfNotNull(value.nearbySession?.matchId))
    }

    private fun cancelled(value: OneOnOneData): OneOnOneData = value.copy(activeMatch = null,
        nearbySession = value.nearbySession?.copy(status = NearbyMatchStatus.CANCELLED,
            terminalMatch = value.activeMatch ?: value.nearbySession.terminalMatch, pendingCommands = emptyList()),
        closedNearbyMatchIds = value.closedNearbyMatchIds + listOfNotNull(value.nearbySession?.matchId))

    private fun commit(value: OneOnOneData) {
        if (value == data) return
        val next = value.copy(revision = maxOf(data.revision + 1, System.currentTimeMillis()))
        storage.write(OneOnOnePersistence.encode(next))
        data = next
        onChange?.invoke(next)
    }
}

object OneOnOnePersistence {
    internal fun match(value: OneOnOneMatch): JSONObject = JSONObject()
        .put("id", value.id).put("opponentId", value.opponentId).put("opponentName", value.opponentName)
        .put("userSide", value.userSide.name).put("settings", MatchPayloads.settings(value.settings))
        .put("start", value.startDateMillis).put("end", value.endDateMillis ?: JSONObject.NULL)
        .put("events", JSONArray().apply {
            value.events.forEach { logged ->
                put(JSONObject().put("id", logged.id).put("date", logged.dateMillis)
                    .put("event", MatchPayloads.event(logged.event)))
            }
        })

    internal fun readMatch(json: JSONObject): OneOnOneMatch {
        val events = json.getJSONArray("events")
        return OneOnOneMatch(
            opponentId = json.getString("opponentId"), opponentName = json.getString("opponentName"),
            userSide = PlayerSide.valueOf(json.getString("userSide")), settings = MatchPayloads.readSettings(json.getJSONObject("settings")),
            id = json.getString("id"), startDateMillis = json.getLong("start"),
            endDateMillis = if (json.isNull("end")) null else json.getLong("end"),
            events = (0 until events.length()).map { index ->
                val logged = events.getJSONObject(index)
                LoggedMatchEvent(MatchPayloads.readEvent(logged.getJSONObject("event")), logged.getString("id"), logged.getLong("date"))
            },
        )
    }

    fun encode(value: OneOnOneData): String = JSONObject()
        .put("version", 1).put("playerName", value.playerName).put("revision", value.revision)
        .put("friends", JSONArray().apply {
            value.friends.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("added", it.addedDateMillis)) }
        })
        .put("matches", JSONArray().apply { value.matches.forEach { put(match(it)) } })
        .put("activeMatch", value.activeMatch?.let(::match) ?: JSONObject.NULL)
        .put("processedIds", JSONArray(value.processedCommandIds.toList()))
        .put("nearby", value.nearbySession?.let(NearbyMatchPayloads::session) ?: JSONObject.NULL)
        .put("closedNearbyIds", JSONArray(value.closedNearbyMatchIds.toList())).toString()

    fun decode(value: String): OneOnOneData? = runCatching {
        val json = JSONObject(value)
        require(json.getInt("version") == 1)
        val friends = json.getJSONArray("friends")
        val matches = json.getJSONArray("matches")
        val ids = json.getJSONArray("processedIds")
        OneOnOneData(
            playerName = json.getString("playerName"), revision = json.getLong("revision"),
            friends = (0 until friends.length()).map { index ->
                val friend = friends.getJSONObject(index)
                MatchFriend(friend.getString("name"), friend.getString("id"), friend.getLong("added"))
            },
            matches = (0 until matches.length()).map { readMatch(matches.getJSONObject(it)) },
            activeMatch = if (json.isNull("activeMatch")) null else readMatch(json.getJSONObject("activeMatch")),
            processedCommandIds = (0 until ids.length()).map { ids.getString(it) }.toSet(),
            nearbySession = if (!json.has("nearby") || json.isNull("nearby")) null else NearbyMatchPayloads.readSession(json.getJSONObject("nearby")),
            closedNearbyMatchIds = json.optJSONArray("closedNearbyIds")?.let { closed ->
                (0 until closed.length()).map { closed.getString(it) }.toSet()
            } ?: emptySet(),
        )
    }.getOrNull()
}
