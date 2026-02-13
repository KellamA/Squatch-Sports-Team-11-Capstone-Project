//
//  Squatch_Sports_WatchOS_CompanionApp.swift
//  Squatch Sports WatchOS Companion Watch App
//
//  Created by Kellam Adams on 2/4/26.
//

import SwiftUI

@main
struct Squatch_Sports_WatchOS_Companion_Watch_AppApp: App {
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            Group {
                if showSplash {
                    SplashView()
                        .task {
                            try? await Task.sleep(for: .seconds(1.8))
                            showSplash = false
                        }
                } else {
                    ContentView()
                }
            }
        }
    }
}
