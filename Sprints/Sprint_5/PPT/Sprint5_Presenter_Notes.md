# Sprint 5 presenter notes

Minimal 15-slide review. The separate Sprint 5 closeout slide is removed; remaining Sprint 5 tasks stay visible in Next Steps, and the Sprint 6 backlog stays visible in Kanban.

Required order: Intro → Objectives → Features/Demo → Deployment & Testing → Kanban → Client Meetings → Report → AI → Achievements & Challenges → Next Steps → Conclusion

## Slide 1 — Intro · Kellam (~45s)

We are Team 11: Kellam, Melvin and Kaleb. Squatch Sports helps basketball players choose drills on a phone, log makes, misses and swishes on a watch, and review their progress. The client calls the motivation goal “hide the broccoli”: make useful training habits enjoyable. Sprint 5 adds friendly 1v1 competition and aligns the major features across Apple and Android. Melvin handled parity and the iPhone/Apple Watch foundation, Kaleb built nearby iPhone games, and Kellam ported the feature to Android and Wear OS.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md

## Slide 2 — Objectives · Kellam (~35s)

Our goals were gamification parity, phone/watch 1v1, nearby competition and the Android/Wear OS port. The Android work is merged into main through PR 38 and issue 32 is closed. Kaleb’s nearby iPhone feature is considered tested and complete based on Kellam’s team confirmation. These are completed implementation goals; Android Nearby hardware acceptance and client rollout remain separate next steps. The two ecosystems still do not play one mixed Android–iPhone match.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/38; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32

## Slide 3 — Features/Demo · Melvin (~55s)

The iPhone 1v1 mode lets users add friends, select Red or Blue, choose 1s/2s or 2s/3s, target score, win by two, make-it-take-it and first possession. It tracks baskets, fouls, turnovers, steals and ball possession. A match is an event log, so replay computes the state and undo removes the last event. A paused match survives closing the app; a saved finished match updates overall and head-to-head records. Possession-based restrictions on which player may log actions are planned in issue 25. The visual is a diagram, not an iOS device screenshot.

**Demo cue:** Show setup, log a basket and foul, undo, then finish/save a short game.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/24; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/28; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25

## Slide 4 — Features/Demo · Melvin (~50s)

Gamification was split by platform in Sprint 4: Android had streaks/goals and iOS had XP. Sprint 5 brings the same major gamification rules and celebrations to both platforms. The watch has big scoring buttons, your/opponent pages, Miss, Foul, Turnover, Undo and Save. The phone applies the official rules and sends back the scoreboard. Apple Watch requests the active game on open or reconnect, fixing the waiting-screen issue found in simulator testing. Steals remain phone-only. Apple Watch workout-driven keep-alive is Sprint 6 work assigned to Kellam in this presentation.

**Demo cue:** Show the paired Apple Watch score, log a play and show the phone update; briefly show the gamification progress.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/27; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/29; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/36; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md

## Slide 5 — Features/Demo · Kaleb (~55s)

Nearby iPhone play uses Apple MultipeerConnectivity. Both phones share a match ID and settings, but use opposite local player sides. Scoring, fouls, turnovers, steals, possession, undo, finish and cancel sync without echoing remote events. Each phone continues updating its own paired Apple Watch. The feature is tested and complete per Kellam’s current team confirmation; this is not a new assistant-run iOS test session. Follow-up ticket 31 is still open on GitHub and needs board reconciliation separately from the confirmed feature status. No new Swift test counts or latency measurements are claimed.

**Demo cue:** Use Kaleb’s team-tested nearby iPhone demonstration: connect, start, log plays on both phones and show paired-watch updates.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/26; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/30; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md

## Slide 6 — Features/Demo · Kellam (~65s)

The Android/Wear OS port is merged in PR 38 and issue 32 is closed. Shared Kotlin code contains the rules, persistence, records and event replay, while Compose supplies friends/setup/court and watch controls. Wear OS commands are saved before sending, and persisted IDs prevent duplicate scoring. Nearby Android play uses encrypted Google Nearby Connections with code confirmation on both phones. The connecting phone hosts; guest phone/watch commands queue until the host acknowledges them. Tests cover duplicate/dropped replies, stale snapshots, reconnects and restarts. The screenshots are genuine current emulator captures of a temporary 4–2 fixture, later discarded without saving a record. The round watch is scrollable; not all controls fit at once. Android Nearby radio validation on physical phones remains pending. There is no Android-to-iPhone interoperability.

**Demo cue:** Phone: Dashboard → 1v1 → Add Friend → Play → Start. Watch +2; phone opponent +2; watch +2 then Undo; finish/save a short game. Briefly show Nearby Players and its permission/code-confirmation flow. Allow 75–90 seconds for the live segment.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/38; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32; android/README.md; Sprints/Sprint_5/Testing/Android_Verification.md

## Slide 7 — Deployment & Testing · Kellam (~55s)

The requested Gradle command, ./gradlew :app:assembleDebug :wear:assembleDebug testDebugUnitTest, passed on merged Android code and was repeated with --rerun-tasks for fresh execution. JUnit XML reports 121 passing JVM tests with no failures/errors. The last clean UI run had nine phone and four Wear OS tests passing; its actual timestamps are retained. Both debug APKs were produced. In the actual paired emulator apps, watch-to-phone scoring, phone-to-watch scoring, watch Undo and cancellation/clearing also worked. These results do not prove physical Android Nearby radio behavior or real-device latency. Client distribution/accounts and collected player feedback remain follow-ups. Kaleb’s iPhone status is team-confirmed rather than derived from these Android results. The report-ready screenshot and raw logs are linked in the notes.

