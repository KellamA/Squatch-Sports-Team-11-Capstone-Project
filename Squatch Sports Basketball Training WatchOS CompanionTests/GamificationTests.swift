//
//  GamificationTests.swift
//  Squatch Sports Basketball Training WatchOS CompanionTests
//
//  Ports android/shared/src/test (StreakCalculatorTest, GoalTrackerTest, GamificationPayloadsTest)
//  so both platforms are checked against the same known dates and data.
//

import Foundation
import Testing
@testable import Squatch_Sports_Basketball_Training_WatchOS_Companion

/// Gregorian UTC calendar with weeks starting Sunday, so results don't depend on the test machine.
private let utc: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC")!
    calendar.firstWeekday = 1
    return calendar
}()

private func date(_ month: Int, _ day: Int, hour: Int = 12, minute: Int = 0) -> Date {
    utc.date(from: DateComponents(year: 2026, month: month, day: day, hour: hour, minute: minute))!
}

/// Wednesday, Sep 30, 2026. With a Sunday week start, the week is Sep 27 – Oct 3.
private let today = date(9, 30)

private func sep(_ days: Int...) -> [Date] {
    days.map { date(9, $0) }
}

private func session(on day: Date, makes: Int, attempts: Int, swishes: Int = 0) -> WorkoutSession {
    WorkoutSession(
        drill: "Spot Shooting",
        makes: makes,
        misses: attempts - makes,
        swishes: swishes,
        attempts: attempts,
        startDate: day,
        endDate: day
    )
}

// MARK: - Streaks

struct StreakCalculatorTests {
    private func streak(_ days: [Date]) -> StreakResult {
        StreakCalculator.calculate(trainingDays: days, today: today, calendar: utc)
    }

    @Test("Streak rules with known workout dates", arguments: [
        ("no sessions", [Int](), 0, 0),
        ("first workout today", [30], 1, 1),
        ("consecutive days", [28, 29, 30], 3, 3),
        ("same-day sessions count once", [29, 30, 30], 2, 2),
        ("rest day keeps streak without adding", [27, 28, 30], 3, 3),
        ("not trained today yet, still alive", [28, 29], 2, 2),
        ("rest day yesterday, still alive", [27, 28], 2, 2),
        ("two missed days break the streak", [26, 27], 0, 2),
        ("best streak remembered", [20, 21, 22, 23, 24, 28, 29, 30], 3, 5),
        ("every other day continues", [24, 26, 28, 30], 4, 4)
    ])
    func streakCases(name: String, days: [Int], current: Int, best: Int) {
        let result = streak(days.map { date(9, $0) })
        #expect(result.current == current, "current: \(name)")
        #expect(result.best == best, "best: \(name)")
    }

    @Test func futureDatesAreIgnored() {
        let result = streak(sep(29, 30) + [date(10, 2)])
        #expect(result.current == 2)
        #expect(result.best == 2)
    }

    @Test func lastTrainingDayIsReported() {
        #expect(streak(sep(27, 28)).lastTrainingDay == utc.startOfDay(for: date(9, 28)))
        #expect(streak([]).lastTrainingDay == nil)
    }

    @Test func sessionsAreGroupedByLocalDay() {
        // 23:30 and 00:30 UTC fall on two different days in UTC.
        let sessions = [date(9, 29, hour: 23, minute: 30), date(9, 30, hour: 0, minute: 30)].map {
            session(on: $0, makes: 1, attempts: 1)
        }
        #expect(StreakCalculator.calculate(sessions: sessions, today: today, calendar: utc).current == 2)
    }

    @Test func celebrationWhenStreakGrows() throws {
        let celebration = try #require(StreakCalculator.celebration(before: streak(sep(28, 29)), after: streak(sep(28, 29, 30))))
        #expect(celebration.streak == 3)
        #expect(celebration.isMilestone)
        #expect(celebration.isNewBest)
    }

    @Test func noCelebrationForSecondSessionSameDay() {
        #expect(StreakCalculator.celebration(before: streak(sep(29, 30)), after: streak(sep(29, 30, 30))) == nil)
    }

    @Test func continuedStreakBelowBestIsNotNewBest() throws {
        let before = streak(sep(20, 21, 22, 23, 24, 29))
        let after = streak(sep(20, 21, 22, 23, 24, 29, 30))
        let celebration = try #require(StreakCalculator.celebration(before: before, after: after))
        #expect(celebration.streak == 2)
        #expect(!celebration.isMilestone)
        #expect(!celebration.isNewBest)
    }

    @Test func nextMilestone() {
        #expect(StreakCalculator.nextMilestone(after: 0) == 3)
        #expect(StreakCalculator.nextMilestone(after: 3) == 7)
        #expect(StreakCalculator.nextMilestone(after: 365) == nil)
    }
}

// MARK: - Goals

struct GoalTrackerTests {
    private let targets = GoalTargets(dailyShots: 50, dailyMakes: 30, weeklySessions: 3, targetFGPercent: 60)

