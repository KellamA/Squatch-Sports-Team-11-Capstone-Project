// OneOnOneStore.swift
// Friends, the match in progress, finished matches, and win/loss records for 1v1.
// Saved in UserDefaults on this phone. To add online friends later, keep this API and
// swap the storage (see Sprints/Sprint_5/1v1_Plan.md).

import Foundation
import Combine

/// Wins and losses, overall or against one friend.
nonisolated struct HeadToHeadRecord: Equatable {
    var wins = 0
    var losses = 0

    var games: Int { wins + losses }

    var winPercentage: Int {
        games == 0 ? 0 : wins * 100 / games
    }

    /// e.g. "3-1".
    var text: String { "\(wins)-\(losses)" }
}

final class OneOnOneStore: ObservableObject {
    private let defaults: UserDefaults
    private enum Keys {
        static let playerName = "squatch.oneOnOne.playerName"
        static let friends = "squatch.oneOnOne.friends"
        static let matches = "squatch.oneOnOne.matches"
        static let activeMatch = "squatch.oneOnOne.activeMatch"
    }

    /// Called after the match in progress (or your name on it) changes. The watch sync uses this
    /// to send the new scoreboard (see WatchMatchBridge.swift).
    var onActiveMatchChange: (() -> Void)?

    /// Your name on the scoreboard.
    @Published var playerName: String {
        didSet {
            defaults.set(playerName, forKey: Keys.playerName)
            onActiveMatchChange?()
        }
    }

    @Published private(set) var friends: [Friend] {
        didSet { save(friends, forKey: Keys.friends) }
    }

    /// Finished matches, newest first.
    @Published private(set) var matches: [OneOnOneMatch] {
        didSet { save(matches, forKey: Keys.matches) }
    }

    /// The match being played. Saved so it survives leaving the app mid-game.
    @Published private(set) var activeMatch: OneOnOneMatch? {
        didSet {
            save(activeMatch, forKey: Keys.activeMatch)
            onActiveMatchChange?()
        }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.playerName = defaults.string(forKey: Keys.playerName) ?? "You"
        self.friends = Self.load([Friend].self, from: defaults, key: Keys.friends) ?? []
        self.matches = Self.load([OneOnOneMatch].self, from: defaults, key: Keys.matches) ?? []
        self.activeMatch = Self.load(OneOnOneMatch.self, from: defaults, key: Keys.activeMatch)
    }

    // MARK: - Friends

    /// Adds a friend by name. Returns nil for an empty or duplicate name.
    @discardableResult
    func addFriend(named name: String) -> Friend? {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              !friends.contains(where: { $0.name.caseInsensitiveCompare(trimmed) == .orderedSame }) else { return nil }
        let friend = Friend(name: trimmed)
        friends.append(friend)
        return friend
    }

    /// Removes a friend. Their past matches stay in history.
    func removeFriend(_ friend: Friend) {
        friends.removeAll { $0.id == friend.id }
    }

    // MARK: - Matches

    func startMatch(against opponent: Friend, userSide: PlayerSide, settings: MatchSettings) {
        activeMatch = OneOnOneMatch(opponent: opponent, userSide: userSide, settings: settings)
    }

    func record(_ event: MatchEvent) {
        activeMatch?.record(event)
    }

    func undoLastEvent() {
        activeMatch?.undo()
    }

    /// Saves a finished match to history and clears it. Unfinished matches are discarded instead.
    func finishMatch() {
        guard let match = activeMatch else { return }
        if match.state.isOver {
            matches.insert(match, at: 0)
        }
        activeMatch = nil
    }

    /// Ends the current match without saving it.
    func cancelMatch() {
        activeMatch = nil
    }

    // MARK: - Records

    var overallRecord: HeadToHeadRecord {
        record(for: matches)
    }

    func record(against friend: Friend) -> HeadToHeadRecord {
        record(for: matches.filter { $0.opponentID == friend.id })
    }

    func matches(against friend: Friend) -> [OneOnOneMatch] {
        matches.filter { $0.opponentID == friend.id }
    }

    private func record(for matches: [OneOnOneMatch]) -> HeadToHeadRecord {
        matches.reduce(into: HeadToHeadRecord()) { record, match in
            switch match.userWon {
            case true?: record.wins += 1
            case false?: record.losses += 1
            case nil: break
            }
        }
    }

    // MARK: - Persistence

    private func save<T: Encodable>(_ value: T?, forKey key: String) {
        if let value, let data = try? JSONEncoder().encode(value) {
            defaults.set(data, forKey: key)
        } else {
            defaults.removeObject(forKey: key)
        }
    }

    private static func load<T: Decodable>(_ type: T.Type, from defaults: UserDefaults, key: String) -> T? {
        defaults.data(forKey: key).flatMap { try? JSONDecoder().decode(type, from: $0) }
    }
}
