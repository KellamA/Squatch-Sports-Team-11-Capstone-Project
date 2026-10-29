import SwiftUI

@main
struct Squatch_Sports_Basketball_Training_iOSApp: App {
    @StateObject private var appData = AppDataStore()

    init() {
        _ = WorkoutConnectivity.shared
    }

    var body: some Scene {
        WindowGroup {
            AppRootView()
                .environmentObject(appData)
                .environmentObject(WorkoutConnectivity.shared)
        }
    }
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
