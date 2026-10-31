package com.squatchsports.training.shared

import org.junit.Assert.*
import org.junit.Test

internal class MemoryMatchStorage : MatchStorage {
    var value: String? = null
    override fun read() = value
    override fun write(value: String) { this.value = value }
}

class OneOnOneRepositoryTest {
    private val storage = MemoryMatchStorage()
    private val store = OneOnOneRepository(storage)
    private fun start(side: PlayerSide = PlayerSide.RED, settings: MatchSettings = MatchSettings()): String {
        val friend = store.data.friends.firstOrNull() ?: store.addFriend("Kaleb")!!
        assertTrue(store.startMatch(friend.id, side, settings))
        return store.data.activeMatch!!.id
    }
    private fun play(friend: MatchFriend, userWins: Boolean) {
        store.startMatch(friend.id, PlayerSide.RED, MatchSettings(targetScore = 5, winByTwo = false))
        repeat(3) { store.record(MatchEvent(MatchEventType.SCORE, if (userWins) PlayerSide.RED else PlayerSide.BLUE, 2)) }
        store.finishMatch()
    }

    @Test fun trimsNamesAndRejectsEmptyOrCaseInsensitiveDuplicates() {
        assertEquals("Kaleb", store.addFriend("  Kaleb ")!!.name)
        assertNull(store.addFriend("kaleb"))
        assertNull(store.addFriend("   "))
        store.setPlayerName(" Melvin ")
        store.setPlayerName(" ")
        assertEquals("Melvin", store.data.playerName)
    }

    @Test fun recordsAreOverallAndPerOpponent() {
        val kaleb = store.addFriend("Kaleb")!!
        val kellam = store.addFriend("Kellam")!!
        play(kaleb, true); play(kaleb, true); play(kaleb, false); play(kellam, false)
        assertEquals(HeadToHeadRecord(2, 1), store.data.recordAgainst(kaleb.id))
        assertEquals(HeadToHeadRecord(0, 1), store.data.recordAgainst(kellam.id))
        assertEquals("2-2", store.data.overallRecord.text)
        assertEquals(50, store.data.overallRecord.winPercentage)
        assertEquals(4, OneOnOneRepository(storage).data.matches.size)
    }

    @Test fun cannotReplaceAnActiveGameOrStartAgainstUnknownFriend() {
        assertFalse(store.startMatch("missing", PlayerSide.RED, MatchSettings()))
        val id = start()
        assertFalse(store.startMatch(store.data.friends.first().id, PlayerSide.BLUE, MatchSettings()))
        assertEquals(id, store.data.activeMatch!!.id)
    }

