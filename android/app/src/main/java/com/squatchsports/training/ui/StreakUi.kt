package com.squatchsports.training.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squatchsports.training.shared.StreakCalculator
import com.squatchsports.training.shared.StreakResult
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

val StreakOrange = Color(0xFFFF9500)

@Composable
fun StreakCard(streak: StreakResult, trainingDays: Set<LocalDate>) {
    val active = streak.current > 0
    val flameScale = if (active) {
        val transition = rememberInfiniteTransition(label = "flame")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "flameScale",
        ).value
    } else {
        1f
    }

    MaterialCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🔥",
                fontSize = 40.sp,
                modifier = Modifier.scale(flameScale),
                color = if (active) Color.Unspecified else Color.Gray.copy(alpha = 0.4f),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${streak.current}-day streak",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (active) StreakOrange else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    streakStatus(streak, LocalDate.now()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${streak.best}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(14.dp))
        WeekStrip(trainingDays, LocalDate.now())

        StreakCalculator.nextMilestone(streak.current)?.let { next ->
            val previous = StreakCalculator.MILESTONES.lastOrNull { it <= streak.current } ?: 0
            val remaining = next - streak.current
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (streak.current - previous).toFloat() / (next - previous) },
                modifier = Modifier.fillMaxWidth(),
                color = StreakOrange,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$remaining more training day${if (remaining == 1) "" else "s"} to reach $next",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The last 7 days: filled = trained, outlined = no workout, bold ring = today. */
@Composable
private fun WeekStrip(trainingDays: Set<LocalDate>, today: LocalDate) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (6 downTo 0).map { today.minusDays(it.toLong()) }.forEach { day ->
            val trained = day in trainingDays
            val isToday = day == today
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(if (trained) StreakOrange else Color.Transparent, CircleShape)
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (trained) Text("🔥", fontSize = 14.sp)
                }
            }
        }
    }
}

private fun streakStatus(streak: StreakResult, today: LocalDate): String {
    val last = streak.lastTrainingDay
    if (streak.current == 0 || last == null) return "Log a workout to start a streak"
    return when (ChronoUnit.DAYS.between(last, today)) {
        0L -> "You trained today. Streak is safe!"
        1L -> "Train today to grow it, or take a rest day"
        else -> "Rest day used. Train today to keep it alive!"
    }
}
