package com.squatchsports.training.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.squatchsports.training.data.AppDataStore
import com.squatchsports.training.shared.GoalProgress
import com.squatchsports.training.shared.GoalTracker
import com.squatchsports.training.shared.GoalType

private val GoalGreen = Color(0xFF34C759)

@Composable
fun GoalRing(goal: GoalProgress, size: Dp, strokeWidth: Dp, content: @Composable () -> Unit) {
    val animated by animateFloatAsState(goal.fraction, animationSpec = tween(900), label = "goalRing")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = strokeWidth,
        )
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxSize(),
            color = if (goal.isComplete) GoalGreen else StreakOrange,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round,
        )
        content()
    }
}

/** Compact ring row for the dashboard. Tapping it opens the Goals screen. */
@Composable
fun TodayGoalsCard(goals: List<GoalProgress>, onClick: () -> Unit) {
    val shown = goals.filter { it.type != GoalType.TARGET_FG }
    Box(Modifier.clickable(onClick = onClick)) {
        MaterialCard {
            Text("Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                shown.forEach { goal ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GoalRing(goal, size = 72.dp, strokeWidth = 7.dp) {
                            Text(
                                if (goal.isComplete) "✓" else "${(goal.fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (goal.isComplete) GoalGreen else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(goal.type.title, style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${goal.current}/${goal.target}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsScreen(appData: AppDataStore, contentPadding: PaddingValues) {
    var editing by remember { mutableStateOf<GoalType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val progress = appData.goalProgress
        val completed = progress.count { it.isComplete }
        Text(
            "$completed of ${progress.size} goals complete",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        progress.forEach { goal -> GoalDetailCard(goal, onEdit = { editing = goal.type }) }
    }

    editing?.let { type ->
        EditGoalDialog(
            type = type,
            current = appData.goalTargets.targetFor(type),
            onSave = { appData.updateGoal(type, it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun GoalDetailCard(goal: GoalProgress, onEdit: () -> Unit) {
    MaterialCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalRing(goal, size = 64.dp, strokeWidth = 6.dp) {
                Text(
                    if (goal.isComplete) "✓" else "${(goal.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (goal.isComplete) GoalGreen else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(goal.type.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(progressText(goal), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    statusText(goal),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (goal.isComplete) GoalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${goal.type.title} goal")
            }
        }
    }
}

private fun progressText(goal: GoalProgress): String = when (goal.type) {
    GoalType.TARGET_FG -> "${goal.current}% today • target ${goal.target}%"
    else -> "${goal.current} / ${goal.target} ${goal.type.unit}"
}

private fun statusText(goal: GoalProgress): String = when {
    goal.isComplete -> "Complete! 🏆"
    goal.type == GoalType.TARGET_FG && !goal.eligible -> "Take at least ${GoalTracker.MIN_FG_ATTEMPTS} shots today to count"
    goal.type == GoalType.TARGET_FG -> "${goal.target - goal.current}% to go"
    goal.type == GoalType.WEEKLY_SESSIONS -> "${goal.target - goal.current} more this week"
    else -> "${goal.target - goal.current} more today"
}

@Composable
private fun EditGoalDialog(type: GoalType, current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember(type) { mutableStateOf(current.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null && value in type.range

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${type.title} goal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(4) },
                label = { Text("Target (${type.unit})") },
                supportingText = { Text("Between ${type.range.first} and ${type.range.last}") },
                isError = !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSave) }, enabled = valid) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
