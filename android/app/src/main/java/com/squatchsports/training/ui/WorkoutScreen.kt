package com.squatchsports.training.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.data.AppDataStore
import com.squatchsports.training.shared.CourtPositions
import com.squatchsports.training.shared.WorkoutSession
import java.util.Date

private const val SHOTS_PER_POSITION = 5

private class PhoneWorkoutState(
    private val selectedDrill: String,
    private val appData: AppDataStore,
    private val connectivity: WorkoutConnectivity,
) {
    var workoutActive by mutableStateOf(false)
        private set
    var hasWorkout by mutableStateOf(false)
        private set
    var workoutStartDateMillis by mutableLongStateOf(0L)
        private set
    var lastReceivedValue by mutableStateOf<Int?>(null)
        private set
    var makes by mutableIntStateOf(0)
        private set
    var misses by mutableIntStateOf(0)
        private set
    var swishes by mutableIntStateOf(0)
        private set
    var currentPositionIndex by mutableIntStateOf(0)
        private set
    private var shotsAtCurrentPosition by mutableIntStateOf(0)

    val totalMakes: Int get() = makes + swishes
    val attempts: Int get() = totalMakes + misses
    val fgPercent: Double get() = if (attempts == 0) 0.0 else totalMakes.toDouble() / attempts * 100.0

    val subtitleText: String
        get() {
            val positions = CourtPositions.forDrill(selectedDrill)
            return when (selectedDrill) {
                "Spot Shooting" -> {
                    val totalShots = maxOf(1, positions.size * SHOTS_PER_POSITION)
                    if (workoutActive) {
                        "Position ${minOf(currentPositionIndex + 1, positions.size)} of ${positions.size} • $attempts/$totalShots shots"
                    } else "$totalShots total shots ($SHOTS_PER_POSITION per spot)"
                }
                "Free Throws" -> if (workoutActive) "Shot $attempts of $SHOTS_PER_POSITION" else "5 shot routine"
                "Form Shooting" -> if (workoutActive) "Shot $attempts of $SHOTS_PER_POSITION" else "Close range mechanics"
                "Catch & Shoot" -> if (workoutActive) "Shot $attempts of $SHOTS_PER_POSITION" else "Quick feet and release"
                "Off the Dribble" -> if (workoutActive) "Shot $attempts of $SHOTS_PER_POSITION" else "Create space and shoot"
                "Midrange Series" -> {
                    val totalShots = maxOf(1, positions.size * SHOTS_PER_POSITION)
                    if (workoutActive) {
                        "Position ${minOf(currentPositionIndex + 1, positions.size)} of ${positions.size} • $attempts/$totalShots shots"
                    } else "$totalShots total midrange shots"
                }
                "3PT Series" -> {
                    val totalShots = maxOf(1, positions.size * SHOTS_PER_POSITION)
                    if (workoutActive) {
                        "Position ${minOf(currentPositionIndex + 1, positions.size)} of ${positions.size} • $attempts/$totalShots shots"
                    } else "$totalShots total threes"
                }
                "Finishing" -> if (workoutActive) "Shot $attempts of $SHOTS_PER_POSITION" else "Touch and angles at the rim"
                else -> if (workoutActive) "Active drill session" else "Ready to start"
            }
        }

    fun startWorkout() {
        val now = System.currentTimeMillis()
        workoutStartDateMillis = now
        hasWorkout = true
        workoutActive = true
        makes = 0
        misses = 0
        swishes = 0
        lastReceivedValue = null
        currentPositionIndex = 0
        shotsAtCurrentPosition = 0

        val initialPosition = CourtPositions.forDrill(selectedDrill).firstOrNull()
        connectivity.sendWorkoutStarted(selectedDrill, initialPosition)
    }

    fun receiveWatchShot(value: Int) {
        lastReceivedValue = value
        if (workoutActive) applyShot(value)
    }

    fun recordManualShot(value: Int) {
        if (!workoutActive) return
        applyShot(value)
        connectivity.sendShotToWatch(value)
    }

    fun finishWorkout() {
        val endDate = System.currentTimeMillis()
        if (workoutActive && attempts > 0) {
            appData.addSession(
                WorkoutSession(
                    drill = selectedDrill,
                    makes = totalMakes,
                    misses = misses,
                    swishes = swishes,
                    attempts = attempts,
                    startDateMillis = workoutStartDateMillis.takeIf { it > 0 } ?: endDate,
                    endDateMillis = endDate,
                ),
            )
        }
        workoutActive = false
        connectivity.sendWorkoutStopped()
    }

    private fun applyShot(value: Int) {
        when (value) {
            0 -> misses += 1
            1 -> makes += 1
            2 -> swishes += 1
            else -> return
        }

        shotsAtCurrentPosition += 1
        val positions = CourtPositions.forDrill(selectedDrill)
        if (shotsAtCurrentPosition >= SHOTS_PER_POSITION) {
            shotsAtCurrentPosition = 0
            currentPositionIndex += 1
            if (currentPositionIndex >= positions.size) {
                finishWorkout()
            } else {
                connectivity.sendPositionUpdate(positions[currentPositionIndex])
            }
        }
    }
}

