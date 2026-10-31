package com.squatchsports.training.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

    private var finished = false
    private fun show(
        match: WatchMatchSnapshot,
        pending: Int = 0,
        finishing: Boolean = false,
        viewportDp: Int? = null,
        round: Boolean = true,
        fontScale: Float = 1f,
    ) {
        finished = match.state.isOver
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                screenLayout = (screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK.inv()) or
                    if (round) Configuration.SCREENLAYOUT_ROUND_YES else Configuration.SCREENLAYOUT_ROUND_NO
                if (viewportDp != null) {
                    screenWidthDp = viewportDp
                    screenHeightDp = viewportDp
                }
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) {
                SquatchSportsTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.then(if (viewportDp == null) Modifier.fillMaxSize() else Modifier.requiredSize(viewportDp.dp))
                            .testTag("watch-viewport")) {
                            WatchMatchControls(match, pending, phoneConnected = false, hasPendingFinish = finishing) { action, event ->
                                commands.add(action to event)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun list(page: Int = 0): SemanticsNodeInteraction {
        val tag = if (finished) "match-finished-controls" else "match-controls-$page"
        return compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag(tag)))
    }

    private fun control(description: String, page: Int = 0): SemanticsNodeInteraction {
        list(page).performScrollToNode(hasContentDescription(description))
        return compose.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    /** Check the full button rectangle, not just whether Compose calls it 'displayed'. */
    private fun assertInsideRoundScreen(node: SemanticsNodeInteraction) {
        val viewport = compose.onNodeWithTag("watch-viewport").fetchSemanticsNode().boundsInRoot
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue("Empty bounds: $bounds", bounds.width > 0 && bounds.height > 0)
        for (x in listOf(bounds.left, bounds.right)) {
            for (y in listOf(bounds.top, bounds.bottom)) {
                val dx = (x - viewport.center.x) / (viewport.width / 2)
                val dy = (y - viewport.center.y) / (viewport.height / 2)
                assertTrue("Button $bounds crosses circular viewport $viewport", dx * dx + dy * dy <= 1.01f)
            }
        }
    }

    @Test fun logsBothPlayersAndSwitchesPages() {
        show(snapshot())
        control("Melvin plus 2").performClick()
        control("Melvin Miss").performClick()
        control("Opponent's plays →").performClick()
        control("Kaleb plus 1", page = 1).performClick()
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
        control("Melvin plus 3").performClick()
        control("Melvin Foul").performClick()
        control("Melvin TO").performClick()
        control("Undo last play").performClick()
        list().performScrollToNode(hasText("2 plays pending sync"))
        compose.onNodeWithText("2 plays pending sync").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(MatchEvent(MatchEventType.SCORE, PlayerSide.BLUE, 3), commands[0].second)
            assertEquals(MatchEventType.FOUL, commands[1].second!!.type)
            assertEquals(MatchEventType.TURNOVER, commands[2].second!!.type)
            assertEquals(MatchCommandAction.UNDO, commands[3].first)
        }
    }

    @Test fun finishedMatchCanBeSavedFromWatch() {
        show(snapshot(winner = PlayerSide.RED))
        control("Save Match").performClick()
        compose.runOnIdle { assertEquals(listOf(MatchCommandAction.FINISH to null), commands) }
    }

    @Test fun pendingSaveDisablesSaveAndUndoUntilPhoneAcknowledges() {
        show(snapshot(winner = PlayerSide.RED), pending = 1, finishing = true)
        control("Saving…").assertIsNotEnabled()
        control("Undo last play").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
    }

    @Test fun roundWatchControlsStayInsideCircularBezel() {
        show(snapshot())
        for (description in listOf("Melvin plus 1", "Melvin plus 2", "Melvin Miss", "Melvin Foul", "Melvin TO",
            "Undo last play", "Opponent's plays →")) {
            assertInsideRoundScreen(control(description))
        }
        control("Opponent's plays →").performClick()
        assertInsideRoundScreen(control("Kaleb plus 1", page = 1))
        assertInsideRoundScreen(control("← Your plays", page = 1))
    }

    @Test fun smallRoundWatchWithLargerTextKeepsAllButtonsReachable() {
        show(snapshot(), viewportDp = 192, fontScale = 1.2f)
        for (description in listOf("Melvin plus 1", "Melvin plus 2", "Melvin Miss", "Melvin Foul", "Melvin TO",
            "Undo last play", "Opponent's plays →")) {
            val button = control(description)
            assertInsideRoundScreen(button)
            button.performClick()
        }
        assertInsideRoundScreen(control("Kaleb plus 1", page = 1))
    }

    @Test fun smallRoundWatchFinishButtonsStayInsideCircularBezel() {
        show(snapshot(winner = PlayerSide.RED), viewportDp = 192, fontScale = 1.2f)
        assertInsideRoundScreen(control("Save Match"))
        assertInsideRoundScreen(control("Undo last play"))
    }

    @Test fun squareWatchKeepsScoringUndoAndPagingReachable() {
        show(snapshot(), viewportDp = 192, round = false)
        control("Melvin plus 2").performClick()
        control("Undo last play").performClick()
        control("Opponent's plays →").performClick()
        control("Kaleb plus 1", page = 1).performClick()
        compose.runOnIdle { assertEquals(3, commands.size) }
    }
}
