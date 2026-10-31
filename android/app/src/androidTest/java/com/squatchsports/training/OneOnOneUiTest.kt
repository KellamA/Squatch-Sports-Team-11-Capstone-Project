package com.squatchsports.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.squatchsports.training.data.OneOnOneStore
import com.squatchsports.training.ui.LiveMatchScreen
import com.squatchsports.training.ui.MatchSetupScreen
import com.squatchsports.training.ui.OneOnOneHomeScreen
import com.squatchsports.training.ui.theme.SquatchSportsTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Uses separate storage; never replaces the user's friends or workout history. */
class OneOnOneUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "one_on_one_ui_test_${UUID.randomUUID()}"
    private lateinit var store: OneOnOneStore

    @Before fun setup() {
        store = OneOnOneStore(context, preferencesName)
        compose.setContent {
            SquatchSportsTheme {
                var page by remember { mutableStateOf("home") }
                var opponentId by remember { mutableStateOf<String?>(null) }
                when (page) {
                    "home" -> OneOnOneHomeScreen(store, PaddingValues(),
                        onSetup = { opponentId = it; page = "setup" }, onResume = { page = "match" })
                    "setup" -> MatchSetupScreen(store, opponentId, PaddingValues(), onStart = { page = "match" })
                    "match" -> LiveMatchScreen(store, PaddingValues(), onLeave = { page = "home" }, onDone = { page = "home" })
                }
            }
        }
    }

    @After fun cleanup() { context.deleteSharedPreferences(preferencesName) }

    private fun addFriendAndOpenSetup() {
        compose.onNodeWithText("Add Friend").performScrollTo().performClick()
        compose.onNodeWithText("Name").performTextInput("Kaleb")
        compose.onNodeWithText("Add", substring = false).performClick()
        compose.onNodeWithText("Play Kaleb").performScrollTo().performClick()
    }

    @Test fun playUndoAndSaveFullMatch() {
        addFriendAndOpenSetup()
        compose.onNodeWithText("Start Match").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Red plus 2").performScrollTo().performClick()
        compose.onNodeWithText("Undo last play").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, store.data.activeMatch!!.state.red.points) }
        repeat(6) { compose.onNodeWithContentDescription("Red plus 2").performScrollTo().performClick() }
        compose.onNodeWithText("You win!").assertIsDisplayed()
        compose.onNodeWithText("Save Match").performClick()
        compose.onNodeWithText("Recent Games").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertNull(store.data.activeMatch)
            assertEquals(1, store.data.overallRecord.wins)
            assertEquals(1, store.data.matches.size)
        }
    }

    @Test fun bluePlayerCanUseTwosAndThreesAndResumePausedMatch() {
        addFriendAndOpenSetup()
        compose.onNodeWithText("Blue", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("2s and 3s").performScrollTo().performClick()
        compose.onNodeWithText("Start Match").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Blue plus 3").performScrollTo().performClick()
        compose.onNodeWithText("Leave Match").performScrollTo().performClick()
        compose.onNodeWithText("Resume Match").performScrollTo().performClick()
        compose.runOnIdle {
            val reopened = OneOnOneStore(context, preferencesName)
            assertEquals(3, reopened.data.activeMatch!!.state.blue.points)
        }
        repeat(6) { compose.onNodeWithContentDescription("Blue plus 3").performScrollTo().performClick() }
        compose.onNodeWithText("You win!").assertIsDisplayed()
        compose.onNodeWithText("Save Match").performClick()
        compose.runOnIdle {
            assertEquals(1, store.data.overallRecord.wins)
            assertEquals(21, store.data.matches.first().state.blue.points)
        }
    }
}
