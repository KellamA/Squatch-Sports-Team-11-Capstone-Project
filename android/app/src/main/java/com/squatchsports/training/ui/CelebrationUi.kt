package com.squatchsports.training.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squatchsports.training.data.Celebration
import com.squatchsports.training.shared.GoalType
import com.squatchsports.training.shared.StreakCelebration
import kotlinx.coroutines.delay

private const val CELEBRATION_MILLIS = 3500L

/** Full-screen popup for streak and goal celebrations. Tap anywhere or wait to dismiss. */
@Composable
fun CelebrationOverlay(celebration: Celebration?, onDismiss: () -> Unit) {
    // Keep the last value so the fade-out still has content to draw.
    var shown by remember { mutableStateOf(celebration) }
    if (celebration != null) shown = celebration

    LaunchedEffect(celebration) {
        if (celebration != null) {
            delay(CELEBRATION_MILLIS)
            onDismiss()
        }
    }

    AnimatedVisibility(visible = celebration != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) togetherWith
                        scaleOut()
                },
                label = "celebration",
            ) { current ->
                when (current) {
                    is Celebration.Streak -> StreakCelebrationCard(current.streak)
                    is Celebration.Goals -> GoalCelebrationCard(current)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun StreakCelebrationCard(celebration: StreakCelebration) {
    val (title, subtitle) = when {
        celebration.isMilestone -> "Milestone reached!" to "${celebration.streak} training days in a row. Squatch is proud."
        celebration.isNewBest -> "New personal best!" to "Your longest streak ever: ${celebration.streak} days."
        celebration.streak == 1 -> "Streak started!" to "Come back tomorrow to keep it going."
        else -> "Streak continued!" to "Nice work, keep the fire going."
    }
    CelebrationCard(emoji = "🔥", headline = "${celebration.streak}", caption = "day streak", title = title, subtitle = subtitle)
}

@Composable
private fun GoalCelebrationCard(celebration: Celebration.Goals) {
    val goals = celebration.completed
    val subtitle = goals.joinToString("\n") { goal ->
        when (goal.type) {
            GoalType.TARGET_FG -> "${goal.type.title}: ${goal.current}% (target ${goal.target}%)"
            else -> "${goal.type.title}: ${goal.current} / ${goal.target} ${goal.type.unit}"
        }
    }
    CelebrationCard(
        emoji = "🏆",
        headline = if (goals.size == 1) "Goal" else "${goals.size} Goals",
        caption = "complete",
        title = if (goals.size == 1) "You hit your ${goals.single().type.title} goal!" else "You crushed ${goals.size} goals!",
        subtitle = subtitle,
    )
}

@Composable
private fun CelebrationCard(emoji: String, headline: String, caption: String, title: String, subtitle: String) {
    Card(
        modifier = Modifier.padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, fontSize = 64.sp)
            Text(headline, fontSize = 44.sp, fontWeight = FontWeight.Black, color = StreakOrange)
            Text(caption, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text("Tap to continue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
