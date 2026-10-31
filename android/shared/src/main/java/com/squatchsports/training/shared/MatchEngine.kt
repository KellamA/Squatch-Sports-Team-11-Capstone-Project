package com.squatchsports.training.shared

import java.util.UUID

/** Kotlin port of the iOS event-replay rules. Colors identify sides, not people. */
enum class PlayerSide(val title: String) {
    RED("Red"), BLUE("Blue");
    val opponent: PlayerSide get() = if (this == RED) BLUE else RED
}

enum class ScoringSystem(val title: String, val insidePoints: Int, val outsidePoints: Int, val defaultTarget: Int) {
    ONES_AND_TWOS("1s and 2s", 1, 2, 11),
    TWOS_AND_THREES("2s and 3s", 2, 3, 21),
}

data class MatchSettings(
    val scoring: ScoringSystem = ScoringSystem.ONES_AND_TWOS,
    val targetScore: Int = 11,
    val winByTwo: Boolean = true,
    val makeItTakeIt: Boolean = false,
    val firstPossession: PlayerSide = PlayerSide.RED,
) {
    init { require(targetScore in TARGET_RANGE) }
    companion object { val TARGET_RANGE = 5..50 }
}

enum class MatchEventType { SCORE, FOUL, TURNOVER, STEAL, POSSESSION }

data class MatchEvent(val type: MatchEventType, val side: PlayerSide, val points: Int = 0) {
    init {
        require(if (type == MatchEventType.SCORE) points in 1..3 else points == 0)
    }

    fun summary(name: (PlayerSide) -> String = { it.title }): String = when (type) {
        MatchEventType.SCORE -> "${name(side)} +$points"
        MatchEventType.FOUL -> "${name(side)} foul"
        MatchEventType.TURNOVER -> "${name(side)} turnover"
        MatchEventType.STEAL -> "${name(side)} steal"
        MatchEventType.POSSESSION -> "Ball → ${name(side)}"
    }
}

data class LoggedMatchEvent(
    val event: MatchEvent,
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
)

data class PlayerTally(
    val points: Int = 0,
    val baskets: Int = 0,
    val fouls: Int = 0,
    val turnovers: Int = 0,
    val steals: Int = 0,
)

data class MatchState(
    val red: PlayerTally = PlayerTally(),
    val blue: PlayerTally = PlayerTally(),
    val possession: PlayerSide,
    val winner: PlayerSide? = null,
) {
    val isOver: Boolean get() = winner != null
    val leader: PlayerSide? get() = when {
        red.points > blue.points -> PlayerSide.RED
        blue.points > red.points -> PlayerSide.BLUE
        else -> null
    }
    operator fun get(side: PlayerSide): PlayerTally = if (side == PlayerSide.RED) red else blue

    fun isGamePoint(side: PlayerSide, settings: MatchSettings): Boolean {
        val projected = this[side].points + settings.scoring.outsidePoints
        return !isOver && projected >= settings.targetScore &&
            (!settings.winByTwo || projected - this[side.opponent].points >= 2)
    }
}

object MatchEngine {
    fun state(events: List<MatchEvent>, settings: MatchSettings): MatchState {
        var state = MatchState(possession = settings.firstPossession)
        for (event in events) {
            if (state.isOver) break
            if (!isValid(event, settings)) continue
            val tally = state[event.side]
            val updated = when (event.type) {
                MatchEventType.SCORE -> tally.copy(points = tally.points + event.points, baskets = tally.baskets + 1)
                MatchEventType.FOUL -> tally.copy(fouls = tally.fouls + 1)
                MatchEventType.TURNOVER -> tally.copy(turnovers = tally.turnovers + 1)
                MatchEventType.STEAL -> tally.copy(steals = tally.steals + 1)
                MatchEventType.POSSESSION -> tally
            }
            state = if (event.side == PlayerSide.RED) state.copy(red = updated) else state.copy(blue = updated)
            if (event.type == MatchEventType.STEAL) {
                val opponent = state[event.side.opponent].let { it.copy(turnovers = it.turnovers + 1) }
                state = if (event.side == PlayerSide.RED) state.copy(blue = opponent) else state.copy(red = opponent)
            }
            val possession = when (event.type) {
                MatchEventType.SCORE -> if (settings.makeItTakeIt) event.side else event.side.opponent
                MatchEventType.FOUL, MatchEventType.TURNOVER -> event.side.opponent
                MatchEventType.STEAL, MatchEventType.POSSESSION -> event.side
            }
            state = state.copy(possession = possession, winner = winner(state.red.points, state.blue.points, settings))
        }
        return state
    }

    fun isValid(event: MatchEvent, settings: MatchSettings): Boolean =
        event.type != MatchEventType.SCORE || event.points == settings.scoring.insidePoints ||
            event.points == settings.scoring.outsidePoints

    fun winner(red: Int, blue: Int, settings: MatchSettings): PlayerSide? = when {
        red >= settings.targetScore && (!settings.winByTwo || red - blue >= 2) -> PlayerSide.RED
        blue >= settings.targetScore && (!settings.winByTwo || blue - red >= 2) -> PlayerSide.BLUE
        else -> null
    }
}

data class MatchFriend(
    val name: String,
    val id: String = UUID.randomUUID().toString(),
    val addedDateMillis: Long = System.currentTimeMillis(),
)

data class OneOnOneMatch(
    val opponentId: String,
    val opponentName: String,
    val userSide: PlayerSide,
    val settings: MatchSettings,
    val id: String = UUID.randomUUID().toString(),
    val events: List<LoggedMatchEvent> = emptyList(),
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long? = null,
) {
    val state: MatchState get() = MatchEngine.state(events.map { it.event }, settings)
    val userWon: Boolean? get() = state.winner?.let { it == userSide }
    fun name(side: PlayerSide, playerName: String): String = if (side == userSide) playerName else opponentName

    fun record(event: MatchEvent, now: Long = System.currentTimeMillis()): OneOnOneMatch {
        if (state.isOver || !MatchEngine.isValid(event, settings)) return this
        val next = copy(events = events + LoggedMatchEvent(event, dateMillis = now))
        return if (next.state.isOver) next.copy(endDateMillis = now) else next
    }

    fun undo(): OneOnOneMatch = if (events.isEmpty()) this else copy(events = events.dropLast(1), endDateMillis = null)
}

data class HeadToHeadRecord(val wins: Int = 0, val losses: Int = 0) {
    val games: Int get() = wins + losses
    val winPercentage: Int get() = if (games == 0) 0 else wins * 100 / games
    val text: String get() = "$wins-$losses"

    companion object {
        fun forMatches(matches: List<OneOnOneMatch>) = HeadToHeadRecord(
            wins = matches.count { it.userWon == true },
            losses = matches.count { it.userWon == false },
        )
    }
}
