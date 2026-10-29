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

// MARK: - 1v1 Match Models (shared with the watch)
// The rules engine is iPhone-only (OneOnOne/MatchEngine.swift); the watch sends these events and shows the phone's scoreboard.

/// The two players in a match. Red and blue are colors, not people: the setup screen decides who is which.
nonisolated enum PlayerSide: String, Codable, CaseIterable, Identifiable {
    case red
    case blue

    var id: String { rawValue }

    var opponent: PlayerSide {
        self == .red ? .blue : .red
    }

    var title: String {
        self == .red ? "Red" : "Blue"
    }
}

/// How baskets are worth. Streetball 1v1 is usually 1s and 2s.
nonisolated enum ScoringSystem: String, Codable, CaseIterable, Identifiable {
    case onesAndTwos
    case twosAndThrees

    var id: String { rawValue }

    var title: String {
        self == .onesAndTwos ? "1s and 2s" : "2s and 3s"
    }

    /// Points for a basket inside the arc.
    var insidePoints: Int {
        self == .onesAndTwos ? 1 : 2
    }

    /// Points for a basket from beyond the arc.
    var outsidePoints: Int {
        self == .onesAndTwos ? 2 : 3
    }

    /// A sensible default target for this scoring system.
    var defaultTargetScore: Int {
        self == .onesAndTwos ? 11 : 21
    }
}

nonisolated struct MatchSettings: Codable, Equatable {
    var scoring: ScoringSystem = .onesAndTwos
    var targetScore = 11
    /// The winner must lead by at least 2 when reaching the target.
    var winByTwo = true
    /// The scorer keeps the ball after a make instead of giving it to the other player.
    var makeItTakeIt = false
    /// Who checks the ball first.
    var firstPossession: PlayerSide = .red

    static let targetRange = 5...50
}

/// Something that happened in a match. Each case names the player who did it.
nonisolated enum MatchEvent: Codable, Equatable {
    /// A made basket worth `points`.
    case score(PlayerSide, points: Int)
    /// A foul committed by this player; the other player gets the ball.
    case foul(PlayerSide)
    /// This player lost the ball (bad dribble, out of bounds, travel...); the other player gets it.
    case turnover(PlayerSide)
    /// This player stole the ball; counts as a turnover for the other player.
    case steal(PlayerSide)
    /// The ball goes to this player without a stat, e.g. they got it back after a missed shot.
    case possession(PlayerSide)

    var side: PlayerSide {
        switch self {
        case .score(let side, _), .foul(let side), .turnover(let side), .steal(let side), .possession(let side):
            side
        }
    }

    /// Short play-by-play text using color names, e.g. "Red +2" or "Blue foul".
    var summary: String {
        summary { $0.title }
    }

    /// Play-by-play text with player names, e.g. "Kaleb +2".
    func summary(name: (PlayerSide) -> String) -> String {
        switch self {
        case .score(let side, let points): "\(name(side)) +\(points)"
        case .foul(let side): "\(name(side)) foul"
        case .turnover(let side): "\(name(side)) turnover"
        case .steal(let side): "\(name(side)) steal"
        case .possession(let side): "Ball → \(name(side))"
        }
    }
}

/// What the watch shows during a 1v1 match. Built by the iPhone, which keeps the real match and rules.
nonisolated struct WatchMatchSnapshot: Codable, Equatable {
    let matchID: UUID
    /// When the iPhone built this snapshot; the watch ignores older ones that arrive late.
    let updatedAt: Date
    let userSide: PlayerSide
    let userName: String
    let opponentName: String
    let scoring: ScoringSystem
    let targetScore: Int
    let redPoints: Int
    let bluePoints: Int
    let redFouls: Int
    let blueFouls: Int
    let possession: PlayerSide
    let winner: PlayerSide?
    let lastPlay: String?
    let canUndo: Bool

    func points(for side: PlayerSide) -> Int {
        side == .red ? redPoints : bluePoints
    }

    func fouls(for side: PlayerSide) -> Int {
        side == .red ? redFouls : blueFouls
    }

    func name(for side: PlayerSide) -> String {
        side == userSide ? userName : opponentName
    }
}

