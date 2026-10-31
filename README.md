# Squatch Sports Basketball Training App

CptS 423 Capstone – Team 11: Kellam Adams, Melvin Sanare, Kaleb Kebede  
Client: Squatch Sports

## Project summary

### One-sentence description of the project

A basketball shooting-practice app where players pick a drill on their phone, log every make, miss, and swish on their smartwatch with a quick swipe, and stay motivated with streaks, goals, and levels.

### Additional information about the project

Squatch Sports helps basketball players train on their own. Logging every shot during practice is tedious, so this app moves shot logging to the wrist: the phone runs the drill and shows the court spot to shoot from, and the watch records each shot with a swipe (up = make, down = miss, right = swish). Completed workouts are saved and feed History, Analytics, and Goals.

The first semester built the iOS and watchOS app, which Squatch Sports has released in its production app. The second semester expands the app to **Android and Wear OS** and adds a **gamification layer** inspired by Duolingo. The client calls this "hiding the broccoli": making the useful but tedious parts (logging workouts and reviewing stats) enjoyable enough that players keep coming back.

| Platform | Location in this repo |
|---|---|
| iOS phone app + watchOS app (SwiftUI) | `Squatch Sports Basketball Training WatchOS Companion/` and the `.xcodeproj` |
| Android phone app + Wear OS app (Kotlin, Jetpack Compose) | `android/` |

## Installation

### Prerequisites

