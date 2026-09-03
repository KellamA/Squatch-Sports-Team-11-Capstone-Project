package com.squatchsports.training.data

import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.squatchsports.training.shared.WorkoutSession
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AppDataStore(context: Context) {
    private val preferences = context.getSharedPreferences("squatch_data", Context.MODE_PRIVATE)

    val sessions = mutableStateListOf<WorkoutSession>()

    var dailyShotGoal by mutableIntStateOf(preferences.getInt(KEY_DAILY_SHOT_GOAL, 200))
        private set
    var weeklySessionGoal by mutableIntStateOf(preferences.getInt(KEY_WEEKLY_SESSION_GOAL, 5))
        private set
    var targetFgGoal by mutableIntStateOf(preferences.getInt(KEY_TARGET_FG_GOAL, 75))
        private set

    init {
        sessions.addAll(readSessions())
    }

    fun addSession(session: WorkoutSession) {
        sessions.add(0, session)
        persistSessions()
    }

    fun clearAllSessions() {
        sessions.clear()
        persistSessions()
    }

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
        const val KEY_TARGET_FG_GOAL = "squatch.targetFGGoal"
    }
}
