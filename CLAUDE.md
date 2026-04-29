# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Squatch Sports is a basketball training app consisting of two targets that communicate over WatchConnectivity:

- **iOS app** — Dashboard, workout session management, and shot stat tracking
- **watchOS app** — Shot logging interface worn on the wrist during play

## Building & Running

Open in Xcode:
```
open "Squatch Sports Basketball Training WatchOS Companion.xcodeproj"
```

- Build/run via Xcode (Cmd+R). There is no CLI build script or test runner configured.
- The Xcode project has two schemes: `Squatch Sports Basketball Training WatchOS Companion` (iOS) and `Squatch Sports WatchOS Companion Watch App` (watchOS).
- To run tests: Cmd+U in Xcode, or via `xcodebuild test`.

## Architecture

### Communication Layer (`SharedWorkout.swift`)
`WorkoutConnectivity` is a singleton (`WorkoutConnectivity.shared`) that wraps `WCSession`. It is the single source of truth for workout state across both targets. Both iOS and watchOS targets compile this file. It publishes:
- `workoutActive: Bool` — toggled by `workoutStarted`/`workoutStopped` messages
- `receivedValue: Int?` — last shot code sent from the Watch

Shot codes: `0` = miss, `1` = make, `2` = swish (defined as `ShotDirection` enum in `ContentView.swift`).

### iOS Target (`Squatch Sports Basketball Training WatchOS Companion/`)
- `Squatch_Sports_Basketball_Training_iOSApp.swift` — App entry point; shows `SplashView` for 2.8s then `DashboardHomeView`
- `DashboardHomeView.swift` — Root navigation grid; links to `iPhoneWorkoutView`, `DrillsHomeView`, and placeholder views for History/Analytics/Settings
- `iPhoneWorkoutView.swift` — Active workout screen; sends start/stop signals to Watch via `WorkoutConnectivity`, receives shot codes via `onReceive(connectivity.$receivedValue)`, tracks makes/misses/swishes in local `@State`

### watchOS Target (`Squatch_Sports_WatchOS_CompanionApp.swift` + `ContentView.swift`)
- `SquatchSportsWatchOSCompanionApp` — Entry point; shows `SplashView` then `ContentView` after 2.8s
- `ContentView.swift` — Gesture-based shot logger. Shows a waiting screen until `connectivity.workoutActive == true`. During a workout, displays edge bars (Make/Miss/Swish) and a center drag pad. Swipe up = make, swipe down = miss, swipe right = swish. Sends `ShotDirection.rawValue` (Int) to iPhone via `connectivity.sendValueToPhone(_:)`.

### Shared UI (`SplashView.swift`)
Compiled into both targets via `#if os(watchOS)` conditional. Displays the Squatch Sports logo with an animated dot loader.

## Key Conventions

- `WorkoutConnectivity.shared` must be injected as an `@EnvironmentObject` on watchOS (`ContentView`) but is accessed via `@ObservedObject` on iOS (`iPhoneWorkoutView`).
- History, Analytics, and Settings sections currently show `PlaceholderView` — they are not yet implemented.
- `DrillsHomeView` exists as a file but its contents are minimal/placeholder.
- `Item.swift` exists in the iOS target but is unused boilerplate from the Xcode template.
