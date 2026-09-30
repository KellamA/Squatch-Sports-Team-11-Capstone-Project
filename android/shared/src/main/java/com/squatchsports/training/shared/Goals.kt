package com.squatchsports.training.shared

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class GoalPeriod { DAY, WEEK }

enum class GoalType(
    val title: String,
    val unit: String,
    val period: GoalPeriod,
    val range: IntRange,
) {
    DAILY_SHOTS("Daily Shots", "shots", GoalPeriod.DAY, 10..2000),
    DAILY_MAKES("Daily Makes", "makes", GoalPeriod.DAY, 5..1000),
    WEEKLY_SESSIONS("Weekly Sessions", "sessions", GoalPeriod.WEEK, 1..21),
    TARGET_FG("Today's FG%", "%", GoalPeriod.DAY, 10..100),
}

data class GoalTargets(
    val dailyShots: Int = 200,
    val dailyMakes: Int = 100,
    val weeklySessions: Int = 5,
    val targetFgPercent: Int = 75,
) {
    fun targetFor(type: GoalType): Int = when (type) {
        GoalType.DAILY_SHOTS -> dailyShots
        GoalType.DAILY_MAKES -> dailyMakes
        GoalType.WEEKLY_SESSIONS -> weeklySessions
        GoalType.TARGET_FG -> targetFgPercent
    }

    fun with(type: GoalType, value: Int): GoalTargets = when (type) {
        GoalType.DAILY_SHOTS -> copy(dailyShots = value)
        GoalType.DAILY_MAKES -> copy(dailyMakes = value)
        GoalType.WEEKLY_SESSIONS -> copy(weeklySessions = value)
        GoalType.TARGET_FG -> copy(targetFgPercent = value)
    }
}

/**
 * Progress toward one goal. [eligible] is false when the goal cannot be completed yet
 * (FG% needs at least [GoalTracker.MIN_FG_ATTEMPTS] shots today so one lucky shot does not count).
 */
data class GoalProgress(
    val type: GoalType,
    val current: Int,
    val target: Int,
    val eligible: Boolean = true,
) {
    val fraction: Float
        get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)

    val isComplete: Boolean
        get() = eligible && target > 0 && current >= target
}

object GoalTracker {
    const val MIN_FG_ATTEMPTS = 10

    fun defaultWeekStart(locale: Locale = Locale.getDefault()): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    fun progress(
        sessions: Collection<WorkoutSession>,
        targets: GoalTargets,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        weekStart: DayOfWeek = defaultWeekStart(),
    ): List<GoalProgress> {
        val todays = sessions.filter { StreakCalculator.toLocalDate(it.endDateMillis, zone) == today }
        val startOfWeek = startOfWeek(today, weekStart)
        val thisWeek = sessions.count {
            val day = StreakCalculator.toLocalDate(it.endDateMillis, zone)
            !day.isBefore(startOfWeek) && day.isBefore(startOfWeek.plusWeeks(1))
        }
        val todayAttempts = todays.sumOf { it.attempts }
        val todayMakes = todays.sumOf { it.makes }
        val todayFg = if (todayAttempts == 0) 0 else todayMakes * 100 / todayAttempts

        return listOf(
            GoalProgress(GoalType.DAILY_SHOTS, todayAttempts, targets.dailyShots),
            GoalProgress(GoalType.DAILY_MAKES, todayMakes, targets.dailyMakes),
            GoalProgress(GoalType.WEEKLY_SESSIONS, thisWeek, targets.weeklySessions),
            GoalProgress(GoalType.TARGET_FG, todayFg, targets.targetFgPercent, eligible = todayAttempts >= MIN_FG_ATTEMPTS),
        )
    }

    /** Identifies the day or week a goal belongs to, so each goal is celebrated once per period. */
    fun periodKey(type: GoalType, today: LocalDate, weekStart: DayOfWeek = defaultWeekStart()): String {
        val periodStart = when (type.period) {
            GoalPeriod.DAY -> today
            GoalPeriod.WEEK -> startOfWeek(today, weekStart)
        }
        return "${type.name}@$periodStart"
    }

    /** Goals that are complete and have not been celebrated yet in their current period. */
    fun newlyCompleted(
        progress: List<GoalProgress>,
        today: LocalDate,
        alreadyCelebrated: Set<String>,
        weekStart: DayOfWeek = defaultWeekStart(),
    ): List<GoalProgress> = progress.filter {
        it.isComplete && periodKey(it.type, today, weekStart) !in alreadyCelebrated
    }

    private fun startOfWeek(day: LocalDate, weekStart: DayOfWeek): LocalDate =
        day.with(TemporalAdjusters.previousOrSame(weekStart))
}
