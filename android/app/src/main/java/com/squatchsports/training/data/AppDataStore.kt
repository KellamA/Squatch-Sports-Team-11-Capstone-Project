package com.squatchsports.training.data

import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.squatchsports.training.shared.GoalProgress
import com.squatchsports.training.shared.GoalTargets
import com.squatchsports.training.shared.GoalTracker
import com.squatchsports.training.shared.GoalType
import com.squatchsports.training.shared.StreakCalculator
import com.squatchsports.training.shared.StreakResult
import com.squatchsports.training.shared.WorkoutSession
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class AppDataStore(context: Context) {
    private val preferences = context.getSharedPreferences("squatch_data", Context.MODE_PRIVATE)

    val sessions = mutableStateListOf<WorkoutSession>()

    var goalTargets by mutableStateOf(
        GoalTargets(
            dailyShots = preferences.getInt(KEY_DAILY_SHOT_GOAL, 200),
            dailyMakes = preferences.getInt(KEY_DAILY_MAKE_GOAL, 100),
            weeklySessions = preferences.getInt(KEY_WEEKLY_SESSION_GOAL, 5),
            targetFgPercent = preferences.getInt(KEY_TARGET_FG_GOAL, 75),
        ),
    )
        private set

    val dailyShotGoal: Int get() = goalTargets.dailyShots
    val weeklySessionGoal: Int get() = goalTargets.weeklySessions
    val targetFgGoal: Int get() = goalTargets.targetFgPercent

    /** Popups waiting to be shown after a workout, oldest first. The UI shows the first one. */
    val celebrations = mutableStateListOf<Celebration>()

    private val celebratedGoals: MutableSet<String> =
        preferences.getStringSet(KEY_CELEBRATED_GOALS, emptySet())!!.toMutableSet()

    init {
        sessions.addAll(readSessions())
    }

    fun addSession(session: WorkoutSession) {
        val before = streak
        sessions.add(0, session)
        persistSessions()
        StreakCalculator.celebrationFor(before, streak)?.let { celebrations.add(Celebration.Streak(it)) }
        celebrateCompletedGoals()
    }

    fun clearAllSessions() {
        sessions.clear()
        persistSessions()
        resetCelebratedGoals()
    }

    fun dismissCelebration() {
        if (celebrations.isNotEmpty()) celebrations.removeAt(0)
    }

    fun updateGoal(type: GoalType, value: Int) {
        val clamped = value.coerceIn(type.range)
        goalTargets = goalTargets.with(type, clamped)
        val key = when (type) {
            GoalType.DAILY_SHOTS -> KEY_DAILY_SHOT_GOAL
            GoalType.DAILY_MAKES -> KEY_DAILY_MAKE_GOAL
            GoalType.WEEKLY_SESSIONS -> KEY_WEEKLY_SESSION_GOAL
            GoalType.TARGET_FG -> KEY_TARGET_FG_GOAL
        }
        preferences.edit { putInt(key, clamped) }
    }

    val goalProgress: List<GoalProgress>
        get() = GoalTracker.progress(sessions, goalTargets, LocalDate.now())

    private fun celebrateCompletedGoals() {
        val today = LocalDate.now()
        // Only the current day/week matters; drop records from earlier periods.
        celebratedGoals.retainAll(GoalType.entries.map { GoalTracker.periodKey(it, today) }.toSet())
        val completed = GoalTracker.newlyCompleted(goalProgress, today, celebratedGoals)
        completed.forEach { celebratedGoals += GoalTracker.periodKey(it.type, today) }
        persistCelebratedGoals()
        if (completed.isNotEmpty()) celebrations.add(Celebration.Goals(completed))
    }

    private fun resetCelebratedGoals() {
        celebratedGoals.clear()
        persistCelebratedGoals()
    }

    private fun persistCelebratedGoals() {
        preferences.edit { putStringSet(KEY_CELEBRATED_GOALS, celebratedGoals.toSet()) }
    }

    /**
     * Debug helper for testing and demos: replaces history with 6 training days over the last week,
     * with a rest day 4 days ago. The current streak becomes 6, so finishing a workout today hits the
     * 7-day milestone.
     */
    fun loadDemoHistory() {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val demo = listOf(7L, 6L, 5L, 3L, 2L, 1L).map { daysAgo ->
            val end = today.minusDays(daysAgo).atTime(17, 30).atZone(zone)
            WorkoutSession(
                drill = if (daysAgo % 2 == 0L) "Spot Shooting" else "Free Throws",
                makes = 15 + daysAgo.toInt(),
                misses = 10 - daysAgo.toInt(),
                swishes = 3,
                attempts = 25,
                startDateMillis = end.minusMinutes(30).toInstant().toEpochMilli(),
                endDateMillis = end.toInstant().toEpochMilli(),
            )
        }
        sessions.clear()
        sessions.addAll(demo.sortedByDescending { it.endDateMillis })
        persistSessions()
        resetCelebratedGoals()
    }

    val streak: StreakResult
        get() = StreakCalculator.calculateFromSessions(sessions, LocalDate.now())

    val trainingDays: Set<LocalDate>
        get() = sessions.map { StreakCalculator.toLocalDate(it.endDateMillis) }.toSet()

    val totalMakes: Int get() = sessions.sumOf { it.makes }
    val totalAttempts: Int get() = sessions.sumOf { it.attempts }
    val totalMisses: Int get() = sessions.sumOf { it.misses }
    val totalSwishes: Int get() = sessions.sumOf { it.swishes }
    val shootingPercentage: Int
        get() = if (totalAttempts == 0) 0 else ((totalMakes.toDouble() / totalAttempts) * 100).toInt()
    val sessionsCompleted: Int get() = sessions.size

    val favoriteDrill: String
        get() = sessions.groupBy { it.drill }.maxByOrNull { it.value.size }?.key ?: "No sessions yet"

    val todayMakes: Int
        get() = sessions.filter { isToday(it.endDateMillis) }.sumOf { it.makes }

    val todayAttempts: Int
        get() = sessions.filter { isToday(it.endDateMillis) }.sumOf { it.attempts }

    val weeklySessionsCompleted: Int
        get() = sessions.count { isThisWeek(it.endDateMillis) }

    val bestSession: WorkoutSession?
        get() = sessions.maxByOrNull { it.percentage }

    val mostRecentSession: WorkoutSession?
        get() = sessions.maxByOrNull { it.endDateMillis }

    val currentFocus: String
        get() = when {
            shootingPercentage < targetFgGoal -> "Improve shot consistency"
            weeklySessionsCompleted < weeklySessionGoal -> "Complete more sessions this week"
            else -> "Keep building momentum"
        }

    private fun readSessions(): List<WorkoutSession> = runCatching {
        val array = JSONArray(preferences.getString(KEY_SESSIONS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    WorkoutSession(
                        id = json.getString("id"),
                        drill = json.getString("drill"),
                        makes = json.getInt("makes"),
                        misses = json.getInt("misses"),
                        swishes = json.getInt("swishes"),
                        attempts = json.getInt("attempts"),
                        startDateMillis = json.getLong("startDateMillis"),
                        endDateMillis = json.getLong("endDateMillis"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun persistSessions() {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("drill", session.drill)
                    .put("makes", session.makes)
                    .put("misses", session.misses)
                    .put("swishes", session.swishes)
                    .put("attempts", session.attempts)
                    .put("startDateMillis", session.startDateMillis)
                    .put("endDateMillis", session.endDateMillis),
            )
        }
        preferences.edit { putString(KEY_SESSIONS, array.toString()) }
    }

    private fun isToday(timeMillis: Long): Boolean {
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return now.get(Calendar.ERA) == date.get(Calendar.ERA) &&
            now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(timeMillis: Long): Boolean {
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfWeek = (startOfWeek.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        return timeMillis >= startOfWeek.timeInMillis && timeMillis < endOfWeek.timeInMillis
    }

    private companion object {
        const val KEY_SESSIONS = "squatch.sessions"
        const val KEY_DAILY_SHOT_GOAL = "squatch.dailyShotGoal"
        const val KEY_WEEKLY_SESSION_GOAL = "squatch.weeklySessionGoal"
        const val KEY_DAILY_MAKE_GOAL = "squatch.dailyMakeGoal"
        const val KEY_CELEBRATED_GOALS = "squatch.celebratedGoals"
        const val KEY_TARGET_FG_GOAL = "squatch.targetFGGoal"
    }
}
