#if !os(watchOS)

//
//  Squatch_Sports_Basketball_Training_WatchOS_CompanionApp.swift
//  Squatch Sports Basketball Training WatchOS Companion
//
//  Created by Kellam Adams on 2/4/26.
//

import SwiftUI
import SwiftData
import WatchConnectivity
import Combine
// import SharedWorkout -- only if using as a module


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

    init() {
        _ = WorkoutConnectivity.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(WorkoutConnectivity.shared)
        }
        .modelContainer(sharedModelContainer)
    }
}

#endif
