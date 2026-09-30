package com.squatchsports.training.data

import com.squatchsports.training.shared.GoalProgress
import com.squatchsports.training.shared.StreakCelebration

/** A popup shown after a saved workout. */
sealed interface Celebration {
    data class Streak(val streak: StreakCelebration) : Celebration
    data class Goals(val completed: List<GoalProgress>) : Celebration
}
