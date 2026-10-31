package com.squatchsports.training.shared

import org.junit.Assert.*
import org.junit.Test

class MatchEngineTest {
    private fun score(side: PlayerSide, points: Int = 1) = MatchEvent(MatchEventType.SCORE, side, points)
    private fun state(vararg events: MatchEvent, settings: MatchSettings = MatchSettings()) = MatchEngine.state(events.toList(), settings)
    private fun match(settings: MatchSettings = MatchSettings(), userSide: PlayerSide = PlayerSide.RED) =
        OneOnOneMatch("friend", "Kaleb", userSide, settings)

    @Test fun newGameIsEmptyAndUsesFirstPossession() {
        val state = state(settings = MatchSettings(firstPossession = PlayerSide.BLUE))
        assertEquals(PlayerTally(), state.red)
        assertEquals(PlayerTally(), state.blue)
        assertEquals(PlayerSide.BLUE, state.possession)
        assertNull(state.winner)
        assertNull(state.leader)
    }

    @Test fun basketsCountAndNormallyGiveBallAway() {
        val state = state(score(PlayerSide.RED, 2), score(PlayerSide.BLUE))
        assertEquals(2, state.red.points)
        assertEquals(1, state.red.baskets)
        assertEquals(1, state.blue.points)
        assertEquals(PlayerSide.RED, state.possession)
        assertEquals(PlayerSide.RED, state.leader)
    }

    @Test fun makeItTakeItKeepsBall() {
        assertEquals(PlayerSide.RED, state(score(PlayerSide.RED), settings = MatchSettings(makeItTakeIt = true)).possession)
    }

    @Test fun foulChangesPossessionWithoutAddingPoints() {
        val state = state(MatchEvent(MatchEventType.FOUL, PlayerSide.RED))
        assertEquals(1, state.red.fouls)
        assertEquals(0, state.red.points)
        assertEquals(PlayerSide.BLUE, state.possession)
    }

    @Test fun turnoverChangesPossession() {
        val state = state(MatchEvent(MatchEventType.TURNOVER, PlayerSide.RED))
        assertEquals(1, state.red.turnovers)
        assertEquals(PlayerSide.BLUE, state.possession)
    }

    @Test fun stealAlsoCountsOpponentTurnover() {
        val state = state(MatchEvent(MatchEventType.STEAL, PlayerSide.BLUE))
        assertEquals(1, state.blue.steals)
        assertEquals(1, state.red.turnovers)
        assertEquals(PlayerSide.BLUE, state.possession)
    }

    @Test fun missedShotChangesBallWithoutStats() {
        val state = state(MatchEvent(MatchEventType.POSSESSION, PlayerSide.BLUE))
        assertEquals(PlayerTally(), state.red)
        assertEquals(PlayerTally(), state.blue)
        assertEquals(PlayerSide.BLUE, state.possession)
    }

    @Test fun reachingTargetWinsWithoutWinByTwo() {
        val settings = MatchSettings(targetScore = 5, winByTwo = false)
        val events = List(5) { score(PlayerSide.BLUE) }
        assertEquals(PlayerSide.BLUE, MatchEngine.state(events, settings).winner)
    }

    @Test fun winByTwoExtendsGamePastTarget() {
        val settings = MatchSettings(targetScore = 5)
        val events = listOf(score(PlayerSide.RED, 2), score(PlayerSide.BLUE, 2), score(PlayerSide.RED, 2), score(PlayerSide.BLUE, 2), score(PlayerSide.RED))
        assertNull(MatchEngine.state(events, settings).winner) // 5–4
        assertEquals(PlayerSide.RED, MatchEngine.state(events + score(PlayerSide.RED), settings).winner) // 6–4
    }

    @Test fun eventsAfterWinnerAreIgnored() {
        val events = List(3) { score(PlayerSide.RED, 2) } + score(PlayerSide.BLUE, 2)
        val state = MatchEngine.state(events, MatchSettings(targetScore = 5, winByTwo = false))
        assertEquals(PlayerSide.RED, state.winner)
        assertEquals(0, state.blue.points)
    }