/// Something the watch asks the iPhone to do to the current match.
nonisolated struct WatchMatchCommand: Codable, Equatable, Identifiable {
    enum Action: Codable, Equatable {
        case record(MatchEvent)
        case undo
        /// Save the finished match (same as "Save Match" on the iPhone).
        case finish
    }

    /// Lets the iPhone ignore a command that was delivered twice.
    var id = UUID()
    let matchID: UUID
    let action: Action
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
    case matchSnapshot
    case matchEnded
    case matchCommand
    /// Watch asks the iPhone to resend the current 1v1 match (on launch or reconnect).
    case matchRequest
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
    /// The 1v1 match in progress on the iPhone (watch side), or nil when there is none.
    @Published var match: WatchMatchSnapshot? = nil
    /// Whether the other device is reachable right now for live messages.
    @Published var isCounterpartReachable = false

    /// iPhone side: called on the main queue for each 1v1 command from the watch.
    var onMatchCommand: ((WatchMatchCommand) -> Void)?
    /// iPhone side: called when the watch asks for the current 1v1 match.
    var onMatchRequest: (() -> Void)?

    private static let statsKey = "stats"
    private static let matchKey = "match"
    private static let payloadKey = "payload"

    /// Latest values synced through the application context. The context is replaced as a whole,
    /// so stats and the match are always sent together.
    private var contextPayload: [String: Any] = [:]
    private var contextNeedsSending = false
    /// Recent command IDs, so a command delivered twice is applied once.
    private var handledCommandIDs: [UUID] = []

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
        guard let data = try? JSONEncoder().encode(stats) else { return }
        contextPayload[Self.statsKey] = data
        flushContext()
    }

    /// iPhone side: sends the 1v1 scoreboard to the watch, or tells it the match ended (nil).
    /// Sent live when the watch is reachable and also stored as application context for later.
    func publishMatch(_ snapshot: WatchMatchSnapshot?) {
        if let snapshot, let data = try? JSONEncoder().encode(snapshot) {
            contextPayload[Self.matchKey] = data
            sendMessage(["type": WorkoutMessage.matchSnapshot.rawValue, Self.payloadKey: data])
        } else {
            contextPayload.removeValue(forKey: Self.matchKey)
            sendMessage(["type": WorkoutMessage.matchEnded.rawValue])
        }
        flushContext()
    }

    /// Watch side: sends a 1v1 play (or undo/finish) to the iPhone. Sent live when possible,
    /// otherwise queued by the system and delivered when the iPhone is back in range.
    func sendMatchCommand(_ command: WatchMatchCommand) {
        guard WCSession.isSupported(), let data = try? JSONEncoder().encode(command) else { return }
        let message: [String: Any] = ["type": WorkoutMessage.matchCommand.rawValue, Self.payloadKey: data]
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(message, replyHandler: nil) { _ in
                // The live send failed; queue it. The iPhone ignores it if it already arrived.
                WCSession.default.transferUserInfo(message)
            }
        } else {
            WCSession.default.transferUserInfo(message)
        }
    }

    private func flushContext() {
        contextNeedsSending = true
        guard WCSession.isSupported(), WCSession.default.activationState == .activated else { return }
        do {
            try WCSession.default.updateApplicationContext(contextPayload)
            contextNeedsSending = false
        } catch {
            print("Unable to update application context: \(error.localizedDescription)")
        }
    }

    private func sendMessage(_ dict: [String: Any]) {
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(dict, replyHandler: nil, errorHandler: nil)
        }
    }

    // MARK: - Receiving

    private func applyContext(_ context: [String: Any]) {
        let stats = (context[Self.statsKey] as? Data).flatMap { try? JSONDecoder().decode(WatchStats.self, from: $0) }
        let match = (context[Self.matchKey] as? Data).flatMap { try? JSONDecoder().decode(WatchMatchSnapshot.self, from: $0) }
        DispatchQueue.main.async {
            if let stats { self.stats = stats }
            if let match {
                self.applyMatch(match)
            } else if context[Self.matchKey] == nil, self.match?.winner == nil {
                // No match in the context: the iPhone ended it. Keep a finished game on screen
                // until the watch user taps Done.
                self.match = nil
            }
        }
    }

    /// Ignores snapshots that are older than the one already shown (messages can arrive out of order).
    private func applyMatch(_ snapshot: WatchMatchSnapshot) {
        if let current = match, current.matchID == snapshot.matchID, current.updatedAt > snapshot.updatedAt {
            return
        }
        match = snapshot
    }

    private func handleMatchCommand(_ data: Data) {
        guard let command = try? JSONDecoder().decode(WatchMatchCommand.self, from: data),
              !handledCommandIDs.contains(command.id) else { return }
        handledCommandIDs.append(command.id)
        if handledCommandIDs.count > 200 {
            handledCommandIDs.removeFirst(handledCommandIDs.count - 200)
        }
        onMatchCommand?(command)
    }

    // MARK: - WCSessionDelegate
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            print("WCSession activation failed with error: \(error.localizedDescription)")
        } else {
            print("WCSession activated with state: \(activationState.rawValue)")
        }
        DispatchQueue.main.async {
            self.isCounterpartReachable = session.isReachable
            #if os(watchOS)
            // Show what the iPhone last published (stats, a match in progress) right away on launch,
            // then ask for the latest match in case that copy is missing or out of date.
            self.applyContext(session.receivedApplicationContext)
            self.requestMatch()
            #else
            if self.contextNeedsSending {
                self.flushContext()
            }
            #endif
        }
    }

    #if os(iOS)
    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) { WCSession.default.activate() }
    #endif

    func sessionReachabilityDidChange(_ session: WCSession) {
        DispatchQueue.main.async {
            self.isCounterpartReachable = session.isReachable
            #if os(watchOS)
            if session.isReachable {
                self.requestMatch()
            }
            #endif
        }
    }

    /// Watch side: asks the iPhone to resend the current 1v1 match.
    func requestMatch() {
        sendMessage(["type": WorkoutMessage.matchRequest.rawValue])
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        applyContext(applicationContext)
    }

    /// Queued 1v1 commands from the watch that were sent while the iPhone was out of reach.
    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        guard userInfo["type"] as? String == WorkoutMessage.matchCommand.rawValue,
              let data = userInfo[Self.payloadKey] as? Data else { return }
        DispatchQueue.main.async {
            self.handleMatchCommand(data)
        }
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
                case .matchSnapshot:
                    if let data = message[Self.payloadKey] as? Data,
                       let snapshot = try? JSONDecoder().decode(WatchMatchSnapshot.self, from: data) {
                        self.applyMatch(snapshot)
                    }
                case .matchEnded:
                    // Keep a finished game on screen until the watch user taps Done.
                    if self.match?.winner == nil {
                        self.match = nil
                    }
                case .matchCommand:
                    if let data = message[Self.payloadKey] as? Data {
                        self.handleMatchCommand(data)
                    }
                case .matchRequest:
                    self.onMatchRequest?()
                }
            }
        }
    }

    /// Watch side: hides a finished match after the user taps Done.
    func clearFinishedMatch() {
        if match?.winner != nil {
            match = nil
        }
    }
}
