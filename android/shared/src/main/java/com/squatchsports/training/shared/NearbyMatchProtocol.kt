package com.squatchsports.training.shared

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class NearbyMatchRole { HOST, GUEST }
enum class NearbyMatchStatus { ACTIVE, FINISHED, CANCELLED }

/** Persisted with the match so a guest never becomes an independent scorer after a restart. */
data class NearbyMatchSession(
    val peerId: String,
    val matchId: String,
    val role: NearbyMatchRole,
    val hostName: String,
    val guestName: String,
    val status: NearbyMatchStatus = NearbyMatchStatus.ACTIVE,
    val lastSnapshotRevision: Long = 0,
    val pendingCommands: List<WatchMatchCommand> = emptyList(),
    val terminalMatch: OneOnOneMatch? = null,
)

/** The host's complete event log is authoritative, including undo, save and cancellation. */
data class NearbyGameSnapshot(
    val match: OneOnOneMatch,
    val hostName: String,
    val guestName: String,
    val status: NearbyMatchStatus,
    val revision: Long,
    val acknowledgedIds: Set<String>,
)

sealed interface NearbyMatchMessage {
    data class Snapshot(val value: NearbyGameSnapshot) : NearbyMatchMessage
    data class Commands(val values: List<WatchMatchCommand>) : NearbyMatchMessage
    data class Request(val matchId: String?) : NearbyMatchMessage
}

data class NearbyPlayerIdentity(val id: String, val name: String)

/** Versioned Android-to-Android protocol. Apple's MultipeerConnectivity is not wire-compatible. */
object NearbyMatchPayloads {
    const val MAX_PAYLOAD_BYTES = 1_048_576
    private const val VERSION = 1

    fun identity(value: NearbyPlayerIdentity): String {
        UUID.fromString(value.id)
        var name = value.name.trim().ifBlank { "Squatch Player" }
        fun json() = JSONObject().put("v", VERSION).put("id", value.id).put("n", name).toString()
        // Nearby endpoint names have a byte limit, not a character limit (emoji count too).
        while (json().encodeToByteArray().size > 131) {
            name = name.substring(0, name.offsetByCodePoints(name.length, -1))
        }
        return json()
    }

    fun decodeIdentity(value: String): NearbyPlayerIdentity? = runCatching {
        val json = JSONObject(value)
        require(json.getInt("v") == VERSION)
        val id = json.getString("id").also { UUID.fromString(it) }
        val name = json.getString("n").trim().also { require(it.isNotEmpty()) }
        NearbyPlayerIdentity(id, name)
    }.getOrNull()

    internal fun session(value: NearbyMatchSession): JSONObject = JSONObject()
        .put("peerId", value.peerId).put("matchId", value.matchId).put("role", value.role.name)
        .put("hostName", value.hostName).put("guestName", value.guestName).put("status", value.status.name)
        .put("revision", value.lastSnapshotRevision)
        .put("pending", JSONArray(MatchPayloads.commands(value.pendingCommands).decodeToString()))
        .put("terminal", value.terminalMatch?.let(OneOnOnePersistence::match) ?: JSONObject.NULL)

    internal fun readSession(json: JSONObject) = NearbyMatchSession(
        peerId = json.getString("peerId"), matchId = json.getString("matchId"),
        role = NearbyMatchRole.valueOf(json.getString("role")), hostName = json.getString("hostName"),
        guestName = json.getString("guestName"), status = NearbyMatchStatus.valueOf(json.getString("status")),
        lastSnapshotRevision = json.getLong("revision"),
        pendingCommands = requireNotNull(MatchPayloads.decodeCommands(json.getJSONArray("pending").toString().encodeToByteArray())),
        terminalMatch = if (json.isNull("terminal")) null else OneOnOnePersistence.readMatch(json.getJSONObject("terminal")),
    )

    fun encode(message: NearbyMatchMessage): ByteArray = JSONObject().apply {
        put("version", VERSION)
        when (message) {
            is NearbyMatchMessage.Snapshot -> {
                val value = message.value
                put("type", "snapshot").put("match", OneOnOnePersistence.match(value.match))
                put("hostName", value.hostName).put("guestName", value.guestName)
                put("status", value.status.name).put("revision", value.revision)
                put("ack", JSONArray(value.acknowledgedIds.toList()))
            }
            is NearbyMatchMessage.Commands -> {
                put("type", "commands").put("commands", JSONArray(MatchPayloads.commands(message.values).decodeToString()))
            }
            is NearbyMatchMessage.Request -> put("type", "request").put("matchId", message.matchId ?: JSONObject.NULL)
        }
    }.toString().encodeToByteArray().also { require(it.size <= MAX_PAYLOAD_BYTES) }

    fun decode(data: ByteArray): NearbyMatchMessage? = runCatching {
        require(data.size <= MAX_PAYLOAD_BYTES)
        val json = JSONObject(data.decodeToString())
        require(json.getInt("version") == VERSION)
        when (json.getString("type")) {
            "snapshot" -> {
                val match = OneOnOnePersistence.readMatch(json.getJSONObject("match"))
                require(match.id.isNotBlank() && match.events.all { MatchEngine.isValid(it.event, match.settings) })
                val status = NearbyMatchStatus.valueOf(json.getString("status"))
                require(status != NearbyMatchStatus.FINISHED || match.state.isOver)
                val revision = json.getLong("revision").also { require(it > 0) }
                val host = json.getString("hostName").also { require(it.isNotBlank()) }
                val guest = json.getString("guestName").also { require(it.isNotBlank()) }
                val ids = json.getJSONArray("ack")
                NearbyMatchMessage.Snapshot(NearbyGameSnapshot(match, host, guest, status, revision,
                    (0 until ids.length()).map { ids.getString(it) }.toSet()))
            }
            "commands" -> NearbyMatchMessage.Commands(requireNotNull(MatchPayloads.decodeCommands(
                json.getJSONArray("commands").toString().encodeToByteArray())))
            "request" -> NearbyMatchMessage.Request(if (json.isNull("matchId")) null else json.getString("matchId"))
            else -> error("Unknown nearby message")
        }
    }.getOrNull()
}
