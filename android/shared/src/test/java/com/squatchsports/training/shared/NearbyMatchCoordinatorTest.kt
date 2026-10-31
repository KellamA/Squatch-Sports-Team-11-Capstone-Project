package com.squatchsports.training.shared

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Two real repositories/coordinators communicating through the serialized wire protocol. */
class NearbyMatchCoordinatorTest {
    private data class Packet(val from: String, val to: String, val message: NearbyMatchMessage)
    private val packets = mutableListOf<Packet>()
    private val nodes = mutableMapOf<String, Node>()
    private var guestPackets = 0

    private inner class Node(val identity: NearbyPlayerIdentity, val storage: MemoryMatchStorage = MemoryMatchStorage()) {
        val repository = OneOnOneRepository(storage)
        val coordinator = NearbyMatchCoordinator(repository) { peer, message ->
            if (identity.id == guest?.identity?.id) guestPackets++
            packets.add(Packet(identity.id, peer, requireNotNull(NearbyMatchPayloads.decode(NearbyMatchPayloads.encode(message)))))
        }
        init { repository.setPlayerName(identity.name); repository.onChange = { coordinator.onRepositoryChange() }; nodes[identity.id] = this }
        val match: OneOnOneMatch get() = repository.data.activeMatch!!
        fun play(side: PlayerSide, points: Int = 2) = coordinator.perform(MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, side, points))
    }
    private var guest: Node? = null
    private val host = Node(NearbyPlayerIdentity(UUID.randomUUID().toString(), "Melvin"))

    private fun connect(start: Boolean = true, side: PlayerSide = PlayerSide.RED, settings: MatchSettings = MatchSettings()): Node {
        val other = guest ?: Node(NearbyPlayerIdentity(UUID.randomUUID().toString(), "Kaleb")).also { guest = it }
        host.coordinator.connected(other.identity, isHost = true)
        other.coordinator.connected(host.identity, isHost = false)
        drain()
        if (start) { assertTrue(host.coordinator.startMatch(side, settings)); drain() }
        return other
    }
    private fun drain() {
        var count = 0
        while (packets.isNotEmpty()) {
            check(count++ < 10_000) { "Protocol echo loop" }
            val packet = packets.removeAt(0)
            nodes[packet.to]?.coordinator?.receive(packet.from, packet.message)
        }
    }
    private fun assertSameGame(other: Node) {
        assertEquals(host.match.id, other.match.id)
        assertEquals(host.match.events, other.match.events)
        assertEquals(host.match.state, other.match.state)
    }

    @Test fun hostStartsSameGameWithOppositePlayerPerspectiveAndFriendRecords() {
        val other = connect(side = PlayerSide.BLUE, settings = MatchSettings(scoring = ScoringSystem.TWOS_AND_THREES, targetScore = 21))
        assertSameGame(other)
        assertEquals(PlayerSide.BLUE, host.match.userSide)
        assertEquals(PlayerSide.RED, other.match.userSide)
        assertEquals("Kaleb", host.match.opponentName)
        assertEquals("Melvin", other.match.opponentName)
        assertEquals(1, other.repository.data.friends.size)
        assertFalse(other.coordinator.canHost)
    }

    @Test fun guestCannotStartOrOverwriteHostsGame() {
        val other = connect(start = false)
        assertFalse(other.coordinator.startMatch(PlayerSide.BLUE, MatchSettings()))
        assertTrue(host.coordinator.startMatch(PlayerSide.RED, MatchSettings()))
        drain()
        assertFalse(host.coordinator.startMatch(PlayerSide.BLUE, MatchSettings()))
        assertSameGame(other)
    }

    @Test fun phonePlaysOnBothSidesAndUndoConvergeWithoutEcho() {
        val other = connect()
        host.play(PlayerSide.RED)
        drain()
        val before = guestPackets
        host.play(PlayerSide.RED, 1)
        drain()
        assertEquals(before, guestPackets) // Received host snapshots are not broadcast back as plays.
        other.play(PlayerSide.BLUE)
        drain()
        assertEquals(2, host.match.state.blue.points)
        other.coordinator.perform(MatchCommandAction.UNDO)
        drain()
        assertEquals(0, host.match.state.blue.points)
        assertEquals(3, host.match.state.red.points)
        assertSameGame(other)
    }

    @Test fun simultaneousInputsAreOrderedByHostAndDuplicatesAreHarmless() {
        val other = connect()
        host.play(PlayerSide.RED)
        other.play(PlayerSide.BLUE)
        host.play(PlayerSide.RED, 1)
        other.coordinator.perform(MatchCommandAction.RECORD, MatchEvent(MatchEventType.FOUL, PlayerSide.RED))
        val duplicate = packets.filter { it.message is NearbyMatchMessage.Commands }
        packets.addAll(duplicate)
        drain()
        assertEquals(3, host.match.state.red.points)
        assertEquals(2, host.match.state.blue.points)
        assertEquals(1, host.match.state.red.fouls)
        assertTrue(other.repository.data.nearbySession!!.pendingCommands.isEmpty())
        assertSameGame(other)
    }

    @Test fun guestWatchCommandsAreAcknowledgedOnlyAfterHostAppliesThem() {
        val other = connect()
        val command = WatchMatchCommand(other.match.id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 2))
        other.coordinator.handleWatchCommands(listOf(command, command))
        assertEquals(0, other.match.state.blue.points)
        assertFalse(command.id in other.repository.data.watchUpdate.acknowledgedIds)
        drain()
        assertEquals(2, host.match.state.blue.points)
        assertTrue(command.id in other.repository.data.watchUpdate.acknowledgedIds)
        other.coordinator.handleWatchCommands(listOf(command))
        drain()
        assertEquals(2, host.match.state.blue.points)
        assertSameGame(other)
    }

    @Test fun hostWatchCommandsAlsoReachGuestAndItsWatchSnapshot() {
        val other = connect()
        val command = WatchMatchCommand(host.match.id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.STEAL, PlayerSide.RED))
        host.coordinator.handleWatchCommands(listOf(command))
        drain()
        assertEquals(1, other.repository.data.watchUpdate.match!!.state.red.steals)
        assertEquals(1, other.repository.data.watchUpdate.match!!.state.blue.turnovers)
        assertSameGame(other)
    }

    @Test fun guestQueueSurvivesDisconnectionAndProcessRestart() {
        val other = connect()
        host.coordinator.disconnected(); other.coordinator.disconnected()
        other.play(PlayerSide.BLUE)
        other.coordinator.perform(MatchCommandAction.RECORD, MatchEvent(MatchEventType.TURNOVER, PlayerSide.RED))
        assertTrue(packets.isEmpty())
        assertEquals(0, other.match.state.blue.points)
        val reopened = Node(other.identity, other.storage).also { guest = it }
        assertEquals(NearbyMatchRole.GUEST, reopened.repository.data.nearbySession!!.role)
        assertEquals(2, reopened.repository.data.nearbySession!!.pendingCommands.size)
        connect(start = false)
        assertEquals(2, host.match.state.blue.points)
        assertEquals(1, host.match.state.red.turnovers)
        assertSameGame(reopened)
    }

    @Test fun lostAcknowledgementAndHostRestartCannotDoubleCountGuestPlay() {
        val other = connect()
        other.play(PlayerSide.BLUE)
        val request = packets.removeAt(0)
        host.coordinator.receive(request.from, request.message)
        packets.clear() // Lose all acknowledgements.
        assertEquals(2, host.match.state.blue.points)
        host.coordinator.disconnected(); other.coordinator.disconnected()
        val restarted = Node(host.identity, host.storage)
        restarted.coordinator.connected(other.identity, true)
        other.coordinator.connected(restarted.identity, false)
        drain()
        assertEquals(2, restarted.match.state.blue.points)
        assertEquals(1, restarted.match.events.size)
        assertTrue(other.repository.data.nearbySession!!.pendingCommands.isEmpty())
    }

    @Test fun lateSnapshotsCannotResurrectOldScoreboardOrCancelledGame() {
        val other = connect()
        host.play(PlayerSide.RED)
        val oldSnapshot = packets.first { it.message is NearbyMatchMessage.Snapshot }
        drain()
        host.coordinator.perform(MatchCommandAction.UNDO)
        drain()
        other.coordinator.receive(oldSnapshot.from, oldSnapshot.message)
        assertEquals(0, other.match.state.red.points)
        host.coordinator.perform(MatchCommandAction.CANCEL)
        drain()
        other.coordinator.receive(oldSnapshot.from, oldSnapshot.message)
        assertNull(other.repository.data.activeMatch)
        assertEquals(NearbyMatchStatus.CANCELLED, other.repository.data.nearbySession!!.status)
    }

    @Test fun cancelledGameCannotReappearAfterAnInterveningLocalGameAndRestart() {
        val other = connect()
        host.play(PlayerSide.RED)
        val stale = packets.first { it.message is NearbyMatchMessage.Snapshot }
        drain()
        host.coordinator.perform(MatchCommandAction.CANCEL); drain()
        val friend = other.repository.addFriend("Kellam")!!
        other.repository.startMatch(friend.id, PlayerSide.RED, MatchSettings())
        other.repository.cancelMatch()
        val reopened = Node(other.identity, other.storage).also { guest = it }
        reopened.coordinator.connected(host.identity, false)
        reopened.coordinator.receive(stale.from, stale.message)
        assertNull(reopened.repository.data.activeMatch)
    }

    @Test fun guestFinishSavesOppositeWinLossRecordsExactlyOnceOnBothPhones() {
        val other = connect(settings = MatchSettings(targetScore = 5, winByTwo = false))
        repeat(3) { other.play(PlayerSide.BLUE); drain() }
        assertEquals(true, other.match.userWon)
        other.coordinator.perform(MatchCommandAction.FINISH)
        val duplicate = packets.toList()
        drain(); packets.addAll(duplicate); drain()
        assertNull(host.repository.data.activeMatch)
        assertNull(other.repository.data.activeMatch)
        assertEquals(1, host.repository.data.overallRecord.losses)
        assertEquals(1, other.repository.data.overallRecord.wins)
        assertEquals(1, other.repository.data.matches.size)
        assertEquals(host.repository.data.matches.first().id, other.repository.data.matches.first().id)
    }

    @Test fun offlineGuestReceivesFinishedResultOnReconnect() {
        val other = connect(settings = MatchSettings(targetScore = 5, winByTwo = false))
        host.coordinator.disconnected(); other.coordinator.disconnected()
        repeat(3) { host.play(PlayerSide.RED) }
        host.coordinator.perform(MatchCommandAction.FINISH)
        connect(start = false)
        assertNull(other.repository.data.activeMatch)
        assertEquals(1, other.repository.data.overallRecord.losses)
        other.coordinator.synchronize(); drain()
        assertEquals(1, other.repository.data.matches.size)
    }

    @Test fun guestCancellationClearsBothWithoutAddingARecord() {
        val other = connect()
        other.coordinator.perform(MatchCommandAction.CANCEL)
        drain()
        assertNull(host.repository.data.activeMatch)
        assertNull(other.repository.data.activeMatch)
        assertTrue(other.repository.data.matches.isEmpty())
        assertEquals(NearbyMatchStatus.CANCELLED, host.repository.data.nearbySession!!.status)
    }

    @Test fun prematureSaveAndInvalidScoringCannotDiscardOrInflateGame() {
        val other = connect()
        other.coordinator.perform(MatchCommandAction.FINISH)
        other.play(PlayerSide.BLUE, 3)
        drain()
        assertNotNull(host.repository.data.activeMatch)
        assertEquals(0, host.match.state.blue.points)
        assertTrue(other.repository.data.nearbySession!!.pendingCommands.isEmpty())
    }

    @Test fun unrelatedPeerAndOldMatchCommandsAreRejected() {
        val other = connect()
        val malicious = NearbyMatchMessage.Commands(listOf(WatchMatchCommand(host.match.id, MatchCommandAction.CANCEL)))
        host.coordinator.receive("unknown-peer", malicious)
        val oldMatch = NearbyMatchMessage.Commands(listOf(WatchMatchCommand("old-match", MatchCommandAction.CANCEL)))
        host.coordinator.receive(other.identity.id, oldMatch)
        drain()
        assertNotNull(host.repository.data.activeMatch)
        assertSameGame(other)
    }

    @Test fun incomingGameNeverOverwritesAnUnrelatedLocalMatch() {
        val other = connect(start = false)
        val friend = other.repository.addFriend("Kellam")!!
        other.repository.startMatch(friend.id, PlayerSide.RED, MatchSettings())
        val localId = other.match.id
        host.coordinator.startMatch(PlayerSide.RED, MatchSettings())
        drain()
        assertEquals(localId, other.match.id)
        assertNull(other.repository.data.nearbySession)
    }

    @Test fun aNewGameAfterCancellationKeepsHistoryAndUsesNewSharedId() {
        val other = connect()
        val oldId = host.match.id
        host.coordinator.perform(MatchCommandAction.CANCEL); drain()
        assertTrue(host.coordinator.startMatch(PlayerSide.BLUE, MatchSettings()))
        drain()
        assertNotEquals(oldId, other.match.id)
        assertEquals(PlayerSide.RED, other.match.userSide)
        assertSameGame(other)
    }

    @Test fun guestCanExplicitlyAbandonLocalCopyWithoutCancellingOpponentOrResurrectingIt() {
        val other = connect()
        host.play(PlayerSide.RED)
        val stale = packets.first { it.message is NearbyMatchMessage.Snapshot }
        drain()
        other.coordinator.disconnected()
        other.play(PlayerSide.BLUE)
        other.repository.abandonNearbyMatch()
        assertNull(other.repository.data.activeMatch)
        assertNotNull(host.repository.data.activeMatch)
        assertTrue(other.repository.data.nearbySession!!.pendingCommands.isEmpty())
        val reopened = Node(other.identity, other.storage)
        reopened.coordinator.connected(host.identity, false)
        reopened.coordinator.receive(stale.from, stale.message)
        assertNull(reopened.repository.data.activeMatch)
        assertTrue(reopened.repository.data.matches.isEmpty())
    }

    @Test fun reconnectingDifferentOpponentDoesNotFlushSavedGuestCommands() {
        val other = connect()
        other.coordinator.disconnected()
        other.play(PlayerSide.BLUE)
        other.coordinator.connected(NearbyPlayerIdentity(UUID.randomUUID().toString(), "Other phone"), true)
        assertTrue(packets.isEmpty())
        assertEquals(1, other.repository.data.nearbySession!!.pendingCommands.size)
    }
}
