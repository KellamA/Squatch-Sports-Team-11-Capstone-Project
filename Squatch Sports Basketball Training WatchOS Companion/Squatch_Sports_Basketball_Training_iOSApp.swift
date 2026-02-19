// Squatch_Sports_Basketball_Training_iOSApp.swift
// Main entry for iOS version of prototype workout app

import SwiftUI

@main
struct Squatch_Sports_Basketball_Training_iOSApp: App {
    @State private var showSplash = true
    init() { _ = WorkoutConnectivity.shared }
    var body: some Scene {
        WindowGroup {
            Group {
                if showSplash {
                    SplashView()
                        .background(Color.white)
                        .preferredColorScheme(.light)
                        .transition(.opacity)
                        .task {
                            try? await Task.sleep(for: .seconds(1.8))
                            await MainActor.run {
                                withAnimation(.easeInOut(duration: 0.25)) {
                                    showSplash = false
                                }
                            }
                        }
                } else {
                    DashboardHomeView()
                }
            }
            .animation(.easeInOut(duration: 0.25), value: showSplash)
        }
    }
}

