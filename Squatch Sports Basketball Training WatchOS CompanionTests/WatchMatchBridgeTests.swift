//
//  WatchMatchBridgeTests.swift
//  Squatch Sports Basketball Training WatchOS CompanionTests
//
//  The iPhone side of 1v1 watch scorekeeping: what the watch is sent, and how plays from the watch apply.
//

import Foundation
import Testing
@testable import Squatch_Sports_Basketball_Training_WatchOS_Companion

@MainActor
struct WatchMatchBridgeTests {
    private func makeStore() -> (OneOnOneStore, Friend) {
        let store = OneOnOneStore(defaults: UserDefaults(suiteName: "WatchMatchBridgeTests.\(UUID().uuidString)")!)
        store.playerName = "Melvin"
        let kaleb = store.addFriend(named: "Kaleb")!
        return (store, kaleb)
    }

    @Test func noSnapshotWithoutAMatch() {
        let (store, _) = makeStore()
        #expect(store.watchSnapshot == nil)
    }

    @Test func snapshotShowsTheScoreboard() throws {
        let (store, kaleb) = makeStore()
        store.startMatch(against: kaleb, userSide: .blue, settings: MatchSettings(scoring: .twosAndThrees, targetScore: 21))
        store.record(.score(.blue, points: 3))
        store.record(.foul(.red))

        let snapshot = try #require(store.watchSnapshot)
        #expect(snapshot.userSide == .blue)
        #expect(snapshot.name(for: .blue) == "Melvin")
        #expect(snapshot.name(for: .red) == "Kaleb")
        #expect(snapshot.points(for: .blue) == 3)
        #expect(snapshot.fouls(for: .red) == 1)
        #expect(snapshot.scoring.outsidePoints == 3)
        #expect(snapshot.targetScore == 21)
        #expect(snapshot.possession == .blue)  // Kaleb fouled, so Melvin has the ball
        #expect(snapshot.lastPlay == "Kaleb foul")
        #expect(snapshot.canUndo)
        #expect(snapshot.winner == nil)
    }

    @Test func playsFromTheWatchAreRecorded() throws {
        let (store, kaleb) = makeStore()
        store.startMatch(against: kaleb, userSide: .red, settings: MatchSettings())
        let matchID = try #require(store.activeMatch?.id)

        store.handle(WatchMatchCommand(matchID: matchID, action: .record(.score(.red, points: 2))))
        store.handle(WatchMatchCommand(matchID: matchID, action: .record(.possession(.red))))
        store.handle(WatchMatchCommand(matchID: matchID, action: .record(.turnover(.red))))
        #expect(store.activeMatch?.state.red.points == 2)
        #expect(store.activeMatch?.state.red.turnovers == 1)

        store.handle(WatchMatchCommand(matchID: matchID, action: .undo))
        #expect(store.activeMatch?.state.red.turnovers == 0)
    }

    @Test func commandsForAnOldMatchAreIgnored() throws {
        let (store, kaleb) = makeStore()
        store.startMatch(against: kaleb, userSide: .red, settings: MatchSettings())
        store.handle(WatchMatchCommand(matchID: UUID(), action: .record(.score(.red, points: 2))))
        #expect(store.activeMatch?.state.red.points == 0)
    }

    @Test func finishFromTheWatchSavesTheGame() throws {
        let (store, kaleb) = makeStore()
        store.startMatch(against: kaleb, userSide: .red, settings: MatchSettings(targetScore: 2, winByTwo: false))
        let matchID = try #require(store.activeMatch?.id)

        store.handle(WatchMatchCommand(matchID: matchID, action: .record(.score(.red, points: 2))))
        #expect(store.watchSnapshot?.winner == .red)

        store.handle(WatchMatchCommand(matchID: matchID, action: .finish))
        #expect(store.activeMatch == nil)
        #expect(store.record(against: kaleb).wins == 1)
    }

    @Test func everyChangeNotifiesTheWatchSync() throws {
        let (store, kaleb) = makeStore()
        var notifications = 0
        store.onActiveMatchChange = { notifications += 1 }

        store.startMatch(against: kaleb, userSide: .red, settings: MatchSettings())
        store.record(.score(.red, points: 1))
        store.undoLastEvent()
        store.cancelMatch()
        #expect(notifications == 4)
    }

    @Test func payloadsRoundTripThroughJSON() throws {
        let (store, kaleb) = makeStore()
        store.startMatch(against: kaleb, userSide: .red, settings: MatchSettings())
        store.record(.steal(.blue))
        let snapshot = try #require(store.watchSnapshot)
        #expect(try JSONDecoder().decode(WatchMatchSnapshot.self, from: JSONEncoder().encode(snapshot)) == snapshot)

        let command = WatchMatchCommand(matchID: snapshot.matchID, action: .record(.foul(.red)))
        #expect(try JSONDecoder().decode(WatchMatchCommand.self, from: JSONEncoder().encode(command)) == command)
    }
}
