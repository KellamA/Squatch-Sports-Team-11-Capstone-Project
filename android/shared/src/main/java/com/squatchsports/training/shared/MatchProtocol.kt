package com.squatchsports.training.shared

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object MatchPaths {
    const val PREFIX = "/one-on-one"
    const val STATE = "$PREFIX/state"
    const val COMMANDS = "$PREFIX/commands"
    const val REQUEST = "$PREFIX/request"
}

enum class MatchCommandAction { RECORD, UNDO, FINISH, CANCEL }

data class WatchMatchCommand(
    val matchId: String,
    val action: MatchCommandAction,
    val event: MatchEvent? = null,
    val id: String = UUID.randomUUID().toString(),
) {
    init {
        require(matchId.isNotBlank() && id.isNotBlank())
        require((action == MatchCommandAction.RECORD) == (event != null))
    }
}

data class WatchMatchSnapshot(
    val matchId: String,
    val userSide: PlayerSide,
    val userName: String,
    val opponentName: String,
    val settings: MatchSettings,
    val state: MatchState,
    val lastPlay: String?,
    val canUndo: Boolean,
) {
    fun name(side: PlayerSide): String = if (side == userSide) userName else opponentName
}

/** Revision is persisted by the phone. A null snapshot explicitly clears a saved/cancelled game. */
data class WatchMatchUpdate(
    val revision: Long,
    val match: WatchMatchSnapshot?,
    val acknowledgedIds: Set<String> = emptySet(),
)

/** Shared JSON schema for persistence and the Data Layer. Invalid payloads never become plays. */
object MatchPayloads {
    internal fun settings(value: MatchSettings): JSONObject = JSONObject()
        .put("scoring", value.scoring.name).put("target", value.targetScore)
        .put("winByTwo", value.winByTwo).put("makeItTakeIt", value.makeItTakeIt)
        .put("firstPossession", value.firstPossession.name)

    internal fun readSettings(json: JSONObject) = MatchSettings(
        ScoringSystem.valueOf(json.getString("scoring")), json.getInt("target"),
        json.getBoolean("winByTwo"), json.getBoolean("makeItTakeIt"),
        PlayerSide.valueOf(json.getString("firstPossession")),
    )

    internal fun event(value: MatchEvent): JSONObject = JSONObject()
        .put("type", value.type.name).put("side", value.side.name).put("points", value.points)

    internal fun readEvent(json: JSONObject) = MatchEvent(
        MatchEventType.valueOf(json.getString("type")), PlayerSide.valueOf(json.getString("side")), json.getInt("points"),
    )

    private fun command(value: WatchMatchCommand): JSONObject = JSONObject()
        .put("id", value.id).put("matchId", value.matchId).put("action", value.action.name)
        .put("event", value.event?.let(::event) ?: JSONObject.NULL)

    private fun readCommand(json: JSONObject) = WatchMatchCommand(
        matchId = json.getString("matchId"), action = MatchCommandAction.valueOf(json.getString("action")),
        event = if (json.isNull("event")) null else readEvent(json.getJSONObject("event")), id = json.getString("id"),
    )

    fun commands(values: List<WatchMatchCommand>): ByteArray = JSONArray().apply {
        values.forEach { put(command(it)) }
    }.toString().encodeToByteArray()

    fun decodeCommands(data: ByteArray): List<WatchMatchCommand>? = runCatching {
        val json = JSONArray(data.decodeToString())
        (0 until json.length()).map { readCommand(json.getJSONObject(it)) }
    }.getOrNull()

    private fun tally(value: PlayerTally): JSONObject = JSONObject()
        .put("points", value.points).put("baskets", value.baskets).put("fouls", value.fouls)
        .put("turnovers", value.turnovers).put("steals", value.steals)

    private fun readTally(json: JSONObject) = PlayerTally(
        json.getInt("points"), json.getInt("baskets"), json.getInt("fouls"),
        json.getInt("turnovers"), json.getInt("steals"),
    ).also { require(listOf(it.points, it.baskets, it.fouls, it.turnovers, it.steals).all { n -> n >= 0 }) }

    fun update(value: WatchMatchUpdate): ByteArray = JSONObject().apply {
        put("revision", value.revision)
        put("acknowledgedIds", JSONArray(value.acknowledgedIds.toList()))
        put("match", value.match?.let { snapshot ->
            JSONObject().put("id", snapshot.matchId).put("userSide", snapshot.userSide.name)
                .put("userName", snapshot.userName).put("opponentName", snapshot.opponentName)
                .put("settings", settings(snapshot.settings)).put("red", tally(snapshot.state.red))
                .put("blue", tally(snapshot.state.blue)).put("possession", snapshot.state.possession.name)
                .put("winner", snapshot.state.winner?.name ?: JSONObject.NULL)
                .put("lastPlay", snapshot.lastPlay ?: JSONObject.NULL).put("canUndo", snapshot.canUndo)
        } ?: JSONObject.NULL)
    }.toString().encodeToByteArray()

    fun decodeUpdate(data: ByteArray): WatchMatchUpdate? = runCatching {
        val json = JSONObject(data.decodeToString())
        val revision = json.getLong("revision").also { require(it >= 0) }
        val snapshot = if (json.isNull("match")) null else json.getJSONObject("match").let {
            WatchMatchSnapshot(
                matchId = it.getString("id"), userSide = PlayerSide.valueOf(it.getString("userSide")),
                userName = it.getString("userName"), opponentName = it.getString("opponentName"),
                settings = readSettings(it.getJSONObject("settings")),
                state = MatchState(
                    readTally(it.getJSONObject("red")), readTally(it.getJSONObject("blue")),
                    PlayerSide.valueOf(it.getString("possession")),
                    if (it.isNull("winner")) null else PlayerSide.valueOf(it.getString("winner")),
                ),
                lastPlay = if (it.isNull("lastPlay")) null else it.getString("lastPlay"), canUndo = it.getBoolean("canUndo"),
            )
        }
        val ids = json.getJSONArray("acknowledgedIds")
        WatchMatchUpdate(revision, snapshot, (0 until ids.length()).map { ids.getString(it) }.toSet())
    }.getOrNull()
}

/** Durable FIFO outbox. Only an application-level acknowledgement removes a play, not a successful send. */
class MatchCommandOutbox(
    saved: List<WatchMatchCommand> = emptyList(),
    private val persist: (List<WatchMatchCommand>) -> Unit,
) {
    var pending: List<WatchMatchCommand> = saved
        private set

    fun enqueue(command: WatchMatchCommand) {
        if (pending.any { it.id == command.id }) return
        replace(pending + command)
    }

    fun acknowledge(update: WatchMatchUpdate) {
        replace(pending.filter { it.id !in update.acknowledgedIds && it.matchId == update.match?.matchId })
    }

    private fun replace(next: List<WatchMatchCommand>) {
        if (next == pending) return
        persist(next)
        pending = next
    }
}
