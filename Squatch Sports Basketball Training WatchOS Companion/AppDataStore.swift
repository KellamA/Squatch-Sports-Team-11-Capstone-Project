import SwiftUI
import Foundation
import Combine

struct WorkoutSession: Identifiable, Codable {
    let id: UUID
    let drill: String
    let makes: Int
    let misses: Int
    let swishes: Int
    let attempts: Int
    let startDate: Date
    let endDate: Date

    init(
        id: UUID = UUID(),
        drill: String,
        makes: Int,
        misses: Int,
        swishes: Int,
        attempts: Int,
        startDate: Date,
        endDate: Date
    ) {
        self.id = id
        self.drill = drill
        self.makes = makes
        self.misses = misses
        self.swishes = swishes
        self.attempts = attempts
        self.startDate = startDate
        self.endDate = endDate
    }

    var percentage: Int {
        guard attempts > 0 else { return 0 }
        return Int((Double(makes) / Double(attempts)) * 100)
    }
}

final class AppDataStore: ObservableObject {
    private let defaults: UserDefaults
    private enum Keys {
        static let sessions = "squatch.sessions"
        static let dailyShotGoal = "squatch.dailyShotGoal"
        static let weeklySessionGoal = "squatch.weeklySessionGoal"
        static let targetFGGoal = "squatch.targetFGGoal"
    }

    @Published var sessions: [WorkoutSession] {
        didSet { persistSessions() }
    }

    @Published var dailyShotGoal: Int {
        didSet { defaults.set(dailyShotGoal, forKey: Keys.dailyShotGoal) }
    }

    @Published var weeklySessionGoal: Int {
        didSet { defaults.set(weeklySessionGoal, forKey: Keys.weeklySessionGoal) }
    }

    @Published var targetFGGoal: Int {
        didSet { defaults.set(targetFGGoal, forKey: Keys.targetFGGoal) }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        if let data = defaults.data(forKey: Keys.sessions),
           let decoded = try? JSONDecoder().decode([WorkoutSession].self, from: data) {
            self.sessions = decoded
        } else {
            self.sessions = []
        }

        self.dailyShotGoal = (defaults.object(forKey: Keys.dailyShotGoal) as? Int) ?? 200
        self.weeklySessionGoal = (defaults.object(forKey: Keys.weeklySessionGoal) as? Int) ?? 5
        self.targetFGGoal = (defaults.object(forKey: Keys.targetFGGoal) as? Int) ?? 75
    }

    private func persistSessions() {
        if let data = try? JSONEncoder().encode(sessions) {
            defaults.set(data, forKey: Keys.sessions)
        }
    }

    func addSession(_ session: WorkoutSession) {
        sessions.insert(session, at: 0)
    }

    var totalMakes: Int {
        sessions.reduce(0) { $0 + $1.makes }
    }

    var totalAttempts: Int {
        sessions.reduce(0) { $0 + $1.attempts }
    }

    var totalMisses: Int {
        sessions.reduce(0) { $0 + $1.misses }
    }

    var totalSwishes: Int {
        sessions.reduce(0) { $0 + $1.swishes }
    }

    var shootingPercentage: Int {
        guard totalAttempts > 0 else { return 0 }
        return Int((Double(totalMakes) / Double(totalAttempts)) * 100)
    }

    var sessionsCompleted: Int {
        sessions.count
    }

    var favoriteDrill: String {
        let grouped = Dictionary(grouping: sessions, by: { $0.drill })
        return grouped.max(by: { $0.value.count < $1.value.count })?.key ?? "No sessions yet"
    }

    var todayMakes: Int {
        sessions
            .filter { Calendar.current.isDateInToday($0.endDate) }
            .reduce(0) { $0 + $1.makes }
    }

    var todayAttempts: Int {
        sessions
            .filter { Calendar.current.isDateInToday($0.endDate) }
            .reduce(0) { $0 + $1.attempts }
    }

    var weeklySessionsCompleted: Int {
        sessions.filter { session in
            guard let weekInterval = Calendar.current.dateInterval(of: .weekOfYear, for: Date()) else {
                return false
            }
            return weekInterval.contains(session.endDate)
        }.count
    }

    var bestSession: WorkoutSession? {
        sessions.max { $0.percentage < $1.percentage }
    }

    var mostRecentSession: WorkoutSession? {
        sessions.sorted(by: { $0.endDate > $1.endDate }).first
    }

    var currentFocus: String {
        if shootingPercentage < targetFGGoal {
            return "Improve shot consistency"
        } else if weeklySessionsCompleted < weeklySessionGoal {
            return "Complete more sessions this week"
        } else {
            return "Keep building momentum"
        }
    }

    var todayGoalProgress: Double {
        guard dailyShotGoal > 0 else { return 0 }
        return min(Double(todayAttempts) / Double(dailyShotGoal), 1.0)
    }

    func clearAllSessions() {
        sessions.removeAll()
    }
}