**Android and Wear OS (Windows, macOS, or Linux)**
* [Git](https://git-scm.com/)
* [Android Studio](https://developer.android.com/studio) 2026.1 or newer. It includes the JDK, and it installs the Android SDK (API 37) on first launch.
* About 10 GB of free disk space for the SDK and emulators

**iOS and watchOS (macOS only)**
* A Mac with Xcode 26 or newer (the project targets iOS 26.2 and watchOS 26.2)

### Add-ons

**Android (`android/`)**
| Library | Purpose |
|---|---|
| Jetpack Compose + Material 3 | Phone user interface |
| Wear Compose Material 3 | Watch user interface |
| Google Play services Wearable (Data Layer) | Phone-to-watch messages and streak/goal sync |
| AndroidX Core SplashScreen | Watch splash screen |
| JUnit 4, org.json (tests only) | Unit tests |

**iOS (`Squatch Sports Basketball Training WatchOS Companion/`)**
| Framework | Purpose |
|---|---|
| SwiftUI | iPhone and Apple Watch user interface |
| WatchConnectivity | iPhone-to-Apple Watch messages |
| Combine | Shared app state |

### Installation Steps

**1. Clone the repository**
```bash
git clone https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project.git
cd Squatch-Sports-Team-11-Capstone-Project
```

**2a. Run the Android phone and Wear OS apps**
1. Open Android Studio and choose **File → Open**. Select the **`android`** folder (not the repository root) and wait for Gradle sync to finish.
2. In **Device Manager**, create a phone emulator using a **Google Play** system image (for example a Pixel), and a **Wear OS** emulator.
3. In Device Manager, open the Wear OS emulator's **⋮** menu → **Pair Wearable**, choose the phone, and follow the pairing steps on the phone.
4. Run the **`app`** configuration on the phone and the **`wear`** configuration on the watch.

To build and run the unit tests from the command line instead:
```bash
cd android
./gradlew :app:assembleDebug :wear:assembleDebug testDebugUnitTest
```
On Windows, use `gradlew.bat`. If Java is not found, point `JAVA_HOME` at Android Studio's bundled JDK (for example `C:\Program Files\Android\Android Studio\jbr`).

**2b. Run the iOS and watchOS apps (Mac only)**
1. Open `Squatch Sports Basketball Training WatchOS Companion.xcodeproj` in Xcode.
2. Select the **Squatch Sports Basketball Training WatchOS Companion** scheme and an iPhone simulator paired with an Apple Watch simulator.
3. Press **Run**. To run the watch app, select the **Squatch Sports WatchOS Companion Watch App** scheme.

**3. Load sample data**  
In the phone app (Android or iOS), open **Settings → Load Demo History**. This replaces the workout history with a week of sample workouts so streaks, goals, History, and Analytics have data to show.

## Functionality

A typical session:
1. **Dashboard.** The phone shows your streak (with a 7-day calendar), your level and XP, progress rings for your goals, and today's stats.
2. **Pick a drill.** Open **Drills**, choose one of 8 drills (for example Spot Shooting), read the goal and instructions, and tap **Start Drill**, then **Start Workout**.
3. **Log shots on the watch.** The watch shows a court with the spot to shoot from. Swipe the center pad: **up = make, down = miss, right = swish**. After 5 shots the drill moves to the next spot automatically. The phone's Quick Log buttons can also be used for testing without a watch.
4. **Finish.** The watch shows a summary by court spot. The phone saves the workout to History and Analytics.
5. **Celebrate.** Every workout earns XP (with a level-up popup at each new level). If the workout grows your streak or completes a goal, more popups follow. Each popup appears on the phone and the watch (with a vibration).
6. **Adjust goals.** Open **Goals** and tap the edit (pencil) button to change daily shots, daily makes, weekly sessions, or target FG%.

Gamification rules (same on Android/Wear OS and iOS/watchOS):
* **Streak:** any day with at least one workout counts. One missed day is allowed as a rest day; two missed days in a row reset the current streak. The best streak is kept.
* **Goals:** daily shots, daily makes, weekly sessions, and today's FG% (FG% only counts after 10 shots). Each goal celebrates once per day or week.
* **XP:** 25 XP per workout, 1 XP per make, 2 bonus XP per swish, and 15 bonus XP for 75%+ shooting. You level up every 100 XP.

### 1v1 matches (iOS/watchOS and Android/Wear OS)

Open **Dashboard → 1v1**, add a friend, and tap **Play**. Choose your color, 1s/2s or 2s/3s, a target score (5–50), win-by-two, make-it-take-it, and first possession (or flip a coin). The live court tracks points, possession, fouls, turnovers, and steals, with undo. Pause and resume a game, then save its result to your overall and head-to-head records.

The watch shows both scores and lets you log either player's baskets, misses, fouls, and turnovers, undo, and save a finished match. On Wear OS, swipe left/right or tap the player-switch button. Android match plays are persisted in a FIFO outbox and delivered through the Wear OS Data Layer; the phone deduplicates them and can process them while its activity is closed. The watch shows pending plays until the phone acknowledges them. This reliability applies to **1v1**, not the older workout-shot protocol.

Friends and records are stored locally, not in online accounts. Nearby games copy the shared match to both players' phones. Detailed play-by-play/history screens, extra foul penalties, and steals on the watch remain future work on both platforms. Open the watch app to use scorekeeping; neither platform automatically launches it or starts a fitness-tracking session.

### Nearby multiplayer

Open **1v1 → Nearby Players** on both phones to discover an opponent and play a shared game. The Android port uses encrypted Google Nearby Connections: grant nearby permissions, connect from one phone, compare and accept the code on both phones, then let the connecting phone configure/start the game. Either phone or paired watch can log plays; scores, undo, save and cancellation synchronize. The host orders all Android plays, and pending guest commands survive disconnects/restarts until acknowledged. Reconnect to the same phone to resume syncing.

**iPhone-to-iPhone and Android-to-Android are supported separately—not Android-to-iPhone.** iOS uses Apple's MultipeerConnectivity; the transports/protocols are not interoperable. Keep both phone apps open. Physical-device discovery/transport still requires release testing; see [`android/README.md`](android/README.md#nearby-1v1-two-android-phones).

## Known Problems

* **Apple Watch misses repeated shots from the iPhone** ([#19](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/19)). In `ContentView.swift`, `.onChange(of: connectivity.receivedValue)` does not fire when the same shot code arrives twice in a row. Reproduce by tapping MAKE twice on the iPhone's Quick Log.
* **Android polish still in progress** ([#20](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/20)):
  * The Settings preference toggles are still placeholders and are not permanently saved (`SettingsScreen.kt`). Connection status now reflects connected Wear OS nodes.
  * Some drill descriptions list different shots per spot than the app uses. For example, Spot Shooting says 10, while every drill uses 5 (`Drills.kt`).
  * Shots are not queued if the phone and watch disconnect during a workout (`WorkoutConnectivity.kt`).
* **Emulator tip:** if the Android emulator stops responding to mouse clicks, cold boot it from **Device Manager → ⋮ → Cold Boot Now**. The API 37 phone image also logs repeated crashes of a UWB hardware service; this is an emulator issue and does not affect the app.

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## Additional Documentation

* Sprint reports: [Sprint 1](Sprints/Sprint_1/Sprint_Report/Sprint1_Report.md), [Sprint 2](Sprints/Sprint_2/Sprint_Report/Sprint2_Report.md), [Sprint 3](Sprints/Sprint_3/Sprint_Report/Sprint3_Report.md), [Sprint 4](Sprints/Sprint_4/Sprint_Report/Sprint4_Report.md)
* Client meeting minutes: [Sprint 4 MoMs](Sprints/Sprint_4/MoM/)
* Android project notes: [android/README.md](android/README.md)
* Squatch Sports: <https://www.squatchsports.com>

## License

This project was developed for Squatch Sports and is not licensed for public reuse. Contact Squatch Sports for permission to use this code.
