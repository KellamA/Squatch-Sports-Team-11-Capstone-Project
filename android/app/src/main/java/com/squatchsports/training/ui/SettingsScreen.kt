package com.squatchsports.training.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squatchsports.training.data.AppDataStore

@Composable
fun SettingsScreen(appData: AppDataStore, contentPadding: PaddingValues) {
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var soundEnabled by rememberSaveable { mutableStateOf(true) }
    var watchSyncEnabled by rememberSaveable { mutableStateOf(true) }
    var hapticsEnabled by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SettingsSection("Preferences") {
            SettingsToggle("Notifications", notificationsEnabled) { notificationsEnabled = it }
            HorizontalDivider()
            SettingsToggle("Sound Effects", soundEnabled) { soundEnabled = it }
            HorizontalDivider()
            SettingsToggle("Wear OS Sync", watchSyncEnabled) { watchSyncEnabled = it }
            HorizontalDivider()
            SettingsToggle("Haptics", hapticsEnabled) { hapticsEnabled = it }
        }

        SettingsSection("Watch Status") {
            SettingsValueRow(
                label = "Connection",
                value = if (watchSyncEnabled) "Connected" else "Disconnected",
                valueColor = if (watchSyncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            SettingsValueRow("Last Sync", "Just now")
        }

        SettingsSection("Data") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { appData.clearAllSessions() }
                    .padding(16.dp),
            ) {
                Text("Clear All History", color = MaterialTheme.colorScheme.error)
            }
        }

        SettingsSection("About") {
            SettingsValueRow("Version", "1.0")
            HorizontalDivider()
            SettingsValueRow("App", "Squatch Sports")
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = valueColor)
    }
}
