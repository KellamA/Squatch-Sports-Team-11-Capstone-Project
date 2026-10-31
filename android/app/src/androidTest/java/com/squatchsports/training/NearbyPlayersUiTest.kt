package com.squatchsports.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.squatchsports.training.nearby.NearbyConfirmation
import com.squatchsports.training.nearby.NearbyConnectionState
import com.squatchsports.training.nearby.NearbyPeer
import com.squatchsports.training.shared.MatchSettings
import com.squatchsports.training.shared.NearbyPlayerIdentity
import com.squatchsports.training.shared.PlayerSide
import com.squatchsports.training.shared.ScoringSystem
import com.squatchsports.training.ui.NearbyPlayersContent
import com.squatchsports.training.ui.theme.SquatchSportsTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** No Bluetooth, Wi-Fi discovery, permissions or production match storage are touched. */
class NearbyPlayersUiTest {
    @get:Rule val compose = createComposeRule()
    private val peer = NearbyPeer("endpoint", NearbyPlayerIdentity("peer-id", "Kaleb"))
    private var accepted = 0
    private var rejected = 0
    private var searched = 0
    private var settingsOpened = 0
    private var selected: NearbyPeer? = null
    private var started: Pair<PlayerSide, MatchSettings>? = null

    private fun show(state: NearbyConnectionState, canHost: Boolean = false, permissionError: String? = null) {
        compose.setContent {
            SquatchSportsTheme {
                NearbyPlayersContent(state, canHost, hasActiveMatch = false, PaddingValues(), permissionError,
                    onSearch = { searched++ }, onSettings = { settingsOpened++ }, onConnect = { selected = it },
                    onDisconnect = {}, onAccept = { accepted++ }, onReject = { rejected++ }, onResume = {},
                    onStart = { side, rules -> started = side to rules })
            }
        }
    }

    @Test fun connectionIsNeverAcceptedUntilPlayerConfirmsCode() {
        show(NearbyConnectionState(confirmation = NearbyConfirmation(peer, "4321")))
        compose.onNodeWithText("4321").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, accepted) }
        compose.onNodeWithText("Codes Match — Accept").performClick()
        compose.runOnIdle { assertEquals(1, accepted); assertEquals(0, rejected) }
    }

    @Test fun playerCanRejectAnUnexpectedConnection() {
        show(NearbyConnectionState(confirmation = NearbyConfirmation(peer, "4321")))
        compose.onNodeWithText("Reject").performClick()
        compose.runOnIdle { assertEquals(1, rejected); assertEquals(0, accepted) }
    }

    @Test fun guestWaitsForHostRatherThanStartingCompetingGame() {
        show(NearbyConnectionState(connectedPeer = peer))
        compose.onNodeWithText("Waiting for the host to start the match.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start Nearby 1v1").assertDoesNotExist()
    }

    @Test fun hostConfiguresColorScoringRulesAndFirstBall() {
        show(NearbyConnectionState(connectedPeer = peer), canHost = true)
        compose.onNodeWithText("Blue", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("2s and 3s").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Win by 2").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Make it, take it").performScrollTo().performClick()
        compose.onNodeWithText("Blue starts").performScrollTo().performClick()
        compose.onNodeWithText("Start Nearby 1v1").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(PlayerSide.BLUE, started!!.first)
            assertEquals(MatchSettings(ScoringSystem.TWOS_AND_THREES, 21, winByTwo = false, makeItTakeIt = true,
                firstPossession = PlayerSide.BLUE), started!!.second)
        }
    }

    @Test fun deniedPermissionsExplainRecoveryWithoutBlockingLocalTraining() {
        show(NearbyConnectionState(), permissionError = "Nearby access was denied. Local 1v1 still works.")
        compose.onNodeWithText("Open App Settings").performScrollTo().performClick()
        compose.onNodeWithText("Find Nearby Players").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, settingsOpened); assertEquals(1, searched); assertEquals(0, accepted) }
    }

    @Test fun connectTargetsOnlyTheSelectedPlayer() {
        show(NearbyConnectionState(searching = true, peers = listOf(peer,
            NearbyPeer("another", NearbyPlayerIdentity("other-id", "Kellam")))))
        compose.onNodeWithText("Connect to Kellam").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("another", selected!!.endpointId) }
    }
}
