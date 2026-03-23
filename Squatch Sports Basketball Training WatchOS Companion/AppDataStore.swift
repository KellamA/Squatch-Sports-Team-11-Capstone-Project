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
    @Published var sessions: [WorkoutSession] = [
        WorkoutSession(
            drill: "Form Shooting",
            makes: 32,
            misses: 8,
            swishes: 10,
            attempts: 40,
            startDate: Calendar.current.date(byAdding: .hour, value: -2, to: Date()) ?? Date(),
            endDate: Date()
        ),
        WorkoutSession(
            drill: "Free Throws",
            makes: 18,
            misses: 7,
            swishes: 4,
            attempts: 25,
            startDate: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date(),
            endDate: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date()
        ),
        WorkoutSession(
            drill: "Spot Shooting",
            makes: 21,
            misses: 9,
            swishes: 6,
            attempts: 30,
            startDate: Calendar.current.date(byAdding: .day, value: -2, to: Date()) ?? Date(),
            endDate: Calendar.current.date(byAdding: .day, value: -2, to: Date()) ?? Date()
        )
    ]

    @Published var dailyShotGoal: Int = 200
    @Published var weeklySessionGoal: Int = 5
    @Published var targetFGGoal: Int = 75

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

    func resetSampleData() {
        sessions.removeAll()
    }
}
