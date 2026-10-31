package com.squatchsports.training.shared

import org.junit.Assert.*
import org.junit.Test

class MatchProtocolTest {
    private fun command(id: String = "game", action: MatchCommandAction = MatchCommandAction.RECORD) =
        WatchMatchCommand(id, action, if (action == MatchCommandAction.RECORD) MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2) else null)
    private fun update(revision: Long = 1, matchId: String? = "game", ack: Set<String> = emptySet()) = WatchMatchUpdate(
        revision,
        matchId?.let { WatchMatchSnapshot(it, PlayerSide.BLUE, "Melvin", "Kaleb", MatchSettings(),
            MatchState(PlayerTally(points = 2, fouls = 1), PlayerTally(steals = 1), PlayerSide.BLUE), "Kaleb +2", true) },
        ack,
    )

    @Test fun allCommandActionsAndEventTypesRoundTrip() {
        val commands = MatchEventType.entries.map { type ->
            WatchMatchCommand("game", MatchCommandAction.RECORD, MatchEvent(type, PlayerSide.BLUE, if (type == MatchEventType.SCORE) 3 else 0))
        } + command(action = MatchCommandAction.UNDO) + command(action = MatchCommandAction.FINISH)
        assertEquals(commands, MatchPayloads.decodeCommands(MatchPayloads.commands(commands)))
    }

    @Test fun snapshotsAndAcknowledgementsRoundTrip() {
        val update = update(ack = setOf("a", "b"))
        assertEquals(update, MatchPayloads.decodeUpdate(MatchPayloads.update(update)))
        val cleared = update(2, matchId = null, ack = setOf("finish"))
        assertEquals(cleared, MatchPayloads.decodeUpdate(MatchPayloads.update(cleared)))
    }

    @Test fun winnerAndEmptyLastPlayRoundTrip() {
        val original = update()
        val won = original.copy(match = original.match!!.copy(state = original.match.state.copy(winner = PlayerSide.RED), lastPlay = null))
        assertEquals(won, MatchPayloads.decodeUpdate(MatchPayloads.update(won)))
    }

    @Test fun rejectsMalformedAndUnknownPayloads() {
        for (payload in listOf("", "broken", "{}", "[{\"action\":\"DELETE\"}]")) {
            assertNull(MatchPayloads.decodeCommands(payload.encodeToByteArray()))
        }
        assertNull(MatchPayloads.decodeUpdate("{}".encodeToByteArray()))
        val negative = MatchPayloads.update(update()).decodeToString().replace("\"revision\":1", "\"revision\":-1")
        assertNull(MatchPayloads.decodeUpdate(negative.encodeToByteArray()))
    }

    @Test fun outboxPersistsFifoAndSurvivesRestart() {
        var saved = emptyList<WatchMatchCommand>()
        val outbox = MatchCommandOutbox { saved = it }
        val score = command()
        val undo = command(action = MatchCommandAction.UNDO)
        outbox.enqueue(score); outbox.enqueue(undo); outbox.enqueue(score)
        assertEquals(listOf(score, undo), saved)
        val restarted = MatchCommandOutbox(MatchPayloads.decodeCommands(MatchPayloads.commands(saved))!!) { saved = it }
        assertEquals(listOf(score, undo), restarted.pending)
        restarted.acknowledge(update(ack = setOf(score.id)))
        assertEquals(listOf(undo), restarted.pending)
        assertEquals(listOf(undo), saved)
    }

    @Test fun acceptedSnapshotWithoutAcknowledgementDoesNotDropPendingPlays() {
        val outbox = MatchCommandOutbox {}
        val command = command()
        outbox.enqueue(command)
        outbox.acknowledge(update())
        assertEquals(listOf(command), outbox.pending)
    }

    @Test fun cancelledOrReplacedMatchDropsOnlyObsoleteCommands() {
        val outbox = MatchCommandOutbox {}
        outbox.enqueue(command("old")); val current = command(); outbox.enqueue(current)
        outbox.acknowledge(update())
        assertEquals(listOf(current), outbox.pending)
        outbox.acknowledge(update(2, matchId = null))
        assertTrue(outbox.pending.isEmpty())
    }

    @Test fun offlineBatchRetransmitsSafelyUntilPhoneAcknowledgesIt() {
        var saved = emptyList<WatchMatchCommand>()
        val outbox = MatchCommandOutbox { saved = it }
        val storage = MemoryMatchStorage()
        var phone = OneOnOneRepository(storage)
        val friend = phone.addFriend("Kaleb")!!
        phone.startMatch(friend.id, PlayerSide.RED, MatchSettings())
        val id = phone.data.activeMatch!!.id
        repeat(3) { outbox.enqueue(command(id)) }
        phone.handleCommands(MatchPayloads.decodeCommands(MatchPayloads.commands(outbox.pending))!!)
        // Ack lost, phone and watch restart; sending again must not add 6 more points.
        phone = OneOnOneRepository(storage)
        val watch = MatchCommandOutbox(saved) { saved = it }
        phone.handleCommands(watch.pending)
        assertEquals(6, phone.data.activeMatch!!.state.red.points)
        watch.acknowledge(MatchPayloads.decodeUpdate(MatchPayloads.update(phone.data.watchUpdate))!!)
        assertTrue(watch.pending.isEmpty())
    }

    @Test fun largeOfflineQueueDrainsInOrderedAcknowledgedChunks() {
        val phone = OneOnOneRepository(MemoryMatchStorage())
        val friend = phone.addFriend("Kaleb")!!
        phone.startMatch(friend.id, PlayerSide.RED, MatchSettings())
        val id = phone.data.activeMatch!!.id
        val outbox = MatchCommandOutbox {}
        repeat(250) { outbox.enqueue(WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.FOUL, PlayerSide.BLUE))) }
        var chunks = 0
        while (outbox.pending.isNotEmpty()) {
            val chunk = outbox.pending.take(100)
            phone.handleCommands(chunk)
            phone.handleCommands(chunk) // Duplicate Data Layer delivery is harmless.
            outbox.acknowledge(phone.data.watchUpdate)
            chunks++
        }
        assertEquals(3, chunks)
        assertEquals(250, phone.data.activeMatch!!.state.blue.fouls)
        assertEquals(250, phone.data.activeMatch!!.events.size)
    }

    @Test fun failedOutboxPersistenceDoesNotPretendCommandWasQueued() {
        val outbox = MatchCommandOutbox { error("disk full") }
        assertTrue(runCatching { outbox.enqueue(command()) }.isFailure)
        assertTrue(outbox.pending.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class) fun recordCommandRequiresEvent() { WatchMatchCommand("game", MatchCommandAction.RECORD) }
    @Test(expected = IllegalArgumentException::class) fun undoCannotContainEvent() { WatchMatchCommand("game", MatchCommandAction.UNDO, MatchEvent(MatchEventType.FOUL, PlayerSide.RED)) }
}
