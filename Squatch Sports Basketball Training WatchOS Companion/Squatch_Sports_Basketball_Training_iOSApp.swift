import SwiftUI

@main
struct Squatch_Sports_Basketball_Training_iOSApp: App {
    @StateObject private var appData = AppDataStore(defaults: AppDefaults.current)
    @StateObject private var oneOnOne = OneOnOneStore.shared

    init() {
        // Link the 1v1 match to the watch before any UI, so plays from the watch are handled
        // even when iOS launches the app in the background to deliver them.
        WatchMatchBridge.connect(store: OneOnOneStore.shared, connectivity: WorkoutConnectivity.shared)
    }

    var body: some Scene {
        WindowGroup {
            AppRootView()
                .environmentObject(appData)
                .environmentObject(oneOnOne)
                .environmentObject(WorkoutConnectivity.shared)
        }
    }
}

enum AppDefaults {
    /// Where the app saves its data. UI tests launch with `-uiTesting` to start from empty,
    /// separate storage instead of the user's data.
    static let current: UserDefaults = {
        guard CommandLine.arguments.contains("-uiTesting") else { return .standard }
        let suite = "squatch.uiTesting"
        let defaults = UserDefaults(suiteName: suite) ?? .standard
        defaults.removePersistentDomain(forName: suite)
        return defaults
    }()
}

/// Dashboard plus app-wide celebration popups and watch sync.
private struct AppRootView: View {
    @EnvironmentObject var appData: AppDataStore
    @EnvironmentObject var connectivity: WorkoutConnectivity

    var body: some View {
        DashboardHomeView()
            .overlay {
                CelebrationOverlay(message: appData.celebrations.first, onDismiss: appData.dismissCelebration)
            }
            // Keep the watch's streak/goal display current, and mirror each popup on the watch.
            .onChange(of: appData.watchStats, initial: true) { _, stats in
                connectivity.publishStats(stats)
            }
            .onChange(of: appData.celebrations.first?.id) { _, _ in
                if let message = appData.celebrations.first {
                    connectivity.sendCelebration(message)
                }
            }
    }
}
