import SwiftUI

@main
struct Squatch_Sports_Basketball_Training_iOSApp: App {
    @StateObject private var appData = AppDataStore()

    init() {
        _ = WorkoutConnectivity.shared
    }

    var body: some Scene {
        WindowGroup {
            DashboardHomeView()
                .environmentObject(appData)
                .environmentObject(WorkoutConnectivity.shared)
        }
    }
}
