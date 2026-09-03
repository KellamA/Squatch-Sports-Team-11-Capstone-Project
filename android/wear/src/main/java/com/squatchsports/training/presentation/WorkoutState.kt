package com.squatchsports.training.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.squatchsports.training.connectivity.WorkoutConnectivity
import com.squatchsports.training.shared.CourtPosition
import com.squatchsports.training.shared.CourtPositions
import com.squatchsports.training.shared.ShotDirection
import java.util.UUID

private const val SHOTS_PER_POSITION = 5

data class PositionStats(
    val id: String = UUID.randomUUID().toString(),
    val position: CourtPosition,
    val makes: Int = 0,
    val misses: Int = 0,
    val swishes: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val totalShots: Int get() = makes + misses + swishes
    val makePercentage: Double
        get() = if (totalShots == 0) 0.0 else (makes + swishes).toDouble() / totalShots * 100.0
    val swishPercentage: Double
        get() = if (totalShots == 0) 0.0 else swishes.toDouble() / totalShots * 100.0

    fun recording(direction: ShotDirection): PositionStats = when (direction) {
        ShotDirection.MAKE -> copy(makes = makes + 1)
        ShotDirection.MISS -> copy(misses = misses + 1)
        ShotDirection.SWISH -> copy(swishes = swishes + 1)
    }
}

data class WatchWorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long? = null,
    val positionStats: List<PositionStats> = emptyList(),
    val drillName: String? = null,
) {
    val totalShots: Int get() = positionStats.sumOf { it.totalShots }
    val totalMakes: Int get() = positionStats.sumOf { it.makes + it.swishes }
    val totalMisses: Int get() = positionStats.sumOf { it.misses }
    val totalSwishes: Int get() = positionStats.sumOf { it.swishes }
    val overallPercentage: Double
        get() = if (totalShots == 0) 0.0 else totalMakes.toDouble() / totalShots * 100.0
}

class WearWorkoutState(private val connectivity: WorkoutConnectivity) {
    var lastShotStatus by mutableStateOf<ShotDirection?>(null)
        private set
    var selectedPosition by mutableStateOf<CourtPosition?>(null)
        private set
    var shotCount by mutableIntStateOf(0)
        private set
    var currentSession by mutableStateOf<WatchWorkoutSession?>(null)
        private set
    var currentPositionStats by mutableStateOf<PositionStats?>(null)
        private set
    var showSummary by mutableStateOf(false)
        private set
    var currentDrill by mutableStateOf<String?>(null)
        private set
    var drillPositions by mutableStateOf<List<CourtPosition>>(emptyList())
        private set
    var currentPositionIndex by mutableIntStateOf(0)
        private set

    val shotsPerPosition: Int get() = SHOTS_PER_POSITION

    fun handleWorkoutChange(active: Boolean) {
        if (active) {
            showSummary = false
            currentSession = WatchWorkoutSession(drillName = currentDrill)
            shotCount = 0
            currentPositionStats = null
            currentPositionIndex = 0
            selectedPosition = null
            lastShotStatus = null

            if (currentDrill == null) {
                val general = CourtPosition(0, 0, "General", 0.5, 0.5)
                selectedPosition = general
                currentPositionStats = PositionStats(position = general)
            }
        } else if (!showSummary) {
            var finalSession = currentSession ?: return
            currentPositionStats?.takeIf { it.totalShots > 0 }?.let { stats ->
                finalSession = finalSession.copy(positionStats = finalSession.positionStats + stats)
            }
            if (finalSession.positionStats.isNotEmpty()) {
                finalSession = finalSession.copy(endDateMillis = System.currentTimeMillis())
                currentSession = finalSession
                showSummary = true
            } else {
                currentPositionStats = null
                selectedPosition = null
            }
        }
    }

    fun handleDrill(name: String) {
        currentDrill = name
        drillPositions = CourtPositions.forDrill(name)
        currentSession = currentSession?.copy(drillName = name)
    }

    fun handlePosition(position: CourtPosition, workoutActive: Boolean) {
        if (!workoutActive) return
        selectedPosition = position
        currentPositionStats = PositionStats(position = position)
        shotCount = 0
    }

    fun recordLocalShot(direction: ShotDirection) {
        recordShot(direction)
        connectivity.sendShotToPhone(direction.code)
    }

    fun recordPhoneShot(code: Int, workoutActive: Boolean) {
        if (!workoutActive) return
        ShotDirection.fromCode(code)?.let(::recordShot)
    }

    fun updateGeneralPosition(position: CourtPosition, workoutActive: Boolean) {
        if (!workoutActive || currentDrill != null) return
        selectedPosition = position
        if (currentPositionStats == null) {
            currentPositionStats = PositionStats(position = position)
            shotCount = 0
        }
        connectivity.sendPositionUpdate(position)
    }

    fun doneWithSummary() {
        showSummary = false
        currentSession = null
        currentPositionStats = null
        selectedPosition = null
        shotCount = 0
        currentPositionIndex = 0
        currentDrill = null
        drillPositions = emptyList()
        lastShotStatus = null
        connectivity.sendWorkoutStopped()
    }

    private fun recordShot(direction: ShotDirection) {
        val stats = currentPositionStats ?: return
        currentPositionStats = stats.recording(direction)
        lastShotStatus = direction
        shotCount += 1
        if (shotCount >= SHOTS_PER_POSITION) handlePositionComplete()
    }

    private fun handlePositionComplete() {
        currentPositionStats?.let { stats ->
            currentSession = currentSession?.let { session ->
                session.copy(positionStats = session.positionStats + stats)
            }
        }

        if (currentDrill != null && drillPositions.isNotEmpty()) {
            currentPositionIndex += 1
            if (currentPositionIndex >= drillPositions.size) {
                currentSession = currentSession?.copy(endDateMillis = System.currentTimeMillis())
                showSummary = true
                selectedPosition = null
                currentPositionStats = null
            } else {
                val next = drillPositions[currentPositionIndex]
                selectedPosition = next
                currentPositionStats = PositionStats(position = next)
                shotCount = 0
                connectivity.sendPositionUpdate(next)
            }
        } else {
            shotCount = 0
            selectedPosition?.let { currentPositionStats = PositionStats(position = it) }
        }
    }
}