    @Test fun prematureSaveDoesNotDiscardOrCountUnfinishedGame() {
        start()
        store.record(MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 1))
        store.finishMatch()
        assertTrue(store.data.matches.isEmpty())
        assertNotNull(store.data.activeMatch)
        store.cancelMatch()
        assertNull(store.data.activeMatch)
        assertEquals(0, store.data.overallRecord.games)
    }

    @Test fun matchAndEventsSurviveProcessRelaunch() {
        start(PlayerSide.BLUE, MatchSettings(scoring = ScoringSystem.TWOS_AND_THREES, targetScore = 21))
        store.setPlayerName("Melvin")
        store.record(MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 3))
        store.record(MatchEvent(MatchEventType.FOUL, PlayerSide.RED))
        val reopened = OneOnOneRepository(storage)
        assertEquals(store.data, reopened.data)
        assertEquals(3, reopened.data.activeMatch!!.state.blue.points)
        assertEquals(1, reopened.data.activeMatch!!.state.red.fouls)
    }

    @Test fun removingFriendKeepsActiveAndPastMatchNames() {
        val friend = store.addFriend("Kaleb")!!
        play(friend, true)
        start()
        store.removeFriend(friend.id)
        assertTrue(store.data.friends.isEmpty())
        assertEquals("Kaleb", store.data.matches.first().opponentName)
        assertEquals("Kaleb", store.data.activeMatch!!.opponentName)
    }

    @Test fun watchSnapshotShowsNamesRulesPossessionStatsAndLastPlay() {
        start(PlayerSide.BLUE, MatchSettings(scoring = ScoringSystem.TWOS_AND_THREES, targetScore = 21))
        store.setPlayerName("Melvin")
        store.record(MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 3))
        store.record(MatchEvent(MatchEventType.FOUL, PlayerSide.RED))
        val snapshot = store.data.watchUpdate.match!!
        assertEquals("Melvin", snapshot.name(PlayerSide.BLUE))
        assertEquals("Kaleb", snapshot.name(PlayerSide.RED))
        assertEquals(3, snapshot.state.blue.points)
        assertEquals(1, snapshot.state.red.fouls)
        assertEquals(PlayerSide.BLUE, snapshot.state.possession)
        assertEquals("Kaleb foul", snapshot.lastPlay)
        assertTrue(snapshot.canUndo)
    }

    @Test fun watchBatchReplaysInOrderAndUndoWorks() {
        val id = start()
        store.handleCommands(listOf(
            WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2)),
            WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.TURNOVER, PlayerSide.RED)),
            WatchMatchCommand(id, MatchCommandAction.UNDO),
        ))
        assertEquals(2, store.data.activeMatch!!.state.red.points)
        assertEquals(0, store.data.activeMatch!!.state.red.turnovers)
        assertEquals(1, store.data.activeMatch!!.events.size)
    }

    @Test fun duplicateWatchCommandsAreNotReplayedEvenAfterRelaunch() {
        val id = start()
        val score = WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2))
        val undo = WatchMatchCommand(id, MatchCommandAction.UNDO)
        store.handleCommands(listOf(score, score))
        assertEquals(2, store.data.activeMatch!!.state.red.points)
        val reopened = OneOnOneRepository(storage)
        reopened.handleCommands(listOf(score, undo, undo))
        assertEquals(0, reopened.data.activeMatch!!.state.red.points)
        reopened.handleCommands(listOf(score))
        assertEquals(0, reopened.data.activeMatch!!.state.red.points)
        assertEquals(setOf(score.id, undo.id), reopened.data.watchUpdate.acknowledgedIds)
    }

    @Test fun staleCommandsNeverAffectNewMatch() {
        val oldId = start()
        store.cancelMatch()
        start()
        store.handleCommands(listOf(WatchMatchCommand(oldId, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2)),
            WatchMatchCommand(oldId, MatchCommandAction.FINISH), WatchMatchCommand(oldId, MatchCommandAction.UNDO)))
        assertEquals(0, store.data.activeMatch!!.events.size)
        assertEquals(0, store.data.matches.size)
    }

    @Test fun watchFinishIsAcknowledgedAndSavedExactlyOnce() {
        val id = start(settings = MatchSettings(targetScore = 5, winByTwo = false))
        val batch = List(3) { WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2)) } +
            WatchMatchCommand(id, MatchCommandAction.FINISH)
        store.handleCommands(batch)
        store.handleCommands(batch)
        val reopened = OneOnOneRepository(storage)
        reopened.handleCommands(batch)
        assertNull(reopened.data.activeMatch)
        assertNull(reopened.data.watchUpdate.match)
        assertEquals(1, reopened.data.matches.size)
        assertEquals(1, reopened.data.overallRecord.wins)
        assertTrue(batch.all { it.id in reopened.data.watchUpdate.acknowledgedIds })
    }

    @Test fun invalidPointsAreAcknowledgedButDoNotChangeScore() {
        val id = start()
        val command = WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 3))
        store.handleCommands(listOf(command))
        assertEquals(0, store.data.activeMatch!!.state.red.points)
        assertTrue(command.id in store.data.watchUpdate.acknowledgedIds)
    }

    @Test fun failedPersistenceNeverPublishesOrAcknowledgesAPlay() {
        val id = start()
        val saved = storage.value
        val failing = OneOnOneRepository(object : MatchStorage {
            override fun read() = saved
            override fun write(value: String) { error("disk full") }
        })
        var notifications = 0
        failing.onChange = { notifications++ }
        val command = WatchMatchCommand(id, MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 2))
        assertTrue(runCatching { failing.handleCommands(listOf(command)) }.isFailure)
        assertEquals(0, failing.data.activeMatch!!.state.red.points)
        assertTrue(failing.data.processedCommandIds.isEmpty())
        assertEquals(0, notifications)
    }

    @Test fun revisionsIncreaseAndNoOpsDoNotNotify() {
        start()
        val revision = store.data.revision
        var notifications = 0
        store.onChange = { notifications++ }
        store.undo()
        assertEquals(0, notifications)
        store.record(MatchEvent(MatchEventType.FOUL, PlayerSide.RED))
        assertTrue(store.data.revision > revision)
        assertEquals(1, notifications)
    }

    @Test fun malformedStorageFallsBackToEmptyData() {
        storage.value = "not json"
        val recovered = OneOnOneRepository(storage).data
        assertEquals(OneOnOneData(), recovered.copy(revision = 0))
        assertTrue(recovered.revision > 0)
    }
}
