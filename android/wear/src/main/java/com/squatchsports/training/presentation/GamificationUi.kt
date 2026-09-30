package com.squatchsports.training.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.squatchsports.training.connectivity.CelebrationEvent
import com.squatchsports.training.shared.GoalProgress
import com.squatchsports.training.shared.GoalType
import com.squatchsports.training.shared.WatchStats
import kotlinx.coroutines.delay

private val StreakOrange = Color(0xFFFF9500)
private val GoalGreen = Color(0xFF34C759)
private const val WATCH_CELEBRATION_MILLIS = 3500L

/** Streak and goal rings shown on the watch while waiting for a workout. */
@Composable
fun WatchStatsPanel(stats: WatchStats) {
    val active = stats.currentStreak > 0
    // Pulse a few times, then stop: endless animations drain the watch battery and slow the device.
    val flameScale = remember { Animatable(1f) }
    LaunchedEffect(active) {
        if (active) {
            repeat(3) {
                flameScale.animateTo(1.15f, tween(350, easing = FastOutSlowInEasing))
                flameScale.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "🔥",
                fontSize = 22.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = flameScale.value
                    scaleY = flameScale.value
                },
                color = if (active) Color.Unspecified else Color.Gray.copy(alpha = 0.5f),
            )
            Text(
                "${stats.currentStreak}",
                color = if (active) StreakOrange else Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            "day streak • best ${stats.bestStreak}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
        )
        Text(
            when {
                !active -> "Log a workout to start one"
                stats.trainedToday -> "Streak is safe today"
                else -> "Train today to keep it going"
            },
            color = if (stats.trainedToday) GoalGreen else Color.White.copy(alpha = 0.55f),
            fontSize = 9.sp,
        )

        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stats.goals.filter { it.type != GoalType.TARGET_FG }.forEach { goal ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WatchGoalRing(goal, size = 34.dp)
                    Text(ringLabel(goal.type), color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun WatchGoalRing(goal: GoalProgress, size: Dp) {
    val progress by animateFloatAsState(goal.fraction, animationSpec = tween(900), label = "watchRing")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                if (goal.isComplete) GoalGreen else StreakOrange,
                -90f,
                360f * progress,
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            if (goal.isComplete) "✓" else "${(goal.fraction * 100).toInt()}%",
            color = if (goal.isComplete) GoalGreen else Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun ringLabel(type: GoalType): String = when (type) {
    GoalType.DAILY_SHOTS -> "Shots"
    GoalType.DAILY_MAKES -> "Makes"
    GoalType.WEEKLY_SESSIONS -> "Week"
    GoalType.TARGET_FG -> "FG%"
}

/** Full-screen celebration mirrored from the phone. Vibrates on arrival; tap or wait to dismiss. */
@Composable
fun WatchCelebrationOverlay(event: CelebrationEvent?) {
    var visible by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(event?.sequence) {
        if (event == null) return@LaunchedEffect
        visible = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(WATCH_CELEBRATION_MILLIS)
        visible = false
    }

    AnimatedVisibility(visible = visible && event != null, enter = fadeIn(), exit = fadeOut()) {
        val current = event ?: return@AnimatedVisibility
        val message = current.message
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { visible = false }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            val appear = remember(current.sequence) { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = appear,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(message.emoji, fontSize = 34.sp)
                    Text(message.headline, color = StreakOrange, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(message.caption, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        message.title,
                        modifier = Modifier.padding(top = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        message.subtitle,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )
                }
            }
        }
    }
}
