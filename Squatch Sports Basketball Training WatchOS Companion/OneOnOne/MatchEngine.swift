// MatchEngine.swift
// 1v1 match rules. A match is a log of events (score, foul, turnover, ...); the scoreboard,
// possession, and winner are always computed from that log, so undo is just "drop the last event".
// Pure Swift with no UI, so it is easy to unit test and to port to Android later.

import Foundation

// PlayerSide, ScoringSystem, and MatchEvent live in SharedWorkout.swift so the watch can use them too.

/// An event with when it happened, for the play-by-play list.
nonisolated struct LoggedEvent: Codable, Equatable, Identifiable {
    var id = UUID()
    let event: MatchEvent
    var date = Date()
}

/// Running totals for one player.
nonisolated struct PlayerTally: Equatable {
    var points = 0
    var baskets = 0
    var fouls = 0
    var turnovers = 0
    var steals = 0
}

/// The scoreboard at a point in the match.
nonisolated struct MatchState: Equatable {
    var red = PlayerTally()
    var blue = PlayerTally()
    var possession: PlayerSide
    var winner: PlayerSide?

    var isOver: Bool { winner != nil }

    subscript(side: PlayerSide) -> PlayerTally {
        get { side == .red ? red : blue }
        set {
            if side == .red { red = newValue } else { blue = newValue }
        }
    }

    /// The player currently ahead, or nil when tied.
    var leader: PlayerSide? {
        if red.points == blue.points { return nil }
        return red.points > blue.points ? .red : .blue
    }

    /// True when the next basket by `side` could win the game (shown as "Game point").
    func isGamePoint(for side: PlayerSide, settings: MatchSettings) -> Bool {
        guard !isOver else { return false }
        let needed = max(settings.targetScore - self[side].points, 0)
        guard needed <= settings.scoring.outsidePoints else { return false }
        let projected = self[side].points + settings.scoring.outsidePoints
        return !settings.winByTwo || projected - self[side.opponent].points >= 2
    }
}

nonisolated enum MatchEngine {
    static func state(for events: [MatchEvent], settings: MatchSettings) -> MatchState {
        var state = MatchState(possession: settings.firstPossession)
        for event in events {
            guard !state.isOver else { break }
            apply(event, to: &state, settings: settings)
        }
        return state
    }

    private static func apply(_ event: MatchEvent, to state: inout MatchState, settings: MatchSettings) {
        switch event {
        case .score(let side, let points):
            state[side].points += points
            state[side].baskets += 1
            state.possession = settings.makeItTakeIt ? side : side.opponent
            state.winner = winner(red: state.red.points, blue: state.blue.points, settings: settings)
        case .foul(let side):
            state[side].fouls += 1
            state.possession = side.opponent
        case .turnover(let side):
            state[side].turnovers += 1
            state.possession = side.opponent
        case .steal(let side):
            state[side].steals += 1
            state[side.opponent].turnovers += 1
            state.possession = side
        case .possession(let side):
            state.possession = side
        }
    }

    /// The winner for a score, or nil if the game is still going.
    static func winner(red: Int, blue: Int, settings: MatchSettings) -> PlayerSide? {
        for (side, mine, theirs) in [(PlayerSide.red, red, blue), (.blue, blue, red)] {
            if mine >= settings.targetScore && (!settings.winByTwo || mine - theirs >= 2) {
                return side
            }
        }
        return nil
    }
}

/// A friend you play against. Stored on this phone for now; see Sprints/Sprint_5/1v1_Plan.md for online accounts.
nonisolated struct Friend: Codable, Equatable, Hashable, Identifiable {
    var id = UUID()
    var name: String
    var addedDate = Date()
}

/// One game between you and a friend.
nonisolated struct OneOnOneMatch: Codable, Equatable, Identifiable {
    var id = UUID()
    let opponentID: UUID
    /// Copied when the match starts so old matches keep the name even if the friend is renamed or removed.
    let opponentName: String
    /// Which color you are playing as.
    let userSide: PlayerSide
    let settings: MatchSettings
    private(set) var events: [LoggedEvent] = []
    var startDate = Date()
    var endDate: Date?

    init(id: UUID = UUID(), opponent: Friend, userSide: PlayerSide, settings: MatchSettings) {
        self.id = id
        self.opponentID = opponent.id
        self.opponentName = opponent.name
        self.userSide = userSide
        self.settings = settings
    }

    var state: MatchState {
        MatchEngine.state(for: events.map(\.event), settings: settings)
    }

    var userWon: Bool? {
        state.winner.map { $0 == userSide }
    }

    /// Adds an event. Ignored once the game has a winner. Returns true if it was recorded.
    @discardableResult
    mutating func record(_ event: MatchEvent) -> Bool {
        guard !state.isOver else { return false }
        events.append(LoggedEvent(event: event))
        if state.isOver {
            endDate = Date()
        }
        return true
    }

    /// Removes the last event, reopening the game if it had ended.
    mutating func undo() {
        guard !events.isEmpty else { return }
        events.removeLast()
        endDate = nil
    }

    func name(for side: PlayerSide, playerName: String) -> String {
        side == userSide ? playerName : opponentName
    }
}