@Composable
fun WorkoutScreen(
    selectedDrill: String,
    appData: AppDataStore,
    connectivity: WorkoutConnectivity,
    contentPadding: PaddingValues,
) {
    val state = remember(selectedDrill, appData, connectivity) {
        PhoneWorkoutState(selectedDrill, appData, connectivity)
    }
    val shotListener = remember(state) { { value: Int -> state.receiveWatchShot(value) } }
    DisposableEffect(connectivity, shotListener) {
        connectivity.addShotListener(shotListener)
        onDispose { connectivity.removeShotListener(shotListener) }
    }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(selectedDrill, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(state.subtitleText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (state.workoutActive) "Active Workout" else "No Active Workout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(Modifier.fillMaxWidth()) {
                    WorkoutStat("Makes", "${state.totalMakes}", Modifier.weight(1f))
                    WorkoutStat("Misses", "${state.misses}", Modifier.weight(1f))
                    WorkoutStat("Swish", "${state.swishes}", Modifier.weight(1f))
                    WorkoutStat("Att", "${state.attempts}", Modifier.weight(1f))
                }
                Text(
                    "FG%: ${"%.1f".format(state.fgPercent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = state::startWorkout,
                enabled = !state.workoutActive,
                modifier = Modifier.weight(1f),
            ) { Text("Start Workout", textAlign = TextAlign.Center) }
            OutlinedButton(
                onClick = state::finishWorkout,
                enabled = state.workoutActive,
                modifier = Modifier.weight(1f),
            ) { Text("Stop Workout", textAlign = TextAlign.Center) }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                state.lastReceivedValue?.let { "Last watch code: $it" } ?: "Waiting for watch data…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.workoutActive) {
                val positions = CourtPositions.forDrill(selectedDrill)
                if (state.currentPositionIndex < positions.size) {
                    val current = positions[state.currentPositionIndex]
                    Text(
                        "Position: ${current.name ?: "Unknown"} (${state.currentPositionIndex + 1}/${positions.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Quick Log (Debug)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { state.recordManualShot(1) },
                    enabled = state.workoutActive,
                    modifier = Modifier.weight(1f),
                ) { Text("MAKE") }
                OutlinedButton(
                    onClick = { state.recordManualShot(0) },
                    enabled = state.workoutActive,
                    modifier = Modifier.weight(1f),
                ) { Text("MISS") }
                OutlinedButton(
                    onClick = { state.recordManualShot(2) },
                    enabled = state.workoutActive,
                    modifier = Modifier.weight(1f),
                ) { Text("SWISH") }
            }
        }

        if (state.hasWorkout) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Workout Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Drill: $selectedDrill", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val dateText = if (state.workoutStartDateMillis > 0) {
                    val date = Date(state.workoutStartDateMillis)
                    "${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"
                } else ""
                Text("Started: $dateText", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Makes: ${state.totalMakes}  Misses: ${state.misses}  Swishes: ${state.swishes}")
                Text(
                    "Attempts: ${state.attempts}   FG%: ${"%.1f".format(state.fgPercent)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun WorkoutStat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
