// Squatch_Sports_Basketball_Training_iOSApp.swift
// Main entry for iOS version of prototype workout app

import SwiftUI

@main
struct Squatch_Sports_Basketball_Training_iOSApp: App {
    // Ensure connectivity is started
    init() { _ = WorkoutConnectivity.shared }
    var body: some Scene {
        WindowGroup {
            iPhoneWorkoutView()
        }
    }
}
