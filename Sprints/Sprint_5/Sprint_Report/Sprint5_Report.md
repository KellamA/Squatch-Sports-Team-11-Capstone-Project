# Sprint 5 Report (Oct 1, 2026 – Oct 31, 2026)

**Team 11:** Kellam Adams, Melvin Sanare, Kaleb Kebede  
**Project:** Squatch Sports Basketball Training App – Android/Wear OS Expansion and Gamification  
**Client:** Squatch Sports (Mike Niehl, Preston David)

## YouTube link of Sprint 5 Video (Make this video unlisted)
* [Youtube link:](https://youtu.be/azsZS2ShKGY)

## What's New (User Facing)
* **Same gamification on every platform:** Streaks, goal rings, celebration popups, and XP/levels now work on iPhone, Apple Watch, Android, and Wear OS. In Sprint 4, streaks and goals were Android-only and XP was iOS-only.
* **1v1 match mode (iPhone):** Add friends, set up a match with street rules (1s and 2s or 2s and 3s, play to a target score, win by 2, make it take it, coin flip for first ball), and keep score on a Red vs Blue scoreboard drawn on top of the training court. Track points, fouls, turnovers, steals, and possession, undo mistakes, and see your win/loss record against each friend.
* **1v1 scorekeeping on the Apple Watch:** When a match starts on the iPhone, the watch shows big +1/+2 (or +2/+3), Miss, Foul, and Turnover buttons for your side, with a second page for your opponent. The iPhone updates instantly, and plays logged while the phone is out of range sync when it reconnects.
* **Nearby 1v1 between two iPhones:** Two players can find each other nearby, start one shared match, and each log plays from their own phone.
* **1v1 on Android and Wear OS:** The 1v1 mode, scorekeeping from the Wear OS watch, saved matches and records, and nearby Android-to-Android play are now on Android.

## Work Summary (Developer Facing)
Sprint 5 started by closing the platform gap left at the end of Sprint 4, where each member had built gamification on a different platform. Melvin ported the Android streak and goal logic to Swift and Kaleb's XP system to Kotlin, using the existing Kotlin unit tests as the reference so both platforms produce the same results for the same dates and data. The main new feature, requested by the team and discussed with the client, was a 1v1 mode for players who compete against each other. We designed it around one rule: the iPhone keeps the official match and applies the rules, and every other device (the Apple Watch, or a second iPhone) sends plays to it and shows its scoreboard. A match is stored as a list of plays, so undo, history, and syncing all come from the same data. Melvin built the match rules, iPhone screens, and Apple Watch scorekeeping; Kaleb connected two nearby iPhones with MultipeerConnectivity; and Kellam ported the feature, including nearby play, to Android and Wear OS. Our biggest barriers were testing features that need several devices at once (two phones and two watches) and Xcode simulator quirks, such as a freshly booted simulator that never showed the app window. We solved them by testing in paired simulators, writing UI tests that play a full match, and seeding a match into the app's saved data to check phone-to-watch sync. While testing we found and fixed a real bug: a reinstalled watch app stayed on the waiting screen, so the watch now asks the iPhone for the current match when it opens or reconnects.

## Unfinished Work
* **Nearby 1v1 follow-ups (#31):** The nearby iPhone feature is tested and complete, but Kaleb's 4 new tests are written in XCTest style inside a Swift Testing file, so they do not run yet, and the `_udp` Bonjour service needed by MultipeerConnectivity on real iPhones is missing.
* **Real-device deployment (#34):** All Sprint 5 features were verified in Xcode simulators and Android emulators. Nearby Android play still needs testing on two physical Android phones, and Android-to-iPhone play is not supported. Installing on the team's own iPhones, Apple Watches, and Android devices, and a TestFlight / Play Console internal-testing build through the client's accounts, are still in progress.
* **Restrict 1v1 controls by possession (#25):** A player should not be able to log a basket when it is not their turn. Moved to Sprint 6.
* **Apple Watch repeated-shot bug (#19):** Still open from Sprint 4.

## Completed Issues/User Stories
Here are links to the issues that we completed in this sprint:

 * [#27 Port Android streaks and goals to iOS/watchOS and iOS XP to Android](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/27)
 * [#28 1v1 match mode on iPhone: friends, street rules, Red vs Blue scoreboard](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/28)
 * [#29 Apple Watch 1v1 scorekeeping synced with iPhone](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/29)
 * [#30 Nearby 1v1 between two iPhones](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/30)
 * [#32 Port 1v1 match mode to Android and Wear OS](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32)
 * [#20 Android/Wear OS parity polish and edge cases](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/20)

## Incomplete Issues/User Stories
Here are links to issues we worked on but did not complete in this sprint:

 * [#31 Nearby 1v1: make the new store tests run and add the _udp Bonjour service](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31) We found these problems in code review at the end of the sprint.
 * [#34 Deploy to real devices and prepare TestFlight / Play internal testing](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/34) A TestFlight or Play Console build requires the client's developer accounts, which we have not set up yet.
 * [#33 Sprint 5 system, integration, and user testing](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/33) Real-device and user testing depend on #34.
 * [#25 Restrict 1v1 controls based on player and possession](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25) We noticed this gameplay rule while testing the finished nearby 1v1 feature, so it was planned for Sprint 6.
 * [#19 Apple Watch misses repeated identical shots sent from iPhone](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/19) We prioritized the new 1v1 feature this sprint.

## Team Contributions

| Team member | Sprint 5 work | Issues | Story points |
|---|---|---|---|
| Melvin Sanare | Gamification parity across iOS and Android, 1v1 match mode on iPhone, Apple Watch 1v1 scorekeeping and sync, tests, GitHub issues, MoMs, report | #27, #28, #29, #33, #35 | 18 + documentation |
| Kaleb Kebede | Nearby two-phone 1v1 with MultipeerConnectivity, possession rules design | #30, #31, #25 | 5 |
| Kellam Adams | Android and Wear OS port of 1v1 with nearby Android play, Android test evidence, Sprint 5 slides | #32 | 8 |

## Testing and Validation

### Unit tests
| Test suite | What it checks | Result |
|---|---|---|
| `MatchEngineTests` (Swift) | 1v1 rules: scoring, possession after makes, fouls, turnovers, steals, missed shots, win by 2, make it take it | 13/13 pass |
| `OneOnOneMatchTests` (Swift) | Record and undo, finishing and reopening a match, player names by side, saving | 4/4 pass |
| `OneOnOneStoreTests` (Swift) | Friends, win/loss records, unsaved unfinished matches, resuming a paused match | 5/5 pass (Kaleb's 4 nearby tests do not run yet, #31) |
| `WatchMatchBridgeTests` (Swift) | What the watch receives, plays from the watch, ignoring old matches, saving from the watch | 7/7 pass |
| `StreakCalculatorTests`, `GoalTrackerTests`, `XPCalculatorTests`, `CelebrationMessageTests`, `AppDataStoreGamificationTests` (Swift) | Streak, goal, XP, and popup rules using the same known dates and data as the Kotlin tests | 40/40 pass |
| Android unit tests (Kotlin): `MatchEngineTest`, `MatchProtocolTest`, `NearbyMatchCoordinatorTest`, `NearbyMatchPayloadsTest`, `OneOnOneRepositoryTest`, `NearbyPermissionsTest`, `StreakCalculatorTest`, `GoalTrackerTest`, `GamificationPayloadsTest`, `XpCalculatorTest`, `ModelsTest`, `ExampleUnitTest` | 1v1 rules, saved matches, nearby sync, gamification | 121/121 pass |
| Android emulator UI tests | Phone 1v1 and nearby screens (9), Wear OS match screen (4) | 13/13 pass |

### Integration and system tests
| Test | Devices | Result |
|---|---|---|
| Play a full 1v1 match: add friend, start, log plays, win, save, record updates | iPhone (automated UI test) | ✅ Pass |
| Undo the last play | iPhone (automated UI test) | ✅ Pass |
| Match started on iPhone appears on the Apple Watch with the correct score and possession | iPhone + Apple Watch simulators | ✅ Pass |
| Plays tapped on the watch update the iPhone and come back to the watch | iPhone + Apple Watch simulators | ✅ Pass |
| Reinstalled or reopened watch app recovers the match in progress | iPhone + Apple Watch simulators | ✅ Pass after fix |
| Streak, goal, and level stats shown on each player's own watch | Two iPhones + two Apple Watches | ✅ Pass |
| Two nearby iPhones play one shared match | Two iPhones | ✅ Pass (tested by Kaleb) |
| 1v1 scoring from the phone and the watch, undo, and cancel stay in sync | Paired Android phone + Wear OS emulators | ✅ Pass |
| Two nearby Android phones play one shared match | Two physical Android phones | TODO |

### Non-functional checks
* **Reliability:** Plays from the watch are queued while the iPhone is out of range and delivered once it reconnects; each play has an ID so it is never counted twice.
* **Usability:** Watch buttons were kept large (+1/+2 at 48 pt tall), steals were left to the phone so the watch has only five buttons, and Wear OS controls were moved away from the round screen edges.
* **Performance:** TODO: watch-to-phone delay measured on real devices.

### User testing
* TODO: record feedback from players who tried 1v1 and from the client.

## Code Files for Review
Please review the following code files, which were actively developed during this sprint, for quality:
 * [MatchEngine.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/OneOnOne/MatchEngine.swift)
 * [OneOnOneStore.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/OneOnOne/OneOnOneStore.swift)
 * [MatchView.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/OneOnOne/MatchView.swift)
 * [WatchMatchBridge.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/OneOnOne/WatchMatchBridge.swift)
 * [SharedWorkout.swift (phone ↔ watch sync)](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/SharedWorkout.swift)
 * [MatchEngine.kt (Android)](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/android/shared/src/main/java/com/squatchsports/training/shared/MatchEngine.kt)
 * [MatchScreen.kt (Wear OS)](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/android/wear/src/main/java/com/squatchsports/training/presentation/MatchScreen.kt)
 * [ContentView.swift (watch 1v1 screens)](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/ContentView.swift)
 * [NearbyMatchService.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/OneOnOne/NearbyMatchService.swift)
 * [Gamification.swift](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/main/Squatch%20Sports%20Basketball%20Training%20WatchOS%20Companion/Gamification.swift)

## Client Meetings
 * [September 30, 2026 – Sprint 4 demo and Sprint 5 planning](../MoM/MoM_2026-09-30.md)
 * [October 14, 2026 – Sprint 5 progress and 1v1 design review](../MoM/MoM_2026-10-14.md)
 * October 28, 2026 – cancelled by the team (not enough new progress to report)

## Responsible Use of Generative AI
* **How we used it:** The team used AI coding tools, such as Claude Code, to help write the harder features this sprint, including the cross-platform gamification port, the 1v1 match mode, Apple Watch scorekeeping, and nearby phone-to-phone play. We did not use AI for everything; the team designed the features, wrote and reviewed code ourselves, and tested every change.
* **What it improved:** We built and tested the harder features much faster, with automated tests that play a full match.
* **Limitations we found:** AI-generated code passed its tests but still had a phone-to-watch sync bug that we only found by testing in the simulators. AI also cannot make product decisions; the team decided the 1v1 rules and what belongs on the watch.
* **Accountability:** All AI-assisted work was reviewed and tested by the team, and all final decisions and deliverables remain the team's responsibility.

## Retrospective Summary
Here's what went well:
  * Gamification is now consistent across iOS and Android, fixing the main problem from the Sprint 4 retrospective.
  * The 1v1 feature went from idea to a working iPhone and Apple Watch version with tests in one sprint.
  * Working from one shared feature branch and a written plan let Kaleb build nearby play directly on top of the 1v1 work.
  * GitHub issues now include acceptance criteria, story points, assignees, and linked pull requests.

Here's what we'd like to improve:
  * Test on real devices earlier; multi-device features are hard to verify in simulators.
  * Start each platform's version of a feature at the same time so the Android port does not trail the iOS work.
  * Review pull requests before merging so problems like tests that never run are caught earlier.

Here are changes we plan to implement in the next sprint:
  * Restrict 1v1 controls by possession so players cannot score out of turn (#25).
  * Finish the nearby follow-ups (#31) and test nearby Android play on physical phones.
  * Deploy through TestFlight and Play Console internal testing with the client, and run user testing on real devices (#34, #33).
  * Keep the watch app on screen during a match with a HealthKit workout session (#36).
  * Final regression testing, UI/UX polish, documentation, and handoff to Squatch Sports.
