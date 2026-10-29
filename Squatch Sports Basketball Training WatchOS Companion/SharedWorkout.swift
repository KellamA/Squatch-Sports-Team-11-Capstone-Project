// SharedWorkout.swift
// Shared model and connectivity code for iPhone <-> Watch workout coordination

import Foundation
import WatchConnectivity
import Combine

// Workout model (simplified for prototype)
struct Workout: Identifiable, Codable {
    let id: UUID
    let startDate: Date
    var userValue: Int?
}

// MARK: - Gamification Models (shared with the watch)
// Mirrors android/shared/.../Goals.kt and Gamification.kt so both platforms behave the same.

nonisolated enum GoalPeriod {
    case day
    case week
}

/// Raw values match the Android enum names so synced payloads look the same on both platforms.
nonisolated enum GoalType: String, CaseIterable, Codable {
    case dailyShots = "DAILY_SHOTS"
    case dailyMakes = "DAILY_MAKES"
    case weeklySessions = "WEEKLY_SESSIONS"
    case targetFG = "TARGET_FG"

    var title: String {
        switch self {
        case .dailyShots: "Daily Shots"
        case .dailyMakes: "Daily Makes"
        case .weeklySessions: "Weekly Sessions"
        case .targetFG: "Today's FG%"
        }
    }

    var unit: String {
        switch self {
        case .dailyShots: "shots"
        case .dailyMakes: "makes"
        case .weeklySessions: "sessions"
        case .targetFG: "%"
        }
    }

    /// Short label for the small rings on the watch.
    var ringLabel: String {
        switch self {
        case .dailyShots: "Shots"
        case .dailyMakes: "Makes"
        case .weeklySessions: "Week"
        case .targetFG: "FG%"
        }
    }

    var period: GoalPeriod {
        self == .weeklySessions ? .week : .day
    }

    /// Allowed values when editing the goal.
    var range: ClosedRange<Int> {
        switch self {
        case .dailyShots: 10...2000
        case .dailyMakes: 5...1000
        case .weeklySessions: 1...21
        case .targetFG: 10...100
        }
    }
}

/// Progress toward one goal. `eligible` is false when the goal cannot be completed yet
/// (FG% needs at least 10 shots today so one lucky shot does not count).
nonisolated struct GoalProgress: Codable, Equatable {
    let type: GoalType
    let current: Int
    let target: Int
    var eligible: Bool = true

    var fraction: Double {
        guard target > 0 else { return 0 }
        return min(max(Double(current) / Double(target), 0), 1)
    }

    var isComplete: Bool {
        eligible && target > 0 && current >= target
    }
}

/// Text for a celebration popup, shared so the phone and watch show the same wording.
/// `id` is new for every popup so the watch also shows repeated identical messages.
nonisolated struct CelebrationMessage: Codable, Equatable, Identifiable {
    var id = UUID()
    let emoji: String
    let headline: String
    let caption: String
    let title: String
    let subtitle: String
}

/// Streak, goal, and level snapshot the iPhone syncs to the watch.
nonisolated struct WatchStats: Codable, Equatable {
    let currentStreak: Int
    let bestStreak: Int
    let trainedToday: Bool
    let goals: [GoalProgress]
    let level: Int
    let xpInCurrentLevel: Int
}

// MARK: - Connectivity

// State sync message types
enum WorkoutMessage: String, Codable {
    case workoutStarted
    case workoutStopped
    case sendValue
    case updatePosition
    case drillInfo
    case celebration
}