    @Test fun bothScoringSystemsHaveCorrectDefaultsAndValues() {
        for (system in ScoringSystem.entries) {
            assertEquals(system.insidePoints + 1, system.outsidePoints)
            assertEquals(if (system == ScoringSystem.ONES_AND_TWOS) 11 else 21, system.defaultTarget)
            val state = state(score(PlayerSide.RED, system.outsidePoints), settings = MatchSettings(scoring = system, targetScore = system.defaultTarget))
            assertEquals(system.outsidePoints, state.red.points)
        }
    }

    @Test fun pointsOutsideChosenScoringSystemAreRejected() {
        val match = match().record(score(PlayerSide.RED, 3))
        assertTrue(match.events.isEmpty())
        assertEquals(0, state(score(PlayerSide.RED), settings = MatchSettings(scoring = ScoringSystem.TWOS_AND_THREES)).red.points)
    }

    @Test fun gamePointRespectsLeadAndWinner() {
        val settings = MatchSettings()
        val state = MatchEngine.state(List(9) { score(PlayerSide.RED) }, settings)
        assertTrue(state.isGamePoint(PlayerSide.RED, settings))
        assertFalse(state.isGamePoint(PlayerSide.BLUE, settings))
        assertFalse(MatchState(PlayerTally(points = 10), PlayerTally(points = 11), PlayerSide.RED).isGamePoint(PlayerSide.RED, settings))
        assertFalse(MatchState(possession = PlayerSide.RED, winner = PlayerSide.RED).isGamePoint(PlayerSide.RED, settings))
    }

    @Test fun undoRemovesOnlyLastPlay() {
        val match = match().record(score(PlayerSide.RED, 2)).record(MatchEvent(MatchEventType.FOUL, PlayerSide.BLUE)).undo()
        assertEquals(2, match.state.red.points)
        assertEquals(0, match.state.blue.fouls)
        assertEquals(1, match.events.size)
    }

    @Test fun winningSetsEndDateAndUndoReopensMatch() {
        var match = match(MatchSettings(targetScore = 5, winByTwo = false), PlayerSide.BLUE)
        repeat(3) { match = match.record(score(PlayerSide.BLUE, 2), now = 100L) }
        assertEquals(true, match.userWon)
        assertEquals(100L, match.endDateMillis)
        assertEquals(match, match.record(score(PlayerSide.RED)))
        val reopened = match.undo()
        assertNull(reopened.state.winner)
        assertNull(reopened.endDateMillis)
        assertEquals(4, reopened.state.blue.points)
    }

    @Test fun emptyUndoIsNoOpAndNamesFollowChosenSide() {
        val match = match(userSide = PlayerSide.BLUE)
        assertEquals(match, match.undo())
        assertEquals("Melvin", match.name(PlayerSide.BLUE, "Melvin"))
        assertEquals("Kaleb", match.name(PlayerSide.RED, "Melvin"))
    }

    @Test fun eventSummariesUsePlayerNames() {
        assertEquals("Kaleb +2", score(PlayerSide.BLUE, 2).summary { "Kaleb" })
        assertEquals("Red foul", MatchEvent(MatchEventType.FOUL, PlayerSide.RED).summary())
        assertEquals("Ball → Blue", MatchEvent(MatchEventType.POSSESSION, PlayerSide.BLUE).summary())
    }

    @Test(expected = IllegalArgumentException::class) fun targetCannotBeBelowAllowedRange() { MatchSettings(targetScore = 4) }
    @Test(expected = IllegalArgumentException::class) fun targetCannotBeAboveAllowedRange() { MatchSettings(targetScore = 51) }
    @Test(expected = IllegalArgumentException::class) fun scoreCannotBeNegative() { score(PlayerSide.RED, -1) }
    @Test(expected = IllegalArgumentException::class) fun nonScoreCannotCarryPoints() { MatchEvent(MatchEventType.FOUL, PlayerSide.RED, 1) }
}