Sources: Sprints/Sprint_5/Testing/Android_Test_Results.png; Sprints/Sprint_5/Testing/Android_Build_Test_Results.txt; Sprints/Sprint_5/Testing/Android_Test_Results.json; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/33; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/34

## Slide 8 — Kanban · Melvin (~35s)

All five listed implementation tickets are closed. Melvin owns the parity, iPhone 1v1 and Apple Watch implementation work: 5 + 8 + 5 story points. Kaleb’s nearby implementation is five points and is tested/complete per team confirmation. Kellam’s Android port is eight points, merged through PR 38 which closed issue 32. Pull request IDs and full acceptance details stay in these notes instead of cluttering the table.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/27; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/28; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/29; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/30; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/32

## Slide 9 — Kanban · Melvin (~35s)

The upcoming work stays visible: player/possession restrictions, Apple Watch workout keep-alive, duplicate-file cleanup and the older repeated-shot bug. Kellam owns issues 36 and 37 in this presentation as requested; the slide edit does not change GitHub assignee fields. Android/Wear OS UI, regression polish and the remaining edge cases also stay tracked under issue 20. The remaining Sprint 5 work is visible in the required Next Steps section; a separate closeout slide is not needed.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/36; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/37; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/19; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/20

## Slide 10 — Client Meetings · Kaleb (~35s)

The September 30 Sprint 4 demo/planning meeting set parity and 1v1 as the next goals, with Apple implementation first and Android/Wear OS porting afterward. The October 14 review emphasized wrist input, preserving the existing court, and letting each player use their own phone. Online accounts were left for later. October 28 was cancelled and is not counted as a held meeting. Melvin’s documentation branch contains both minutes.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/MoM/MoM_2026-09-30.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/MoM/MoM_2026-10-14.md

## Slide 11 — Report · Kaleb (~30s)

The report and meeting minutes were sourced from Melvin’s documentation branch, commit 86171e9. That draft predates the Android merge and latest team confirmation, so reconcile its old pending/local-only wording before submission. The Android verification package includes the screenshot, actual Gradle logs, JSON summaries and APK hashes. The presentation and speaker scripts are ready. The recording and actual unlisted video URL still need to be produced; no course submission, upload or client release is being claimed.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/tree/melvin-sprint5-docs/Sprints/Sprint_5/MoM; Sprints/Sprint_5/Testing/Android_Verification.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/35

## Slide 12 — AI · Melvin (~45s)

AI coding tools accelerated Swift-to-Kotlin porting, feature implementation, tests and diagnosis of emulator/toolchain issues. AI also helped organize this deck and the evidence package. Humans chose the gameplay rules, team split, watch UX and acceptance standards. A build or generated test alone cannot prove real-device radio behavior; the team reviews code and tests it in context. Kaleb’s iPhone testing is team-provided confirmation, distinct from the Android tests reproduced here. We remain responsible for the result and for completing the outstanding hardware and release checks.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; Sprints/Sprint_5/Testing/Android_Verification.md

## Slide 13 — Achievements & Challenges · Team (~45s)

The achievement is a full path from training and rewards into phone/watch 1v1 and nearby competition within each platform. Ordering guest commands at one host, saving IDs and replaying event logs handle duplicate and delayed plays. The Android SDK mismatch was solved by pinning a Nearby release compatible with the project’s Kotlin compiler rather than bypassing checks. Round watches require large controls plus scrolling/paging; we are not claiming that every screen is validated on every device. Kaleb’s feature is complete per team confirmation. Android physical Nearby checks, latency, client distribution and collected user feedback remain the key unfinished acceptance work.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; android/README.md; android/gradle/libs.versions.toml

## Slide 14 — Next Steps · Kellam (~40s)

The implementation is already merged, so the next work is acceptance and handoff rather than another Android publishing task. Issue 33 covers system/integration checks, latency, reconnect behavior and actual player feedback. Issue 34 covers physical-device checks and client TestFlight/Play internal distribution. Test Android Nearby with two real phones and paired watches. Issue 35 includes finishing the report, meeting minutes and slides, recording the review and adding the actual unlisted video URL; no upload is claimed. Kaleb’s nearby feature is tested and complete per team confirmation; issue 31 is board reconciliation, not unfinished feature work. Sprint 6 ownership stays visible in Kanban: Kaleb handles possession restrictions, and Kellam handles Apple Watch keep-alive plus repository cleanup. Mixed Android–iPhone games require a shared transport or backend and are not delivered here.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/33; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/34; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/35; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/31; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/25; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/36; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/issues/37

## Slide 15 — Conclusion · Kellam (~20s)

Sprint 5 aligns the major feature set, adds 1v1 on phones and watches, and enables nearby games within each platform ecosystem. The Android port is merged; Kaleb’s nearby feature is tested and complete per team confirmation. Android hardware acceptance and client rollout are next. Thank you to Mike and Preston at Squatch Sports. We welcome questions and feedback.

Sources: https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/blob/melvin-sprint5-docs/Sprints/Sprint_5/Sprint_Report/Sprint5_Report.md; https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project
