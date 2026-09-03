package com.squatchsports.training.model

data class DrillOption(
    val name: String,
    val subtitle: String,
    val icon: DrillIcon,
    val instructions: List<String>,
    val targetText: String,
    val isAvailable: Boolean = true,
    val courtSpots: List<String> = emptyList(),
    val shotsPerSpot: Int = 0,
)

enum class DrillIcon {
    TARGET,
    FREE_THROW,
    LOCATION,
    BOLT,
    SHUFFLE,
    GRID,
    BASKETBALL,
    RUN,
}

val drills = listOf(
    DrillOption(
        name = "Form Shooting",
        subtitle = "Close range mechanics",
        icon = DrillIcon.TARGET,
        instructions = listOf(
            "Start close to the basket.",
            "Focus on arc, touch, and follow-through.",
            "Try to make 20 before moving back.",
        ),
        targetText = "Target: 20 makes close range",
    ),
    DrillOption(
        name = "Free Throws",
        subtitle = "Routine + consistency",
        icon = DrillIcon.FREE_THROW,
        instructions = listOf(
            "Use the same routine every rep.",
            "Track makes out of 25 attempts.",
            "Focus on balance and finish.",
        ),
        targetText = "Target: 20/25 free throws",
    ),
    DrillOption(
        name = "Spot Shooting",
        subtitle = "5 spots around the arc",
        icon = DrillIcon.LOCATION,
        instructions = listOf(
            "Shoot from 5 court spots.",
            "Move to the next spot after finishing the current one.",
            "Track makes and misses for each location.",
        ),
        targetText = "Target: 5 spots, 10 shots each",
        courtSpots = listOf("Left Corner", "Left Wing", "Top of Key", "Right Wing", "Right Corner"),
        shotsPerSpot = 10,
    ),
    DrillOption(
        name = "Catch & Shoot",
        subtitle = "Quick feet + release",
        icon = DrillIcon.BOLT,
        instructions = listOf(
            "Start ready to catch and shoot.",
            "Focus on quick footwork and balance.",
            "Track makes out of 25 or 50 shots.",
        ),
        targetText = "Target: 25 makes catch-and-shoot",
        shotsPerSpot = 5,
    ),
    DrillOption(
        name = "Off the Dribble",
        subtitle = "Create space shots",
        icon = DrillIcon.SHUFFLE,
        instructions = listOf(
            "Use one or two dribbles before each shot.",
            "Work on balance after creating space.",
            "Track makes from both directions.",
        ),
        targetText = "Target: 20 makes off the dribble",
        shotsPerSpot = 5,
    ),
    DrillOption(
        name = "Midrange Series",
        subtitle = "Elbows + short corners",
        icon = DrillIcon.GRID,
        instructions = listOf(
            "Work from both elbows and both short corners.",
            "Focus on rhythm and consistency.",
            "Track total makes across all spots.",
        ),
        targetText = "Target: 5 makes from each midrange spot",
        courtSpots = listOf("Left Elbow", "Right Elbow", "Left Short Corner", "Right Short Corner"),
        shotsPerSpot = 5,
    ),
    DrillOption(
        name = "3PT Series",
        subtitle = "Volume + rhythm",
        icon = DrillIcon.BASKETBALL,
        instructions = listOf(
            "Shoot game-speed threes from around the arc.",
            "Focus on rhythm, balance, and follow-through.",
            "Track makes over a full shooting set.",
        ),
        targetText = "Target: 25 made threes",
        courtSpots = listOf("Left Corner", "Left Wing", "Top of Key", "Right Wing", "Right Corner"),
        shotsPerSpot = 5,
    ),
    DrillOption(
        name = "Finishing",
        subtitle = "Rim touch & angles",
        icon = DrillIcon.RUN,
        instructions = listOf(
            "Finish at the rim from different angles.",
            "Work both left and right hand finishes.",
            "Track makes and focus on touch.",
        ),
        targetText = "Target: 20 made finishes",
        shotsPerSpot = 5,
    ),
)
