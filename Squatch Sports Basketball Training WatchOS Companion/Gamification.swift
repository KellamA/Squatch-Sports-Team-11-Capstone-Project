// Gamification.swift
// Streak, goal, and XP rules for the iPhone app.
// Mirrors android/shared/.../Streaks.kt, Goals.kt, Xp.kt, and Gamification.kt so both platforms behave the same.

import Foundation

// MARK: - Streaks

/// Streak rules:
/// - A training day is any calendar day with at least one saved session.
/// - One missed day between training days is a rest day: the streak survives but does not grow.
/// - Two missed days in a row end the streak.
nonisolated struct StreakResult: Equatable {
    let current: Int
    let best: Int
    /// Start of the most recent training day, or nil with no history.
    let lastTrainingDay: Date?
}

/// Feedback shown when a saved workout grows the current streak.
nonisolated struct StreakCelebration: Equatable {
    let streak: Int
    let isMilestone: Bool
    let isNewBest: Bool
}

nonisolated enum StreakCalculator {
    static let maxRestDays = 1
    static let milestones = [3, 7, 14, 30, 50, 100, 365]

    /// Largest day gap between two training days that keeps a streak alive (1 = consecutive).
    private static let maxGap = maxRestDays + 1

    static func calculate(trainingDays: [Date], today: Date, calendar: Calendar = .current) -> StreakResult {
        let todayStart = calendar.startOfDay(for: today)
        let days = Set(trainingDays.map { calendar.startOfDay(for: $0) })
            .filter { $0 <= todayStart }
            .sorted()
        guard let last = days.last else {
            return StreakResult(current: 0, best: 0, lastTrainingDay: nil)
        }

        var run = 0
        var best = 0
        var previous: Date?
        for day in days {
            if let previous, daysBetween(previous, day, calendar: calendar) <= maxGap {
                run += 1
            } else {
                run = 1
            }
            best = max(best, run)
            previous = day
        }

        let alive = daysBetween(last, todayStart, calendar: calendar) <= maxGap
        return StreakResult(current: alive ? run : 0, best: best, lastTrainingDay: last)
    }

    static func calculate(sessions: [WorkoutSession], today: Date, calendar: Calendar = .current) -> StreakResult {
        calculate(trainingDays: sessions.map(\.endDate), today: today, calendar: calendar)
    }

    /// Returns feedback when `after` grew the streak compared to `before`, or nil otherwise.
    static func celebration(before: StreakResult, after: StreakResult) -> StreakCelebration? {
        guard after.current > before.current else { return nil }
        return StreakCelebration(
            streak: after.current,
            isMilestone: milestones.contains(after.current),
            isNewBest: after.current > before.best && after.current > 1
        )
    }

    static func nextMilestone(after current: Int) -> Int? {
        milestones.first { $0 > current }
    }

    /// Whole calendar days from `start` to `end` (both expected to be start-of-day dates).
    static func daysBetween(_ start: Date, _ end: Date, calendar: Calendar = .current) -> Int {
        calendar.dateComponents([.day], from: start, to: end).day ?? 0
    }
}

// MARK: - Goals

nonisolated struct GoalTargets: Equatable {
    var dailyShots = 200
    var dailyMakes = 100
    var weeklySessions = 5
    var targetFGPercent = 75

    func target(for type: GoalType) -> Int {
        switch type {
        case .dailyShots: dailyShots
        case .dailyMakes: dailyMakes
        case .weeklySessions: weeklySessions
        case .targetFG: targetFGPercent
        }
    }

    func with(_ type: GoalType, _ value: Int) -> GoalTargets {
        var copy = self
        switch type {
        case .dailyShots: copy.dailyShots = value
        case .dailyMakes: copy.dailyMakes = value
        case .weeklySessions: copy.weeklySessions = value
        case .targetFG: copy.targetFGPercent = value
        }
        return copy
    }
}

