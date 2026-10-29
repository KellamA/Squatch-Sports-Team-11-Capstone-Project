import SwiftUI
import Foundation
import Combine

nonisolated struct WorkoutSession: Identifiable, Codable {
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
        static let dailyMakeGoal = "squatch.dailyMakeGoal"
        static let weeklySessionGoal = "squatch.weeklySessionGoal"
        static let targetFGGoal = "squatch.targetFGGoal"
        static let celebratedGoals = "squatch.celebratedGoals"
    }

    @Published var sessions: [WorkoutSession] {
        didSet { persistSessions() }
    }

    @Published var dailyShotGoal: Int {
        didSet { defaults.set(dailyShotGoal, forKey: Keys.dailyShotGoal) }
    }

    @Published var dailyMakeGoal: Int {
        didSet { defaults.set(dailyMakeGoal, forKey: Keys.dailyMakeGoal) }
    }

    @Published var weeklySessionGoal: Int {
        didSet { defaults.set(weeklySessionGoal, forKey: Keys.weeklySessionGoal) }
    }

    @Published var targetFGGoal: Int {
        didSet { defaults.set(targetFGGoal, forKey: Keys.targetFGGoal) }
    }

    /// Popups waiting to be shown after a workout, oldest first. The UI shows the first one.
    @Published private(set) var celebrations: [CelebrationMessage] = []

    /// Period keys (see `GoalTracker.periodKey`) of goals already celebrated.
    private var celebratedGoals: Set<String> {
        didSet { defaults.set(Array(celebratedGoals), forKey: Keys.celebratedGoals) }
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
        self.dailyMakeGoal = (defaults.object(forKey: Keys.dailyMakeGoal) as? Int) ?? 100
        self.weeklySessionGoal = (defaults.object(forKey: Keys.weeklySessionGoal) as? Int) ?? 5
        self.targetFGGoal = (defaults.object(forKey: Keys.targetFGGoal) as? Int) ?? 75
        self.celebratedGoals = Set(defaults.stringArray(forKey: Keys.celebratedGoals) ?? [])
    }

    private func persistSessions() {
        if let data = try? JSONEncoder().encode(sessions) {
            defaults.set(data, forKey: Keys.sessions)
        }
    }

    /// Saves a workout and queues its popups: XP earned (or level up), then streak, then goals.
    func addSession(_ session: WorkoutSession) {
        let streakBefore = streak
        let levelBefore = playerLevel

        sessions.insert(session, at: 0)

        celebrations.append(
            CelebrationMessages.forWorkout(
                earnedXP: XPCalculator.xp(for: session),
                newLevel: playerLevel,
                leveledUp: playerLevel > levelBefore,
                xpUntilNextLevel: xpUntilNextLevel
            )
        )
        if let streakCelebration = StreakCalculator.celebration(before: streakBefore, after: streak) {
            celebrations.append(CelebrationMessages.forStreak(streakCelebration))
        }
        celebrateCompletedGoals()
    }

    func dismissCelebration() {
        if !celebrations.isEmpty {
            celebrations.removeFirst()
        }
    }

    func clearAllSessions() {
        sessions.removeAll()
        celebratedGoals.removeAll()
    }

    /// Debug helper for testing and demos: replaces history with 6 training days over the last week,
    /// with a rest day 4 days ago. The current streak becomes 6, so finishing a workout today hits the
    /// 7-day milestone. Matches `loadDemoHistory()` on Android.
    func loadDemoHistory() {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let demo: [WorkoutSession] = [7, 6, 5, 3, 2, 1].compactMap { daysAgo in
            guard let day = calendar.date(byAdding: .day, value: -daysAgo, to: today),
                  let end = calendar.date(bySettingHour: 17, minute: 30, second: 0, of: day) else { return nil }
            return WorkoutSession(
                drill: daysAgo % 2 == 0 ? "Spot Shooting" : "Free Throws",
                makes: 15 + daysAgo,
                misses: 10 - daysAgo,
                swishes: 3,
                attempts: 25,
                startDate: end.addingTimeInterval(-30 * 60),
                endDate: end
            )
        }
        sessions = demo.sorted { $0.endDate > $1.endDate }
        celebratedGoals.removeAll()
    }

    // MARK: - Goals

    var goalTargets: GoalTargets {
        GoalTargets(
            dailyShots: dailyShotGoal,
            dailyMakes: dailyMakeGoal,
            weeklySessions: weeklySessionGoal,
            targetFGPercent: targetFGGoal
        )
    }

    func updateGoal(_ type: GoalType, to value: Int) {
        let clamped = min(max(value, type.range.lowerBound), type.range.upperBound)
        switch type {
        case .dailyShots: dailyShotGoal = clamped
        case .dailyMakes: dailyMakeGoal = clamped
        case .weeklySessions: weeklySessionGoal = clamped
        case .targetFG: targetFGGoal = clamped
        }
    }

    var goalProgress: [GoalProgress] {
        GoalTracker.progress(sessions: sessions, targets: goalTargets, today: Date())
    }

    private func celebrateCompletedGoals() {
        let today = Date()
        // Only the current day/week matters; drop records from earlier periods.
        let currentKeys = Set(GoalType.allCases.map { GoalTracker.periodKey(for: $0, today: today) })
        var celebrated = celebratedGoals.intersection(currentKeys)
        let completed = GoalTracker.newlyCompleted(goalProgress, today: today, alreadyCelebrated: celebrated)
        completed.forEach { celebrated.insert(GoalTracker.periodKey(for: $0.type, today: today)) }
        celebratedGoals = celebrated
        if !completed.isEmpty {
            celebrations.append(CelebrationMessages.forGoals(completed))
        }
    }

    // MARK: - Streaks

    var streak: StreakResult {
        StreakCalculator.calculate(sessions: sessions, today: Date())
    }

    /// Start-of-day dates with at least one workout.
    var trainingDays: Set<Date> {
        Set(sessions.map { Calendar.current.startOfDay(for: $0.endDate) })
    }

    /// Snapshot synced to the watch app.
    var watchStats: WatchStats {
        let current = streak
        return WatchStats(
            currentStreak: current.current,
            bestStreak: current.best,
            trainedToday: current.lastTrainingDay.map { Calendar.current.isDateInToday($0) } ?? false,
            goals: goalProgress,
            level: playerLevel,
            xpInCurrentLevel: xpInCurrentLevel
        )
    }

    // MARK: - Stats

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

    // MARK: - XP and levels

    var totalXP: Int {
        XPCalculator.totalXP(for: sessions)
    }

    var playerLevel: Int {
        XPCalculator.level(forTotalXP: totalXP)
    }

    var xpInCurrentLevel: Int {
        totalXP % XPCalculator.xpPerLevel
    }

    var xpNeededForNextLevel: Int {
        XPCalculator.xpPerLevel
    }

    var levelProgress: Double {
        Double(xpInCurrentLevel) / Double(xpNeededForNextLevel)
    }

    var xpUntilNextLevel: Int {
        xpNeededForNextLevel - xpInCurrentLevel
    }
}
