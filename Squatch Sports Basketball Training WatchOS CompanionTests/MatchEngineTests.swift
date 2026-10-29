//
//  MatchEngineTests.swift
//  Squatch Sports Basketball Training WatchOS CompanionTests
//
//  1v1 rules: scoring, possession, fouls, turnovers, winners, and undo.
//

import Foundation
import Testing
@testable import Squatch_Sports_Basketball_Training_WatchOS_Companion

private let friend = Friend(name: "Kaleb")

private func state(_ events: [MatchEvent], _ settings: MatchSettings = MatchSettings()) -> MatchState {
    MatchEngine.state(for: events, settings: settings)
}

struct MatchEngineTests {
    @Test func newMatchStartsEmptyWithFirstPossession() {
        let s = state([], MatchSettings(firstPossession: .blue))
        #expect(s.red == PlayerTally())
        #expect(s.blue == PlayerTally())
        #expect(s.possession == .blue)
        #expect(s.winner == nil)
    }

    @Test func scoringAddsPointsAndGivesTheBallAway() {
        let s = state([.score(.red, points: 2), .score(.blue, points: 1)])
        #expect(s.red.points == 2)
        #expect(s.red.baskets == 1)
        #expect(s.blue.points == 1)
        #expect(s.possession == .red)
    }

    @Test func makeItTakeItKeepsTheBall() {
        let s = state([.score(.red, points: 1)], MatchSettings(makeItTakeIt: true))
        #expect(s.possession == .red)
    }

    @Test func foulGivesTheBallToTheOtherPlayer() {
        let s = state([.foul(.red)])
        #expect(s.red.fouls == 1)
        #expect(s.possession == .blue)
    }

    @Test func turnoverChangesPossession() {
        // Red loses the ball while dribbling.
        let s = state([.turnover(.red)])
        #expect(s.red.turnovers == 1)
        #expect(s.possession == .blue)
    }

    @Test func stealCountsForBothPlayers() {
        let s = state([.steal(.blue)])
        #expect(s.blue.steals == 1)
        #expect(s.red.turnovers == 1)
        #expect(s.possession == .blue)
    }

    @Test func possessionChangeMovesTheBallWithoutAStat() {
        // Blue misses and Red gets the ball back.
        let s = state([.possession(.red)], MatchSettings(firstPossession: .blue))
        #expect(s.possession == .red)
        #expect(s.red == PlayerTally())
        #expect(s.blue == PlayerTally())
    }

    @Test func reachingTheTargetWins() {
        let settings = MatchSettings(targetScore: 5, winByTwo: false)
        let s = state(Array(repeating: .score(.blue, points: 1), count: 5), settings)
        #expect(s.winner == .blue)
    }

    @Test func winByTwoKeepsTheGameGoing() {
        let settings = MatchSettings(targetScore: 3, winByTwo: true)
        var events: [MatchEvent] = [.score(.red, points: 2), .score(.blue, points: 2), .score(.red, points: 1)]
        #expect(state(events, settings).winner == nil)  // 3-2: not ahead by 2 yet

        events.append(.score(.red, points: 1))
        #expect(state(events, settings).winner == .red)  // 4-2
    }

    @Test func eventsAfterTheWinAreIgnored() {
        let settings = MatchSettings(targetScore: 2, winByTwo: false)
        let s = state([.score(.red, points: 2), .score(.blue, points: 2)], settings)
        #expect(s.winner == .red)
        #expect(s.blue.points == 0)
    }

    @Test(arguments: [ScoringSystem.onesAndTwos, .twosAndThrees])
    func scoringSystemsHaveDifferentValues(system: ScoringSystem) {
        #expect(system.outsidePoints == system.insidePoints + 1)
        #expect(system.defaultTargetScore == (system == .onesAndTwos ? 11 : 21))
    }

    @Test func gamePointWhenOneBasketCanWin() {
        let settings = MatchSettings(targetScore: 11, winByTwo: true)
        let s = state(Array(repeating: .score(.red, points: 1), count: 9), settings)
        #expect(s.isGamePoint(for: .red, settings: settings))
        #expect(!s.isGamePoint(for: .blue, settings: settings))
    }
}

struct OneOnOneMatchTests {
    @Test func recordAndUndo() {
        var match = OneOnOneMatch(opponent: friend, userSide: .red, settings: MatchSettings())
        match.record(.score(.red, points: 2))
        match.record(.foul(.blue))
        #expect(match.state.red.points == 2)
        #expect(match.state.blue.fouls == 1)

        match.undo()
        #expect(match.state.blue.fouls == 0)
        #expect(match.events.count == 1)
    }

    @Test func finishingSetsEndDateAndUndoReopens() {
        var match = OneOnOneMatch(opponent: friend, userSide: .blue, settings: MatchSettings(targetScore: 2, winByTwo: false))
        match.record(.score(.blue, points: 2))
        #expect(match.state.winner == .blue)
        #expect(match.userWon == true)
        #expect(match.endDate != nil)
        let recordedAfterWin = match.record(.score(.red, points: 1))
        #expect(!recordedAfterWin)

        match.undo()
        #expect(match.state.winner == nil)
        #expect(match.endDate == nil)
    }

    @Test func namesFollowTheChosenSide() {
        let match = OneOnOneMatch(opponent: friend, userSide: .blue, settings: MatchSettings())
        #expect(match.name(for: .blue, playerName: "Melvin") == "Melvin")
        #expect(match.name(for: .red, playerName: "Melvin") == "Kaleb")
    }

    @Test func matchRoundTripsThroughJSON() throws {
        var match = OneOnOneMatch(opponent: friend, userSide: .red, settings: MatchSettings(scoring: .twosAndThrees, targetScore: 21))
        match.record(.score(.red, points: 3))
        match.record(.steal(.blue))
        let decoded = try JSONDecoder().decode(OneOnOneMatch.self, from: JSONEncoder().encode(match))
        #expect(decoded == match)
        #expect(decoded.state == match.state)
    }
}
