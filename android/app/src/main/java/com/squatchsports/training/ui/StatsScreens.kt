package com.squatchsports.training.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squatchsports.training.data.AppDataStore
import com.squatchsports.training.shared.WorkoutSession

@Composable
fun AnalyticsScreen(appData: AppDataStore, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Performance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AnalyticsCard("Makes", "${appData.totalMakes}", Modifier.weight(1f))
            AnalyticsCard("Attempts", "${appData.totalAttempts}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AnalyticsCard("FG%", "${appData.shootingPercentage}%", Modifier.weight(1f))
            AnalyticsCard("Sessions", "${appData.sessionsCompleted}", Modifier.weight(1f))
        }

        appData.bestSession?.let { best ->
            InfoBlock("Best Session", "${best.drill} • ${best.makes}/${best.attempts} • ${best.percentage}%")
        }
        appData.mostRecentSession?.let { recent ->
            InfoBlock(
                "Recent Trend",
                "Last workout was ${recent.drill} with ${recent.makes} makes on ${recent.attempts} attempts.",
            )
        }
        InfoBlock("Favorite Drill", appData.favoriteDrill)
        InfoBlock("Current Focus", appData.currentFocus)
    }
}

@Composable
private fun AnalyticsCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoBlock(title: String, value: String) {
    MaterialCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(value, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GoalsScreen(appData: AppDataStore, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GoalProgressCard("Daily Shot Goal", appData.todayAttempts, appData.dailyShotGoal, "Shots")
        GoalProgressCard("Weekly Session Goal", appData.weeklySessionsCompleted, appData.weeklySessionGoal, "Sessions")
        GoalProgressCard("Make Goal", appData.todayMakes, 100, "Makes")
        MaterialCard {
            Text("Target FG%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${appData.targetFgGoal}%",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            val message = if (appData.shootingPercentage >= appData.targetFgGoal) {
                "You are hitting your target."
            } else {
                "Keep shooting to close the gap."
            }
            Text(
                "Current FG% is ${appData.shootingPercentage}%. $message",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalProgressCard(title: String, current: Int, target: Int, label: String) {
    val progress = if (target <= 0) 0f else (current.toFloat() / target).coerceAtMost(1f)
    MaterialCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "$current / $target $label",
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

@Composable
fun HistoryScreen(appData: AppDataStore, contentPadding: PaddingValues) {
    if (appData.sessions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(38.dp))
            Text(
                "No workout history yet",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Complete a workout and your session history will show here.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            appData.sessions.forEach { session -> HistoryRow(session) }
        }
    }
}

@Composable
private fun HistoryRow(session: WorkoutSession) {
    val context = LocalContext.current
    val formattedDate = DateFormat.getMediumDateFormat(context).format(session.endDateMillis)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(
                        Icons.Default.SportsBasketball,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(session.drill, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${session.makes}/${session.attempts} shots", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "${session.percentage}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
