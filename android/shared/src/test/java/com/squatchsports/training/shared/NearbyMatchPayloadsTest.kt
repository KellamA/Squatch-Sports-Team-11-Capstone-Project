package com.squatchsports.training.shared

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NearbyMatchPayloadsTest {
    private fun snapshot(status: NearbyMatchStatus = NearbyMatchStatus.ACTIVE): NearbyGameSnapshot {
        var match = OneOnOneMatch("friend", "Kaleb", PlayerSide.BLUE, MatchSettings(targetScore = 5, winByTwo = false))
        repeat(if (status == NearbyMatchStatus.FINISHED) 3 else 1) { match = match.record(MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 2)) }
        return NearbyGameSnapshot(match, "Melvin", "Kaleb", status, 123, setOf("command"))
    }

    @Test fun everyMessageAndTerminalStatusRoundTrips() {
        for (status in NearbyMatchStatus.entries) {
            val message = NearbyMatchMessage.Snapshot(snapshot(status))
            assertEquals(message, NearbyMatchPayloads.decode(NearbyMatchPayloads.encode(message)))
        }
        val commands = MatchCommandAction.entries.map { action ->
            WatchMatchCommand("match", action, if (action == MatchCommandAction.RECORD) MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2) else null)
        }
        for (message in listOf(NearbyMatchMessage.Commands(commands), NearbyMatchMessage.Request("match"), NearbyMatchMessage.Request(null))) {
            assertEquals(message, NearbyMatchPayloads.decode(NearbyMatchPayloads.encode(message)))
        }
    }

    @Test fun malformedUnknownAndOversizedPacketsAreRejected() {
        for (input in listOf("", "[]", "{}", "{\"version\":999,\"type\":\"request\",\"matchId\":null}", "{\"version\":1,\"type\":\"eraseEverything\"}")) {
            assertNull(NearbyMatchPayloads.decode(input.encodeToByteArray()))
        }
        assertNull(NearbyMatchPayloads.decode(ByteArray(NearbyMatchPayloads.MAX_PAYLOAD_BYTES + 1)))
    }

    @Test fun unfinishedGameCannotPretendToBeFinished() {
        val message = NearbyMatchMessage.Snapshot(snapshot().copy(status = NearbyMatchStatus.FINISHED))
        assertNull(NearbyMatchPayloads.decode(NearbyMatchPayloads.encode(message)))
    }

    @Test fun invalidPointsAndNegativeRevisionsAreRejected() {
        val value = snapshot()
        val invalidMatch = value.match.copy(events = listOf(LoggedMatchEvent(MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 3))))
        assertNull(NearbyMatchPayloads.decode(NearbyMatchPayloads.encode(NearbyMatchMessage.Snapshot(value.copy(match = invalidMatch)))))
        assertNull(NearbyMatchPayloads.decode(NearbyMatchPayloads.encode(NearbyMatchMessage.Snapshot(value.copy(revision = -1)))))
    }

    @Test fun unicodeEndpointNamesRespectNearbyByteLimitAndKeepStableIdentity() {
        val player = NearbyPlayerIdentity(UUID.randomUUID().toString(), "🏀🔥".repeat(100))
        val encoded = NearbyMatchPayloads.identity(player)
        assertTrue(encoded.encodeToByteArray().size <= 131)
        val decoded = NearbyMatchPayloads.decodeIdentity(encoded)!!
        assertEquals(player.id, decoded.id)
        assertTrue(decoded.name.isNotBlank())
        assertTrue(player.name.startsWith(decoded.name))
    }

    @Test fun unknownEndpointMetadataIsIgnoredAndBlankNamesHaveFallback() {
        assertNull(NearbyMatchPayloads.decodeIdentity("Some other app"))
        assertNull(NearbyMatchPayloads.decodeIdentity("{\"v\":1,\"id\":\"not-an-id\",\"n\":\"Player\"}"))
        assertEquals("Squatch Player", NearbyMatchPayloads.decodeIdentity(NearbyMatchPayloads.identity(
            NearbyPlayerIdentity(UUID.randomUUID().toString(), " ")))!!.name)
    }

    @Test fun durableNearbySessionAndQueueRoundTripAndOlderStorageStillLoads() {
        val value = snapshot()
        val session = NearbyMatchSession("peer", value.match.id, NearbyMatchRole.GUEST, "Melvin", "Kaleb",
            pendingCommands = listOf(WatchMatchCommand(value.match.id, MatchCommandAction.UNDO)))
        val data = OneOnOneData(activeMatch = value.match, nearbySession = session)
        assertEquals(data, OneOnOnePersistence.decode(OneOnOnePersistence.encode(data)))
        val old = JSONObject(OneOnOnePersistence.encode(data.copy(nearbySession = null))).apply { remove("nearby") }.toString()
        assertEquals(data.copy(nearbySession = null), OneOnOnePersistence.decode(old))
    }

    @Test fun largeGameUsesSameProtocolBeyondNearbyBytesLimit() {
        val value = snapshot()
        val match = value.match.copy(events = List(600) { LoggedMatchEvent(MatchEvent(MatchEventType.FOUL, PlayerSide.RED)) })
        val message = NearbyMatchMessage.Snapshot(value.copy(match = match))
        val encoded = NearbyMatchPayloads.encode(message)
        assertTrue(encoded.size > 32 * 1024)
        assertEquals(message, NearbyMatchPayloads.decode(encoded))
    }
}
