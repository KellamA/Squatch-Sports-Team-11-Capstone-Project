# Sprint 5 presenter notes

Layout: Sprint 4 deck; content order follows the requested video template.
Evidence distinguishes verified Android merge status, team-confirmed Kaleb completion, and remaining Android hardware/deployment checks.

## Slide 1 — Kellam (~20s)

Welcome to Team 11’s Sprint 5 review. We are Kellam, Melvin, and Kaleb, building the Squatch Sports basketball training app. This sprint adds 1v1 scorekeeping and nearby play while aligning the major features across the Apple and Android apps. The cover uses the project’s existing Squatch Sports branding; the feature slides include genuine current emulator captures.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; Sprints/Sprint_4/PPT/Sprint4_Review.pptx

## Slide 2 — Kellam (~35s)

The core app is still shooting practice: select a drill, log shots on the watch, and review saved progress. The client wants useful logging to feel enjoyable. We split the sprint so Melvin built the iOS/watchOS foundation and gamification parity, Kaleb connected nearby iPhones, and I ported the 1v1 and nearby flows to Android and Wear OS. Each platform now covers the same main capabilities, but this is not a claim of identical behavior or mixed-platform multiplayer.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/MoM/MoM_2026-09-30.md

## Slide 3 — Kellam (~40s)

We had four goals: close the gamification split from Sprint 4, build 1v1 on iPhone and Apple Watch, connect nearby players, and bring the same major flows to Android and Wear OS. Kaleb’s nearby iPhone feature is tested and complete based on Kellam’s current team confirmation. The Android/Wear OS implementation is now merged into main through PR 38, and issue 32 is closed. Android Nearby physical-device validation and client rollout remain separate next steps. The team-confirmed Apple completion is not being presented as a new iOS test run by this assistant.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/27; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/28; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/29; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/30; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32

## Slide 4 — Melvin (~55s)

The base 1v1 feature keeps a list of events instead of changing scores in several places. Replaying that list computes points, possession and the winner, so undo is just removing the last event. Friends and records are saved locally and a paused match survives closing the app. Demo: add a friend, choose the rules, log a basket, a foul and a steal, undo, and save a short finished match. The diagram is illustrative. Possession-based restrictions on who may log which actions are not implemented yet; that is issue 25.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/24; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/28; Squatch Sports Basketball Training WatchOS Companion/OneOnOne/MatchEngine.swift; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25

## Slide 5 — Melvin (~50s)

Streaks and goals were Android-first in Sprint 4, while XP and levels were iOS-first. This sprint brings those rules and celebrations to both ecosystems. On the watch, the user has big scoring buttons, a second page for the opponent, Miss, Foul, Turnover, Undo and Save. The phone remains responsible for applying rules and sends the confirmed scoreboard back. The Apple Watch now requests the active game when it opens or reconnects, fixing a waiting-screen problem found in simulator testing. Steals are still phone-only, and HealthKit-driven auto-launch/keep-alive is future work.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/27; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/29; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/36; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md

## Slide 6 — Kaleb (~50s)

Kaleb built and tested nearby discovery and shared 1v1 between two iPhones using Apple’s MultipeerConnectivity. The same game ID and settings are installed on both phones, but each player has the opposite local side. Plays, undo, finish and cancellation are forwarded without echoing remote actions, and each phone keeps its own paired watch updated. For this revised review, Kaleb’s feature is considered tested and complete based on Kellam’s explicit team confirmation. That confirmation supersedes the feature’s pending-testing wording in the original draft; no new Swift test counts or independent physical-device measurements are being invented. GitHub ticket 31 is still open and is tracked separately for board reconciliation.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/26; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/30; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md

## Slide 7 — Kellam (~95s)

The Android port includes the match engine, durable repository, friends, match setup, the live court, records, undo, save and pause/resume. Wear OS supports your plays and the opponent’s plays, plus Undo and Save. Commands are saved before sending and removed only after acknowledgement; the phone saves the processed IDs with the match so retries cannot add points twice. Demonstrate Dashboard → 1v1 → Add Friend → Play → Start, then score from the watch, undo a play and save. The screenshots are genuine current emulator captures from a temporary demo game. The Android implementation is merged into GitHub main through PR 38 (merge commit c4b0c76), and issue 32 is closed. This is not a claim that every screen on every watch has been validated.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32; android/shared/src/main/java/com/squatchsports/training/shared/MatchEngine.kt; android/shared/src/main/java/com/squatchsports/training/shared/OneOnOneRepository.kt; android/README.md

## Slide 8 — Kellam (~60s)

Android cannot use Apple’s MultipeerConnectivity. I implemented Android-to-Android play with Google Nearby Connections and explicit code confirmation on both phones. The connecting phone is the host. Guest phone and watch commands are queued durably and ordered at that host; confirmed snapshots update the other phone and both watches. Tests simulate duplicates, dropped replies, reconnects and process restarts. Demo only what is verified: show the Nearby Players screen and permission explanation, then use physical phones for an actual discovery/connection demo only after testing it. Cross-platform iPhone-to-Android games require an iOS transport change or shared backend. This sprint does not provide that interoperability.

Sources: android/shared/src/main/java/com/squatchsports/training/shared/NearbyMatchCoordinator.kt; android/app/src/main/java/com/squatchsports/training/nearby/NearbyMatchService.kt; android/README.md#nearby-1v1-two-android-phones