    private func progress(_ sessions: [WorkoutSession], on day: Date = today) -> [GoalType: GoalProgress] {
        let list = GoalTracker.progress(sessions: sessions, targets: targets, today: day, calendar: utc)
        return Dictionary(uniqueKeysWithValues: list.map { ($0.type, $0) })
    }

    @Test func emptyHistoryHasZeroProgress() {
        let p = progress([])
        for type in GoalType.allCases {
            #expect(p[type]?.current == 0)
        }
        #expect(!p.values.contains { $0.isComplete })
    }

    @Test func dailyGoalsOnlyCountToday() {
        let p = progress([session(on: date(9, 30), makes: 20, attempts: 25), session(on: date(9, 29), makes: 40, attempts: 50)])
        #expect(p[.dailyShots]?.current == 25)
        #expect(p[.dailyMakes]?.current == 20)
        #expect(p[.dailyShots]?.fraction == 0.5)
    }

    @Test func multipleSessionsTodayAddUp() {
        let p = progress([session(on: date(9, 30), makes: 20, attempts: 25), session(on: date(9, 30), makes: 15, attempts: 25)])
        #expect(p[.dailyShots]?.current == 50)
        #expect(p[.dailyShots]?.isComplete == true)
        #expect(p[.dailyMakes]?.isComplete == true)
    }

    @Test func progressIsCappedAtFull() {
        let p = progress([session(on: date(9, 30), makes: 90, attempts: 100)])
        #expect(p[.dailyShots]?.current == 100)
        #expect(p[.dailyShots]?.fraction == 1)
    }

    @Test func weeklySessionsUseWeekBoundaries() {
        // Sep 26 is the Saturday before this week; Sep 27 (Sunday) starts it.
        let p = progress([26, 27, 28, 30].map { session(on: date(9, $0), makes: 5, attempts: 10) })
        #expect(p[.weeklySessions]?.current == 3)
        #expect(p[.weeklySessions]?.isComplete == true)
    }

    @Test func fgGoalNeedsMinimumAttempts() {
        let lucky = progress([session(on: date(9, 30), makes: 5, attempts: 5)])[.targetFG]
        #expect(lucky?.current == 100)
        #expect(lucky?.eligible == false)
        #expect(lucky?.isComplete == false)

        let real = progress([session(on: date(9, 30), makes: 7, attempts: 10)])[.targetFG]
        #expect(real?.current == 70)
        #expect(real?.isComplete == true)
    }

    @Test func fgBelowTargetIsNotComplete() {
        let fg = progress([session(on: date(9, 30), makes: 11, attempts: 20)])[.targetFG]
        #expect(fg?.current == 55)
        #expect(fg?.isComplete == false)
    }

    @Test func periodKeysSeparateDaysAndWeeks() {
        #expect(GoalTracker.periodKey(for: .dailyShots, today: today, calendar: utc) == "DAILY_SHOTS@2026-09-30")
        #expect(GoalTracker.periodKey(for: .weeklySessions, today: today, calendar: utc) == "WEEKLY_SESSIONS@2026-09-27")
    }

    @Test func completedGoalsAreCelebratedOncePerPeriod() {
        let p = GoalTracker.progress(sessions: [session(on: date(9, 30), makes: 40, attempts: 50)], targets: targets, today: today, calendar: utc)
        let first = GoalTracker.newlyCompleted(p, today: today, alreadyCelebrated: [], calendar: utc).map(\.type)
        #expect(first == [.dailyShots, .dailyMakes, .targetFG])

        let celebrated = Set(first.map { GoalTracker.periodKey(for: $0, today: today, calendar: utc) })
        #expect(GoalTracker.newlyCompleted(p, today: today, alreadyCelebrated: celebrated, calendar: utc).isEmpty)

        // The next day the daily goals can be celebrated again.
        let tomorrow = date(10, 1)
        let nextDay = GoalTracker.progress(sessions: [session(on: tomorrow, makes: 40, attempts: 50)], targets: targets, today: tomorrow, calendar: utc)
        #expect(GoalTracker.newlyCompleted(nextDay, today: tomorrow, alreadyCelebrated: celebrated, calendar: utc).count == 3)
    }

    @Test func targetsCanBeUpdated() {
        let updated = targets.with(.dailyMakes, 45)
        #expect(updated.target(for: .dailyMakes) == 45)
        #expect(updated.target(for: .dailyShots) == 50)
    }
}

// MARK: - XP

struct XPCalculatorTests {
    @Test func workoutXPFollowsRules() {
        // 25 per workout + 1 per make. 10/25 = 40%, so no accuracy bonus.
        #expect(XPCalculator.xp(for: session(on: today, makes: 10, attempts: 25)) == 35)
        // Swishes are included in makes and earn 2 bonus XP each: 25 + 10 + 3 * 2.
        #expect(XPCalculator.xp(for: session(on: today, makes: 10, attempts: 25, swishes: 3)) == 41)
        // 75%+ shooting earns 15 bonus XP: 25 + 15 + 15.
        #expect(XPCalculator.xp(for: session(on: today, makes: 15, attempts: 20)) == 55)
    }

