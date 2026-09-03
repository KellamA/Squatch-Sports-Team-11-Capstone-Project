package com.squatchsports.training.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squatchsports.training.data.AppDataStore

@Composable
fun DashboardScreen(
    appData: AppDataStore,
    contentPadding: PaddingValues,
    onStartWorkout: () -> Unit,
    onDrills: () -> Unit,
    onHistory: () -> Unit,
    onAnalytics: () -> Unit,
    onGoals: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Squatch Sports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Choose what you want to work on today.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TodaySummaryCard(
            shotsMade = appData.todayMakes,
            shotsAttempted = appData.todayAttempts,
            sessionsThisWeek = appData.weeklySessionsCompleted,
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DashboardRow {
                DashboardCard("Start Workout", "Track shots live", Icons.Default.PlayCircle, onStartWorkout)
                DashboardCard("Drills", "Pick a drill", Icons.Default.SportsBasketball, onDrills)
            }
            DashboardRow {
                DashboardCard("History", "Past sessions", Icons.Default.History, onHistory)
                DashboardCard("Analytics", "Trends and stats", Icons.Default.BarChart, onAnalytics)
            }
            DashboardRow {
                DashboardCard("Goals", "Track your targets", Icons.Default.TrackChanges, onGoals)
                DashboardCard("Settings", "App preferences", Icons.Default.Settings, onSettings)
            }
        }

        MaterialCard {
            Text("Today’s Focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(appData.currentFocus, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodaySummaryCard(shotsMade: Int, shotsAttempted: Int, sessionsThisWeek: Int) {
    val percentage = if (shotsAttempted == 0) 0 else ((shotsMade.toDouble() / shotsAttempted) * 100).toInt()
    MaterialCard {
        Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Current workout progress",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryStat("$shotsMade", "Makes", Modifier.weight(1f))
            SummaryStat("$shotsAttempted", "Attempts", Modifier.weight(1f))
            SummaryStat("$percentage%", "FG", Modifier.weight(1f))
            SummaryStat("$sessionsThisWeek", "This Week", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun RowScope.DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 128.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun MaterialCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
