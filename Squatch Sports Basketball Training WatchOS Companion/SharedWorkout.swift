// SharedWorkout.swift
// Shared model and connectivity code for iPhone <-> Watch workout coordination

import Foundation
import WatchConnectivity

// Workout model (simplified for prototype)
struct Workout: Identifiable, Codable {
    let id: UUID
    let startDate: Date
    var userValue: Int?
}

// State sync message types
enum WorkoutMessage: String, Codable {
    case workoutStarted
    case workoutStopped
    case sendValue
}

// Connectivity handler for iOS/WatchOS
class WorkoutConnectivity: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WorkoutConnectivity()
    @Published var workoutActive: Bool = false
    @Published var receivedValue: Int? = nil

    private override init() {
        super.init()
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
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

    private func sendMessage(_ dict: [String: Any]) {
        if WCSession.default.isReachable {
            WCSession.default.sendMessage(dict, replyHandler: nil, errorHandler: nil)
        }
    }

    // MARK: - WCSessionDelegate
    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {}

    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) { WCSession.default.activate() }

    func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
        DispatchQueue.main.async {
            if let type = message["type"] as? String, let msg = WorkoutMessage(rawValue: type) {
                switch msg {
                case .workoutStarted:
                    self.workoutActive = true
                case .workoutStopped:
                    self.workoutActive = false
                case .sendValue:
                    if let value = message["value"] as? Int {
                        self.receivedValue = value
                    }
                }
            }
        }
    }
}