    @Test func levelsEvery100XP() {
        #expect(XPCalculator.level(forTotalXP: 0) == 1)
        #expect(XPCalculator.level(forTotalXP: 99) == 1)
        #expect(XPCalculator.level(forTotalXP: 100) == 2)
        #expect(XPCalculator.level(forTotalXP: 250) == 3)
    }
}

// MARK: - Celebration wording and watch payloads

struct CelebrationMessageTests {
    @Test func streakMessagesMatchCelebrationType() {
        #expect(CelebrationMessages.forStreak(StreakCelebration(streak: 7, isMilestone: true, isNewBest: true)).title == "Milestone reached!")
        #expect(CelebrationMessages.forStreak(StreakCelebration(streak: 8, isMilestone: false, isNewBest: true)).title == "New personal best!")
        #expect(CelebrationMessages.forStreak(StreakCelebration(streak: 1, isMilestone: false, isNewBest: false)).title == "Streak started!")
        #expect(CelebrationMessages.forStreak(StreakCelebration(streak: 2, isMilestone: false, isNewBest: false)).title == "Streak continued!")
    }

    @Test func goalMessageListsEachGoal() {
        let message = CelebrationMessages.forGoals([
            GoalProgress(type: .dailyShots, current: 50, target: 50),
            GoalProgress(type: .targetFG, current: 80, target: 75)
        ])
        #expect(message.title == "You crushed 2 goals!")
        #expect(message.subtitle == "Daily Shots: 50 / 50 shots\nToday's FG%: 80% (target 75%)")
    }

    @Test func workoutMessageShowsXPOrLevelUp() {
        let workout = CelebrationMessages.forWorkout(earnedXP: 35, newLevel: 2, leveledUp: false, xpUntilNextLevel: 40)
        #expect(workout.title == "Workout Complete!")
        #expect(workout.subtitle == "Only 40 XP until Level 3.")

        let levelUp = CelebrationMessages.forWorkout(earnedXP: 55, newLevel: 3, leveledUp: true, xpUntilNextLevel: 95)
        #expect(levelUp.title == "Level Up!")
        #expect(levelUp.subtitle == "You earned 55 XP and reached Level 3!")
    }

    @Test func statsRoundTrip() throws {
        let stats = WatchStats(
            currentStreak: 7,
            bestStreak: 9,
            trainedToday: true,
            goals: [
                GoalProgress(type: .dailyShots, current: 120, target: 200),
                GoalProgress(type: .targetFG, current: 80, target: 75, eligible: false)
            ],
            level: 4,
            xpInCurrentLevel: 35
        )
        let decoded = try JSONDecoder().decode(WatchStats.self, from: JSONEncoder().encode(stats))
        #expect(decoded == stats)
    }

    @Test func celebrationRoundTripKeepsID() throws {
        let message = CelebrationMessages.forStreak(StreakCelebration(streak: 7, isMilestone: true, isNewBest: true))
        let decoded = try JSONDecoder().decode(CelebrationMessage.self, from: JSONEncoder().encode(message))
        #expect(decoded == message)
    }

    @Test func repeatedMessagesHaveDifferentIDs() {
        let celebration = StreakCelebration(streak: 2, isMilestone: false, isNewBest: false)
        #expect(CelebrationMessages.forStreak(celebration).id != CelebrationMessages.forStreak(celebration).id)
    }
}

// MARK: - App data store

struct AppDataStoreGamificationTests {
    private func makeStore() -> AppDataStore {
        let suite = "GamificationTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        return AppDataStore(defaults: defaults)
    }

    @Test func firstWorkoutQueuesXPThenStreakPopup() {
        let store = makeStore()
        store.addSession(session(on: Date(), makes: 10, attempts: 25))
        #expect(store.celebrations.map(\.title) == ["Workout Complete!", "Streak started!"])

        store.dismissCelebration()
        #expect(store.celebrations.map(\.title) == ["Streak started!"])
    }

    @Test func levelUpReplacesWorkoutPopup() {
        let store = makeStore()
        // 25 + 60 makes + 15 accuracy bonus = 100 XP, reaching Level 2.
        store.addSession(session(on: Date(), makes: 60, attempts: 60))
        #expect(store.celebrations.first?.title == "Level Up!")
        #expect(store.playerLevel == 2)
    }

    @Test func goalsAreCelebratedOncePerDay() {
        let store = makeStore()
        store.updateGoal(.dailyShots, to: 20)
        store.addSession(session(on: Date(), makes: 5, attempts: 25))
        #expect(store.celebrations.contains { $0.emoji == "🏆" })

        while !store.celebrations.isEmpty { store.dismissCelebration() }
        store.addSession(session(on: Date(), makes: 5, attempts: 25))
        #expect(!store.celebrations.contains { $0.emoji == "🏆" })
    }

    @Test func goalEditsAreClampedToRange() {
        let store = makeStore()
        store.updateGoal(.weeklySessions, to: 50)
        #expect(store.weeklySessionGoal == GoalType.weeklySessions.range.upperBound)
    }

    @Test func demoHistoryGivesSixDayStreak() {
        let store = makeStore()
        store.loadDemoHistory()
        #expect(store.sessions.count == 6)
        #expect(store.streak.current == 6)
        #expect(store.celebrations.isEmpty)
    }
}
