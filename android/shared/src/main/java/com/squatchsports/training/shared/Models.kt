package com.squatchsports.training.shared

import java.util.UUID

/** Shot codes intentionally match the Apple Watch protocol. */
enum class ShotDirection(val code: Int) {
    MISS(0),
    MAKE(1),
    SWISH(2);

    companion object {
        fun fromCode(code: Int): ShotDirection? = entries.firstOrNull { it.code == code }
    }
}

data class CourtPosition(
    val row: Int,
    val column: Int,
    val name: String? = null,
    val rowPercent: Double,
    val columnPercent: Double,
)

object CourtPositions {
    val spotShooting = listOf(
        CourtPosition(0, 0, "Left Corner", 0.20, 0.10),
        CourtPosition(0, 1, "Left Wing", 0.54, 0.27),
        CourtPosition(0, 2, "Top of Key", 0.64, 0.50),
        CourtPosition(0, 3, "Right Wing", 0.54, 0.73),
        CourtPosition(0, 4, "Right Corner", 0.20, 0.90),
    )

    val freeThrow = listOf(
        CourtPosition(0, 0, "Free Throw Line", 0.37, 0.50),
    )

    val formShooting = listOf(
        CourtPosition(0, 0, "Close Range", 0.22, 0.50),
    )

    val midrangeSeries = listOf(
        CourtPosition(0, 0, "Left Elbow", 0.43, 0.32),
        CourtPosition(0, 1, "Right Elbow", 0.43, 0.68),
        CourtPosition(0, 2, "Left Short Corner", 0.22, 0.22),
        CourtPosition(0, 3, "Right Short Corner", 0.22, 0.78),
    )

    val threePointSeries = listOf(
        CourtPosition(0, 0, "Left Corner", 0.20, 0.10),
        CourtPosition(0, 1, "Left Wing", 0.54, 0.27),
        CourtPosition(0, 2, "Top of Key", 0.64, 0.50),
        CourtPosition(0, 3, "Right Wing", 0.54, 0.73),
        CourtPosition(0, 4, "Right Corner", 0.20, 0.90),
    )

    val catchAndShoot = listOf(
        CourtPosition(0, 0, "Catch & Shoot Spot", 0.50, 0.50),
    )

    val offTheDribble = listOf(
        CourtPosition(0, 0, "Off Dribble Spot", 0.50, 0.50),
    )

    val finishing = listOf(
        CourtPosition(0, 0, "Rim Finish", 0.20, 0.50),
    )

    val defaultPosition = CourtPosition(0, 0, "Center", 0.50, 0.50)

    fun forDrill(drillName: String): List<CourtPosition> = when (drillName) {
        "Spot Shooting" -> spotShooting
        "Free Throws" -> freeThrow
        "Form Shooting" -> formShooting
        "Midrange Series" -> midrangeSeries
        "3PT Series" -> threePointSeries
        "Catch & Shoot" -> catchAndShoot
        "Off the Dribble" -> offTheDribble
        "Finishing" -> finishing
        else -> listOf(defaultPosition)
    }
}

data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val drill: String,
    val makes: Int,
    val misses: Int,
    val swishes: Int,
    val attempts: Int,
    val startDateMillis: Long,
    val endDateMillis: Long,
) {
    val percentage: Int
        get() = if (attempts == 0) 0 else ((makes.toDouble() / attempts) * 100).toInt()
}
