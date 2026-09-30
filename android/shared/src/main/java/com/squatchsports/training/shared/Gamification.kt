package com.squatchsports.training.shared

import org.json.JSONArray
import org.json.JSONObject

/** Text for a celebration popup, shared so the phone and watch show the same wording. */
data class CelebrationMessage(
    val emoji: String,
    val headline: String,
    val caption: String,
    val title: String,
    val subtitle: String,
)

object CelebrationMessages {
    fun forStreak(celebration: StreakCelebration): CelebrationMessage {
        val (title, subtitle) = when {
            celebration.isMilestone -> "Milestone reached!" to "${celebration.streak} training days in a row. Squatch is proud."
            celebration.isNewBest -> "New personal best!" to "Your longest streak ever: ${celebration.streak} days."
            celebration.streak == 1 -> "Streak started!" to "Come back tomorrow to keep it going."
            else -> "Streak continued!" to "Nice work, keep the fire going."
        }
        return CelebrationMessage("🔥", "${celebration.streak}", "day streak", title, subtitle)
    }

    fun forGoals(completed: List<GoalProgress>): CelebrationMessage = CelebrationMessage(
        emoji = "🏆",
        headline = if (completed.size == 1) "Goal" else "${completed.size} Goals",
        caption = "complete",
        title = if (completed.size == 1) "You hit your ${completed.single().type.title} goal!" else "You crushed ${completed.size} goals!",
        subtitle = completed.joinToString("\n") { goal ->
            when (goal.type) {
                GoalType.TARGET_FG -> "${goal.type.title}: ${goal.current}% (target ${goal.target}%)"
                else -> "${goal.type.title}: ${goal.current} / ${goal.target} ${goal.type.unit}"
            }
        },
    )
}

/** Streak and goal snapshot the phone syncs to the watch. */
data class WatchStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val trainedToday: Boolean,
    val goals: List<GoalProgress>,
)

object GamificationPayloads {
    fun stats(stats: WatchStats): ByteArray = JSONObject()
        .put("currentStreak", stats.currentStreak)
        .put("bestStreak", stats.bestStreak)
        .put("trainedToday", stats.trainedToday)
        .put(
            "goals",
            JSONArray().apply {
                stats.goals.forEach { goal ->
                    put(
                        JSONObject()
                            .put("type", goal.type.name)
                            .put("current", goal.current)
                            .put("target", goal.target)
                            .put("eligible", goal.eligible),
                    )
                }
            },
        )
        .toString()
        .encodeToByteArray()

    fun decodeStats(data: ByteArray): WatchStats? = runCatching {
        val json = JSONObject(data.decodeToString())
        val goals = json.getJSONArray("goals")
        WatchStats(
            currentStreak = json.getInt("currentStreak"),
            bestStreak = json.getInt("bestStreak"),
            trainedToday = json.getBoolean("trainedToday"),
            goals = (0 until goals.length()).mapNotNull { index ->
                val goal = goals.getJSONObject(index)
                val type = GoalType.entries.firstOrNull { it.name == goal.getString("type") } ?: return@mapNotNull null
                GoalProgress(type, goal.getInt("current"), goal.getInt("target"), goal.getBoolean("eligible"))
            },
        )
    }.getOrNull()

    fun celebration(message: CelebrationMessage): ByteArray = JSONObject()
        .put("emoji", message.emoji)
        .put("headline", message.headline)
        .put("caption", message.caption)
        .put("title", message.title)
        .put("subtitle", message.subtitle)
        .toString()
        .encodeToByteArray()

    fun decodeCelebration(data: ByteArray): CelebrationMessage? = runCatching {
        val json = JSONObject(data.decodeToString())
        CelebrationMessage(
            emoji = json.getString("emoji"),
            headline = json.getString("headline"),
            caption = json.getString("caption"),
            title = json.getString("title"),
            subtitle = json.getString("subtitle"),
        )
    }.getOrNull()
}
