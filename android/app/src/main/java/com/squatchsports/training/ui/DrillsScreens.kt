package com.squatchsports.training.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squatchsports.training.model.DrillIcon
import com.squatchsports.training.model.DrillOption
import com.squatchsports.training.model.drills

@Composable
fun DrillsScreen(
    contentPadding: PaddingValues,
    onDrillSelected: (DrillOption) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item {
            Text(
                "DRILLS",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(drills, key = { it.name }) { drill ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDrillSelected(drill) }
                    .padding(horizontal = 8.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(drillIcon(drill.icon), contentDescription = null, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(drill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        drill.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!drill.isAvailable) {
                    Text("Soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
            HorizontalDivider(modifier = Modifier.padding(start = 48.dp))
        }
    }
}

@Composable
fun DrillDetailScreen(
    drill: DrillOption,
    contentPadding: PaddingValues,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(drillIcon(drill.icon), contentDescription = null, modifier = Modifier.size(34.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(drill.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(drill.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(drill.targetText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (drill.courtSpots.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Court Spots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    drill.courtSpots.forEachIndexed { index, spot ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("${index + 1}. $spot", modifier = Modifier.weight(1f))
                            Text(
                                "${drill.shotsPerSpot} shots",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "Total planned shots: ${drill.courtSpots.size * drill.shotsPerSpot}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            drill.instructions.forEach { step ->
                Text("• $step", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Button(
            onClick = onStart,
            enabled = drill.isAvailable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (drill.isAvailable) "Start Drill" else "Start Drill (Coming Soon)")
        }
    }
}

private fun drillIcon(icon: DrillIcon): ImageVector = when (icon) {
    DrillIcon.TARGET -> Icons.Default.TrackChanges
    DrillIcon.FREE_THROW -> Icons.Default.RadioButtonUnchecked
    DrillIcon.LOCATION -> Icons.Default.LocationOn
    DrillIcon.BOLT -> Icons.Default.Bolt
    DrillIcon.SHUFFLE -> Icons.Default.Shuffle
    DrillIcon.GRID -> Icons.Default.GridView
    DrillIcon.BASKETBALL -> Icons.Default.SportsBasketball
    DrillIcon.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
}