// Connectivity handler for iOS/WatchOS
class WorkoutConnectivity: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WorkoutConnectivity()
    @Published var workoutActive: Bool = false
    @Published var receivedValue: Int? = nil
    @Published var currentPosition: CourtPosition? = nil
    @Published var currentDrill: String? = nil

    /// Latest streak/goal snapshot from the iPhone (watch side), or nil until one arrives.
    @Published var stats: WatchStats? = nil
    /// Latest celebration popup from the iPhone (watch side).
    @Published var celebration: CelebrationMessage? = nil

    private static let statsKey = "stats"
    private static let payloadKey = "payload"

    /// Stats waiting for the session to finish activating.
    private var pendingStats: WatchStats?

    private override init() {
        super.init()
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        } else {
            print("WCSession is not supported on this device. Connectivity features are disabled.")
        }
    }

    // MARK: - Senders
    func sendWorkoutStarted() {
        sendMessage(["type": WorkoutMessage.workoutStarted.rawValue])
    }
    func sendWorkoutStopped() {
        sendMessage(["type": WorkoutMessage.workoutStopped.rawValue])
    }
    func sendValueToPhone(_ value: Int) {
        sendMessage(["type": WorkoutMessage.sendValue.rawValue, "value": value])
    }
    func sendPositionUpdate(_ position: CourtPosition) {
        if let encoded = try? JSONEncoder().encode(position),
           let json = try? JSONSerialization.jsonObject(with: encoded) as? [String: Any] {
            var message = json
            message["type"] = WorkoutMessage.updatePosition.rawValue
            sendMessage(message)
        }
    }
    func sendDrillInfo(_ drillName: String) {
        sendMessage(["type": WorkoutMessage.drillInfo.rawValue, "drillName": drillName])
    }

    /// Mirrors a celebration popup on the watch (only when it is reachable, like other live messages).
    func sendCelebration(_ message: CelebrationMessage) {
        guard let data = try? JSONEncoder().encode(message) else { return }
        sendMessage(["type": WorkoutMessage.celebration.rawValue, Self.payloadKey: data])
    }

    /// Syncs the latest streak/goal snapshot. Stored as application context, so the watch
    /// receives it even if the watch app opens later.
    func publishStats(_ stats: WatchStats) {
        pendingStats = stats
        flushPendingStats()
    }

    private func flushPendingStats() {
        guard WCSession.isSupported(),
              WCSession.default.activationState == .activated,
              let stats = pendingStats,
              let data = try? JSONEncoder().encode(stats) else { return }
        do {
            try WCSession.default.updateApplicationContext([Self.statsKey: data])
            pendingStats = nil
        } catch {
            print("Unable to publish stats: \(error.localizedDescription)")
        }
    }

    private func sendMessage(_ dict: [String: Any]) {
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(dict, replyHandler: nil, errorHandler: nil)
        }
    }

    private func applyStats(from context: [String: Any]) {
        guard let data = context[Self.statsKey] as? Data,
              let decoded = try? JSONDecoder().decode(WatchStats.self, from: data) else { return }
        DispatchQueue.main.async {
            self.stats = decoded
        }
    }

    // MARK: - WCSessionDelegate
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            print("WCSession activation failed with error: \(error.localizedDescription)")
        } else {
            print("WCSession activated with state: \(activationState.rawValue)")
        }
        DispatchQueue.main.async {
            #if os(watchOS)
            // Show the stats the iPhone last published right away on launch.
            self.applyStats(from: session.receivedApplicationContext)
            #else
            self.flushPendingStats()
            #endif
        }
    }

    #if os(iOS)
    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) { WCSession.default.activate() }
    #endif

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        applyStats(from: applicationContext)
    }

    func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
        DispatchQueue.main.async {
            if let type = message["type"] as? String, let msg = WorkoutMessage(rawValue: type) {
                switch msg {
                case .workoutStarted:
                    self.workoutActive = true
                case .workoutStopped:
                    self.workoutActive = false
                    self.currentPosition = nil
                    self.currentDrill = nil
                case .sendValue:
                    if let value = message["value"] as? Int {
                        self.receivedValue = value
                    }
                case .updatePosition:
                    // Decode position from message
                    var positionDict = message
                    positionDict.removeValue(forKey: "type")
                    if let data = try? JSONSerialization.data(withJSONObject: positionDict),
                       let position = try? JSONDecoder().decode(CourtPosition.self, from: data) {
                        self.currentPosition = position
                    }
                case .drillInfo:
                    if let drillName = message["drillName"] as? String {
                        self.currentDrill = drillName
                    }
                case .celebration:
                    if let data = message[Self.payloadKey] as? Data,
                       let decoded = try? JSONDecoder().decode(CelebrationMessage.self, from: data) {
                        self.celebration = decoded
                    }
                }
            }
        }
    }
}
