import XCTest
//
//  OneOnOneStoreTests.swift
//  Squatch Sports Basketball Training WatchOS CompanionTests
//
//  Friends, saving matches, and win/loss records.
//

import Foundation
import Testing
@testable import Squatch_Sports_Basketball_Training_WatchOS_Companion

@MainActor
struct OneOnOneStoreTests {
    private let suiteName = "OneOnOneStoreTests.\(UUID().uuidString)"

    private func makeStore() -> (OneOnOneStore, UserDefaults) {
        let defaults = UserDefaults(suiteName: suiteName)!
        return (OneOnOneStore(defaults: defaults), defaults)
    }

    /// Plays a quick game to 2 that the user wins or loses.
    private func play(_ store: OneOnOneStore, against friend: Friend, userWins: Bool) {
        store.startMatch(against: friend, userSide: .red, settings: MatchSettings(targetScore: 2, winByTwo: false))
        store.record(.score(userWins ? .red : .blue, points: 2))
        store.finishMatch()
    }

    @Test func addFriendTrimsAndRejectsDuplicates() {
        let (store, _) = makeStore()
        #expect(store.addFriend(named: "  Kaleb ")?.name == "Kaleb")
        #expect(store.addFriend(named: "kaleb") == nil)
        #expect(store.addFriend(named: "   ") == nil)
        #expect(store.friends.count == 1)
    }

    @Test func recordsCountWinsAndLossesPerFriend() throws {
        let (store, _) = makeStore()
        let kaleb = try #require(store.addFriend(named: "Kaleb"))
        let kellam = try #require(store.addFriend(named: "Kellam"))

        play(store, against: kaleb, userWins: true)
        play(store, against: kaleb, userWins: true)
        play(store, against: kaleb, userWins: false)
        play(store, against: kellam, userWins: false)

        #expect(store.record(against: kaleb) == HeadToHeadRecord(wins: 2, losses: 1))
        #expect(store.record(against: kellam) == HeadToHeadRecord(wins: 0, losses: 1))
        #expect(store.overallRecord.text == "2-2")
        #expect(store.overallRecord.winPercentage == 50)
    }

    @Test func unfinishedMatchesAreNotSaved() throws {
        let (store, _) = makeStore()
        let kaleb = try #require(store.addFriend(named: "Kaleb"))
        store.startMatch(against: kaleb, userSide: .blue, settings: MatchSettings())
        store.record(.score(.blue, points: 1))
        store.finishMatch()
        #expect(store.matches.isEmpty)
        #expect(store.activeMatch == nil)
    }

    @Test func activeMatchSurvivesRelaunch() throws {
        let (store, defaults) = makeStore()
        let kaleb = try #require(store.addFriend(named: "Kaleb"))
        store.playerName = "Melvin"
        store.startMatch(against: kaleb, userSide: .red, settings: MatchSettings())
        store.record(.score(.red, points: 2))
        store.record(.foul(.blue))

        let reopened = OneOnOneStore(defaults: defaults)
        #expect(reopened.playerName == "Melvin")
        #expect(reopened.friends == [kaleb])
        #expect(reopened.activeMatch?.state.red.points == 2)
        #expect(reopened.activeMatch?.state.blue.fouls == 1)
    }

    @Test func removingAFriendKeepsTheirHistory() throws {
        let (store, _) = makeStore()
        let kaleb = try #require(store.addFriend(named: "Kaleb"))
        play(store, against: kaleb, userWins: true)
        store.removeFriend(kaleb)
        #expect(store.friends.isEmpty)
        #expect(store.matches.first?.opponentName == "Kaleb")
    }
}

// MARK: - Nearby 1v1 Sync Tests

extension OneOnOneStoreTests {

    func testNearbyMatchUsesSharedMatchID() {
        let defaults = UserDefaults(
            suiteName: "OneOnOneStoreTests.NearbySharedID.\(UUID())"
        )!

        let store = OneOnOneStore(defaults: defaults)
        let friend = Friend(name: "Melvin")
        let sharedID = UUID()

        store.startMatch(
            id: sharedID,
            against: friend,
            userSide: .red,
            settings: MatchSettings()
        )

        XCTAssertEqual(store.activeMatch?.id, sharedID)
        XCTAssertEqual(store.activeMatch?.userSide, .red)
        XCTAssertEqual(store.activeMatch?.opponentName, "Melvin")
    }

    func testRemoteEventDoesNotEchoBack() {
        let defaults = UserDefaults(
            suiteName: "OneOnOneStoreTests.RemoteEvent.\(UUID())"
        )!

        let store = OneOnOneStore(defaults: defaults)
        let friend = Friend(name: "Melvin")
        let sharedID = UUID()

        store.startMatch(
            id: sharedID,
            against: friend,
            userSide: .red,
            settings: MatchSettings()
        )

        var echoedEventCount = 0

        store.onLocalMatchEvent = { _, _ in
            echoedEventCount += 1
        }

        store.recordRemote(
            .score(.blue, points: 1),
            matchID: sharedID
        )

        XCTAssertEqual(store.activeMatch?.state.blue.points, 1)
        XCTAssertEqual(echoedEventCount, 0)
    }

    func testRemoteUndoDoesNotEchoBack() {
        let defaults = UserDefaults(
            suiteName: "OneOnOneStoreTests.RemoteUndo.\(UUID())"
        )!

        let store = OneOnOneStore(defaults: defaults)
        let friend = Friend(name: "Melvin")
        let sharedID = UUID()

        store.startMatch(
            id: sharedID,
            against: friend,
            userSide: .red,
            settings: MatchSettings()
        )

        store.recordRemote(
            .score(.red, points: 1),
            matchID: sharedID
        )

        var echoedUndoCount = 0

        store.onLocalUndo = { _ in
            echoedUndoCount += 1
        }

        store.undoRemote(matchID: sharedID)

        XCTAssertEqual(store.activeMatch?.state.red.points, 0)
        XCTAssertEqual(echoedUndoCount, 0)
    }

    func testRemoteCancelClearsMatchWithoutEcho() {
        let defaults = UserDefaults(
            suiteName: "OneOnOneStoreTests.RemoteCancel.\(UUID())"
        )!

        let store = OneOnOneStore(defaults: defaults)
        let friend = Friend(name: "Melvin")
        let sharedID = UUID()

        store.startMatch(
            id: sharedID,
            against: friend,
            userSide: .red,
            settings: MatchSettings()
        )

        var echoedCancelCount = 0

        store.onLocalCancel = { _ in
            echoedCancelCount += 1
        }

        store.cancelRemote(matchID: sharedID)

        XCTAssertNil(store.activeMatch)
        XCTAssertEqual(echoedCancelCount, 0)
    }
}