nonisolated enum GoalTracker {
    static let minFGAttempts = 10

    /// Progress for every goal, in `GoalType.allCases` order. The week follows `calendar.firstWeekday`.
    static func progress(
        sessions: [WorkoutSession],
        targets: GoalTargets,
        today: Date,
        calendar: Calendar = .current
    ) -> [GoalProgress] {
        let todays = sessions.filter { calendar.isDate($0.endDate, inSameDayAs: today) }
        let thisWeek = calendar.dateInterval(of: .weekOfYear, for: today).map { week in
            sessions.filter { week.contains($0.endDate) }.count
        } ?? 0
        let todayAttempts = todays.reduce(0) { $0 + $1.attempts }
        let todayMakes = todays.reduce(0) { $0 + $1.makes }
        let todayFG = todayAttempts == 0 ? 0 : todayMakes * 100 / todayAttempts

        return [
            GoalProgress(type: .dailyShots, current: todayAttempts, target: targets.dailyShots),
            GoalProgress(type: .dailyMakes, current: todayMakes, target: targets.dailyMakes),
            GoalProgress(type: .weeklySessions, current: thisWeek, target: targets.weeklySessions),
            GoalProgress(
                type: .targetFG,
                current: todayFG,
                target: targets.targetFGPercent,
                eligible: todayAttempts >= minFGAttempts
            )
        ]
    }

    /// Identifies the day or week a goal belongs to, so each goal is celebrated once per period.
    /// Format matches Android, e.g. "DAILY_SHOTS@2026-09-30".
    static func periodKey(for type: GoalType, today: Date, calendar: Calendar = .current) -> String {
        let periodStart: Date
        switch type.period {
        case .day:
            periodStart = calendar.startOfDay(for: today)
        case .week:
            periodStart = calendar.dateInterval(of: .weekOfYear, for: today)?.start ?? calendar.startOfDay(for: today)
        }
        let parts = calendar.dateComponents([.year, .month, .day], from: periodStart)
        let day = String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
        return "\(type.rawValue)@\(day)"
    }

    /// Goals that are complete and have not been celebrated yet in their current period.
    static func newlyCompleted(
        _ progress: [GoalProgress],
        today: Date,
        alreadyCelebrated: Set<String>,
        calendar: Calendar = .current
    ) -> [GoalProgress] {
        progress.filter {
            $0.isComplete && !alreadyCelebrated.contains(periodKey(for: $0.type, today: today, calendar: calendar))
        }
    }
}

// MARK: - XP and levels

nonisolated enum XPCalculator {
    static let xpPerWorkout = 25
    static let xpPerMake = 1
    static let bonusXPPerSwish = 2
    static let accuracyBonusXP = 15
    static let accuracyBonusPercent = 75
    static let xpPerLevel = 100

    /// XP for one workout. `session.makes` already includes swishes, so swishes earn 1 + 2 XP.
    static func xp(for session: WorkoutSession) -> Int {
        var xp = xpPerWorkout
        xp += session.makes * xpPerMake
        xp += session.swishes * bonusXPPerSwish
        if session.percentage >= accuracyBonusPercent {
            xp += accuracyBonusXP
        }
        return xp
    }

    static func totalXP(for sessions: [WorkoutSession]) -> Int {
        sessions.reduce(0) { $0 + xp(for: $1) }
    }

    static func level(forTotalXP totalXP: Int) -> Int {
        totalXP / xpPerLevel + 1
    }
}

// MARK: - Celebration wording

nonisolated enum CelebrationMessages {
    static func forStreak(_ celebration: StreakCelebration) -> CelebrationMessage {
        let title: String
        let subtitle: String
        if celebration.isMilestone {
            title = "Milestone reached!"
            subtitle = "\(celebration.streak) training days in a row. Squatch is proud."
        } else if celebration.isNewBest {
            title = "New personal best!"
            subtitle = "Your longest streak ever: \(celebration.streak) days."
        } else if celebration.streak == 1 {
            title = "Streak started!"
            subtitle = "Come back tomorrow to keep it going."
        } else {
            title = "Streak continued!"
            subtitle = "Nice work, keep the fire going."
        }
        return CelebrationMessage(emoji: "🔥", headline: "\(celebration.streak)", caption: "day streak", title: title, subtitle: subtitle)
    }

    static func forGoals(_ completed: [GoalProgress]) -> CelebrationMessage {
        let lines = completed.map { goal in
            switch goal.type {
            case .targetFG:
                "\(goal.type.title): \(goal.current)% (target \(goal.target)%)"
            default:
                "\(goal.type.title): \(goal.current) / \(goal.target) \(goal.type.unit)"
            }
        }
        let single = completed.count == 1 ? completed.first : nil
        return CelebrationMessage(
            emoji: "🏆",
            headline: single != nil ? "Goal" : "\(completed.count) Goals",
            caption: "complete",
            title: single.map { "You hit your \($0.type.title) goal!" } ?? "You crushed \(completed.count) goals!",
            subtitle: lines.joined(separator: "\n")
        )
    }

    static func forWorkout(earnedXP: Int, newLevel: Int, leveledUp: Bool, xpUntilNextLevel: Int) -> CelebrationMessage {
        if leveledUp {
            return CelebrationMessage(
                emoji: "⭐️",
                headline: "Level \(newLevel)",
                caption: "level up",
                title: "Level Up!",
                subtitle: "You earned \(earnedXP) XP and reached Level \(newLevel)!"
            )
        }
        return CelebrationMessage(
            emoji: "🎉",
            headline: "+\(earnedXP)",
            caption: "XP earned",
            title: "Workout Complete!",
            subtitle: "Only \(xpUntilNextLevel) XP until Level \(newLevel + 1)."
        )
    }
}
