package com.squatchsports.training.presentation

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.squatchsports.training.presentation.theme.SquatchSportsTheme
import com.squatchsports.training.shared.MatchCommandAction
import com.squatchsports.training.shared.MatchEvent
import com.squatchsports.training.shared.MatchEventType
import com.squatchsports.training.shared.MatchSettings
import com.squatchsports.training.shared.MatchState
import com.squatchsports.training.shared.PlayerSide
import com.squatchsports.training.shared.PlayerTally
import com.squatchsports.training.shared.ScoringSystem
import com.squatchsports.training.shared.WatchMatchSnapshot
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Stateless UI: no real match storage or Data Layer traffic is touched. */
class WatchMatchUiTest {
    @get:Rule val compose = createComposeRule()
    private val commands = mutableListOf<Pair<MatchCommandAction, MatchEvent?>>()
    private fun snapshot(scoring: ScoringSystem = ScoringSystem.ONES_AND_TWOS, winner: PlayerSide? = null) =
        WatchMatchSnapshot("test", PlayerSide.BLUE, "Melvin", "Kaleb", MatchSettings(scoring = scoring),
            MatchState(PlayerTally(points = if (winner == PlayerSide.RED) 11 else 0), PlayerTally(), PlayerSide.BLUE, winner), null, true)

    private fun show(match: WatchMatchSnapshot, pending: Int = 0, finishing: Boolean = false) {
        compose.setContent {
            SquatchSportsTheme {
                WatchMatchControls(match, pending, phoneConnected = false, hasPendingFinish = finishing) { action, event ->
                    commands.add(action to event)
                }
            }
        }
    }

    @Test fun logsBothPlayersAndSwitchesPages() {
        show(snapshot())
        compose.onNodeWithContentDescription("Melvin plus 2").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Melvin Miss").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Opponent's plays →").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Kaleb plus 1").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf(
                MatchCommandAction.RECORD to MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 2),
                MatchCommandAction.RECORD to MatchEvent(MatchEventType.POSSESSION, PlayerSide.RED),
                MatchCommandAction.RECORD to MatchEvent(MatchEventType.SCORE, PlayerSide.RED, 1),
            ), commands)
        }
    }

    @Test fun twosAndThreesFoulTurnoverUndoAndOfflineIndicator() {
        show(snapshot(ScoringSystem.TWOS_AND_THREES), pending = 2)
        compose.onNodeWithContentDescription("Melvin plus 3").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Melvin Foul").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Melvin TO").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Undo last play").performScrollTo().performClick()
        compose.onNodeWithText("2 plays pending sync").performScrollTo()
        compose.runOnIdle {
            assertEquals(MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 3), commands[0].second)
            assertEquals(MatchEventType.FOUL, commands[1].second!!.type)
            assertEquals(MatchEventType.TURNOVER, commands[2].second!!.type)
            assertEquals(MatchCommandAction.UNDO, commands[3].first)
        }
    }

    @Test fun finishedMatchCanBeSavedFromWatch() {
        show(snapshot(winner = PlayerSide.RED))
        compose.onNodeWithContentDescription("Save Match").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(MatchCommandAction.FINISH to null), commands) }
    }

    @Test fun pendingSaveDisablesSaveAndUndoUntilPhoneAcknowledges() {
        show(snapshot(winner = PlayerSide.RED), pending = 1, finishing = true)
        compose.onNodeWithContentDescription("Saving…").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Undo last play").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
    }
}
