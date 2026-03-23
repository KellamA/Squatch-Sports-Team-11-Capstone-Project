#if !os(watchOS)

import SwiftUI
import SwiftData
import WatchConnectivity
import Combine

struct Squatch_Sports_Basketball_Training_WatchOS_CompanionApp: App {
    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            Item.self,
        ])

        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

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
        .modelContainer(sharedModelContainer)
    }
}

#endif