## Slide 9 — Kellam (~50s)

The requested Gradle command succeeds for both debug APKs and Android unit tests. A forced rerun avoids relying only on up-to-date tasks. The XML results contain 121 passing JVM tests across shared and app modules with zero failures/errors. The additional emulator runs contain nine phone and four Wear OS UI tests, all passing. The screenshot is an evidence page generated from the captured Gradle output and test XML, not a fabricated terminal screen. Kaleb’s nearby iPhone feature is now team-confirmed tested and complete; the Android verification did not rerun Swift tests or produce new Apple test counts. The Android code is merged in PR 38. Android Nearby radio validation, latency measurements and client distribution remain separate from these automated results.

Sources: Sprints/Sprint_5/Testing/Android_Verification.md; Sprints/Sprint_5/Testing/Android_Build_Test_Results.txt; Sprints/Sprint_5/Testing/Android_Test_Results.json; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/33; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/34

## Slide 10 — Melvin (~35s)

Melvin owns 18 labeled points across parity, iPhone 1v1 and Apple Watch scoring. Kaleb’s five-point nearby feature is complete, with testing confirmed by Kellam. Kellam’s eight-point Android port is now merged through PR 38, which closed issue 32. These merged/closed statuses have been verified on GitHub. The team confirmation of Kaleb’s testing is distinguished from Android test evidence collected here; no additional Swift test counts are claimed.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/27; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/28; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/29; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/30; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32

## Slide 11 — Melvin (~30s)

Kaleb’s nearby feature is considered complete based on Kellam’s confirmation. GitHub ticket 31 remains open and was not automatically closed by the Android merge, so the table distinguishes team completion from its current ticket state. Remaining team work includes Android hardware evidence, user feedback, client deployment and the video package. Sprint 6 includes possession restrictions, Apple Watch workout keep-alive and duplicate-file cleanup. Apple Watch issue 19 and platform-specific polish remain tracked.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/33; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/34; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/35; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/36; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/37; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/19

## Slide 12 — Kaleb (~35s)

The September 30 meeting set the goals: gamification parity and a new 1v1 mode, built on the Apple platforms first and then ported to Android. At the October 14 design review, the team emphasized keeping the current court design, prioritizing watch input, and connecting each player’s own phone. Online accounts were left for later. The October 28 meeting was cancelled, so it must not be presented as a completed meeting. The minutes are provided on Melvin’s documentation branch.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/MoM/MoM_2026-09-30.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/MoM/MoM_2026-10-14.md

## Slide 13 — Kaleb (~25s)

Melvin prepared the report and meeting minutes on commit 86171e9 of melvin-sprint5-docs. This revised presentation updates that draft with the Android merge in PR 38 and Kellam’s confirmation that Kaleb’s nearby feature is tested and complete. Reconcile any older pending/local-only wording in the report with these statuses. The recording plan and per-slide notes are ready; the actual video URL and player-feedback evidence still depend on the team producing them. This presentation has not been recorded, uploaded as a video or submitted.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/tree/melvin-sprint5-docs/Sprints/Sprint_5/MoM; Sprints/Sprint_5/PPT/README.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/35

## Slide 14 — Melvin (~40s)

AI coding tools helped port features, draft automated tests, diagnose SDK/compiler errors and create this presentation/evidence package. The team chose the rules, design and acceptance criteria. Automated Android checks and paired-emulator smoke provide evidence for the port; they do not prove Android physical radio behavior. Kaleb’s iPhone testing is reported as complete based on Kellam’s team confirmation, rather than being mislabeled as a new assistant-run Swift test session. The team remains accountable for final content, hardware acceptance and release approval.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; Sprints/Sprint_5/Testing/Android_Verification.md

## Slide 15 — Team (~45s)

The main achievement is a complete path from training and gamification into 1v1 competition on phones and watches. Android’s nearby coordinator handles duplicate commands, out-of-order snapshots, reconnects and process restarts. We also encountered an SDK release compiled with newer Kotlin metadata, so the Android project pins a compatible Nearby version instead of bypassing compiler checks. Round watches need careful layout testing; the match controls use paging and scrolling, but this is not a claim that every screen has been polished on every device. Kaleb’s nearby feature is team-confirmed complete. Android physical Nearby testing, client deployment and collected user feedback remain the unfinished validation tasks.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; android/gradle/libs.versions.toml; android/README.md

## Slide 16 — Kellam (~40s)

The Android port is already merged and issue 32 is closed; Kaleb’s nearby feature is considered tested and complete as confirmed by Kellam. Before submission, finalize the report wording, record the demo and add the real unlisted URL. Next, validate Android Nearby on physical phones/watches, collect latency and user feedback, and prepare client distribution. Sprint 6 focuses on possession restrictions, Apple Watch workout keep-alive, cleanup, regression and handoff. Android-to-iPhone interoperability is separate future work.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/33; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/34; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/36; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/37

## Slide 17 — Kellam (~20s)

To recap: Sprint 5 aligns major training and gamification features, adds 1v1 on phones and watches, and implements nearby games separately for Apple and Android. The Android port is merged in PR 38 and its automated checks pass. Kaleb’s nearby feature is team-confirmed tested and complete. Android physical Nearby acceptance and client rollout remain open. Thank you to Mike and Preston at Squatch Sports and to everyone reviewing the project. We welcome questions and feedback.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project
