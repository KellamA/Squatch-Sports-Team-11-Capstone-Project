package com.squatchsports.training.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.shared.MatchCommandAction
import com.squatchsports.training.shared.MatchEvent
import com.squatchsports.training.shared.MatchEventType
import com.squatchsports.training.shared.PlayerSide
import com.squatchsports.training.shared.WatchMatchSnapshot
import kotlinx.coroutines.launch

private fun PlayerSide.matchColor() = if (this == PlayerSide.RED) Color(0xFFE83845) else Color(0xFF2978F5)

/** Phone-authoritative scoreboard. Offline taps remain visible as pending until acknowledged. */
@Composable
fun WatchMatchScreen(match: WatchMatchSnapshot, connectivity: WorkoutConnectivity) {
    WatchMatchControls(match, connectivity.pendingMatchCommands, connectivity.phoneConnected,
        connectivity.hasPendingFinish, connectivity::sendMatchCommand)
}

/** Stateless controls, so scorekeeping can be tested without sending plays to a real phone. */
@Composable
fun WatchMatchControls(
    match: WatchMatchSnapshot,
    pendingCommands: Int,
    phoneConnected: Boolean,
    hasPendingFinish: Boolean,
    onCommand: (MatchCommandAction, MatchEvent?) -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(match.matchId, match.state.isOver) {
        val previous = view.keepScreenOn
        view.keepScreenOn = !match.state.isOver
        onDispose { view.keepScreenOn = previous }
    }
    val haptics = LocalHapticFeedback.current
    val send: (MatchCommandAction, MatchEvent?) -> Unit = { action, event ->
        onCommand(action, event)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    if (match.state.isOver) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val winner = match.state.winner!!
            Text(if (winner == match.userSide) "🏆 You win!" else "${match.name(winner)} wins!", color = winner.matchColor(), textAlign = TextAlign.Center)
            Text("${match.state[winner].points} – ${match.state[winner.opponent].points}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            MatchWatchButton(if (hasPendingFinish) "Saving…" else "Save Match", winner.matchColor(),
                enabled = !hasPendingFinish) { send(MatchCommandAction.FINISH, null) }
            MatchWatchButton("Undo last play", Color.DarkGray, enabled = !hasPendingFinish) {
                send(MatchCommandAction.UNDO, null)
            }
            MatchSyncStatus(pendingCommands, phoneConnected, match)
        }
    } else {
        HorizontalPager(state = pager, key = { "${match.matchId}:$it" }) { page ->
            val side = if (page == 0) match.userSide else match.userSide.opponent
            Column(
                Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlayerSide.entries.forEach { player ->
                        Column(Modifier.weight(1f).background(player.matchColor().copy(alpha = if (match.state.possession == player) 1f else 0.45f), RoundedCornerShape(10.dp)).padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${match.name(player)}${if (match.state.possession == player) " 🏀" else ""}", fontSize = 10.sp, maxLines = 1)
                            Text("${match.state[player].points}", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { contentDescription = "${player.title} score ${match.state[player].points}" })
                            Text("Fouls ${match.state[player].fouls}", fontSize = 9.sp)
                        }
                    }
                }
                Text(if (side == match.userSide) "Your plays" else "${match.name(side)}'s plays", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(match.settings.scoring.insidePoints, match.settings.scoring.outsidePoints).forEach { points ->
                        MatchWatchButton("+$points", side.matchColor(), Modifier.weight(1f), description = "${match.name(side)} plus $points") {
                            send(MatchCommandAction.RECORD, MatchEvent(MatchEventType.SCORE, side, points))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Miss" to MatchEvent(MatchEventType.POSSESSION, side.opponent),
                        "Foul" to MatchEvent(MatchEventType.FOUL, side),
                        "TO" to MatchEvent(MatchEventType.TURNOVER, side)).forEach { (title, event) ->
                        MatchWatchButton(title, side.matchColor().copy(alpha = 0.4f), Modifier.weight(1f),
                            description = "${match.name(side)} $title") { send(MatchCommandAction.RECORD, event) }
                    }
                }
                MatchWatchButton("Undo last play", Color.DarkGray, enabled = match.canUndo || pendingCommands > 0) {
                    send(MatchCommandAction.UNDO, null)
                }
                MatchSyncStatus(pendingCommands, phoneConnected, match)
                MatchWatchButton(if (page == 0) "Opponent's plays →" else "← Your plays", Color(0xFF222222)) {
                    scope.launch { pager.animateScrollToPage(1 - page) }
                }
            }
        }
    }
}

@Composable
private fun MatchSyncStatus(pendingCommands: Int, phoneConnected: Boolean, match: WatchMatchSnapshot) {
    Text(
        when {
            pendingCommands > 0 -> "$pendingCommands plays pending sync"
            !phoneConnected -> "Phone offline. Plays will sync."
            else -> match.lastPlay ?: "First to ${match.settings.targetScore}"
        },
        color = if (!phoneConnected || pendingCommands > 0) Color(0xFFFF9500) else Color.LightGray,
        fontSize = 10.sp, textAlign = TextAlign.Center,
    )
}

@Composable
private fun MatchWatchButton(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String = title,
    onClick: () -> Unit,
) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp)
        .background(color.copy(alpha = if (enabled) color.alpha else 0.2f), RoundedCornerShape(12.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(4.dp)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text(title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}
