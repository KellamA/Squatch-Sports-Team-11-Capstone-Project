# Sprint 5 demo video / recording plan

## Video link

**Pending team recording and unlisted upload.** Insert the actual YouTube URL here and in the Sprint 5 report after upload. Repository merges do not imply that a video has been recorded or submitted.

Present from [Sprint5_Review.pptx](../PPT/Sprint5_Review.pptx). All 17 slides contain speaker notes; a text copy is in [Sprint5_Presenter_Notes.md](../PPT/Sprint5_Presenter_Notes.md).

## Current completion status

- Kellam's Android/Wear OS 1v1 and nearby implementation is **merged into `main` through [PR #38](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/38)**. Issue #32 is closed.
- Kaleb's nearby iPhone feature is **tested and complete**, per Kellam's team confirmation. Use the team's verified iPhone demo; do not invent new Swift test counts or say the Android verification reran those tests.
- GitHub follow-up #31 remains open and needs separate board reconciliation; this does not change the team-confirmed feature status used in the review.
- Android physical Nearby testing, client rollout and collected player feedback remain follow-ups. Mixed Android–iPhone play is not supported.

## Suggested run of show (~12–14 minutes)

These are planning estimates, not an existing recording. Allow extra time when switching to live demos.

| Template section | Slides | Suggested presenter | Show / explain |
|---|---|---|---|
| Intro | 1–2 | Kellam | Project, team, client goal and responsibilities |
| Objectives | 3 | Kellam | Parity, Apple 1v1, Kaleb's completed nearby work and the merged Android port |
| Features / Demo | 4–5 | Melvin | iPhone rules, friends/records, watch input and gamification parity |
| Features / Demo | 6 | Kaleb | Team-tested nearby iPhone match and paired-watch updates |
| Features / Demo | 7–8 | Kellam | Android/Wear OS demo, Nearby screen and host/guest recovery |
| Deployment & Testing | 9 | Kellam | 121 JVM checks, last clean 13 emulator checks, merge proof and remaining Android hardware testing |
| Kanban | 10–11 | Melvin | Closed #32 / merged #38, team-confirmed Kaleb completion and separate open tickets |
| Client Meetings | 12 | Kaleb | Sep 30 and Oct 14 minutes; Oct 28 was cancelled |
| Report | 13 | Kaleb | Report, MoMs, evidence and real video-link status |
| AI | 14 | Melvin | Assistance, review and accountability; distinguish team confirmations from assistant-run tests |
| Achievements & Challenges | 15 | Team | Delivered features, ordering/recovery, toolchain and round-watch constraints |
| Next Steps | 16 | Kellam | Android physical testing, rollout, recording and Sprint 6 backlog |
| Conclusion | 17 | Kellam | Recap, acknowledge Squatch Sports, invite feedback |

## Android / Wear OS live demo (slide 7)

Before recording:
- Pull the merged `main`, build the debug phone/watch apps, and open both in paired Pixel and Wear OS emulators.
- Confirm pairing; a waiting screen alone does not prove connectivity. See [`android/README.md`](../../../android/README.md#troubleshooting-phonewatch-emulator-connections).
- Use clearly labeled demo data, not a player's production history or fabricated user feedback.

Demo:
1. Phone: **Dashboard → 1v1**. Edit your display name, add a demo opponent and tap **Play**.
2. Show setup choices. Use 1s/2s, target 5 and your color Red for a short demo. Start the match.
3. Watch: tap your **+2** and show the phone changing to 2–0.
4. Phone: log the opponent's **+2** and show the watch changing to 2–2.
5. Watch: log another **+2**, then scroll to **Undo last play**. Both devices return from 4–2 to 2–2.
6. Show vertical scrolling for additional watch controls and switching to the opponent's page. Do not claim every control fits simultaneously on a round screen.
7. Finish a short match, save the result and show the updated record/recent game. Rehearse the exact paired-device sequence before recording.

The captured real-app paired-emulator smoke verified the scoring directions and Undo above, plus **End without saving**, which returned the watch to its stats/waiting screen without creating a result. The screenshots show the earlier 4–2 fixture state.

## Nearby segments

### Kaleb — iPhone (slide 6)

Present the nearby flow as **tested and complete**, as confirmed by the team: discover/connect, share one match ID and opposite player sides, log plays, undo, finish/cancel and keep both paired watches updated. Use Kaleb's verified demonstration or recording. Do not attribute that verification to the Android test command or invent additional test-run metrics.

### Kellam — Android (slide 8)

- Show **1v1 → Nearby Players**, the permission explanation, code comparison and host/guest design.
- Physical discovery/connection still needs **two real Android phones** running the updated app, with Google Play services, Bluetooth/Wi-Fi and requested permissions. Older Android versions also require Location enabled.
- One phone taps **Connect**; compare the code on both devices before accepting. The connecting phone hosts and starts the game.
- Until physical Android testing is collected, demonstrate the screen/architecture and clearly identify that radio acceptance as pending. A serialized two-peer unit test is not a physical connection demo.
- No Android–iPhone shared game: iOS uses MultipeerConnectivity, while Android uses Google Nearby Connections.

## Evidence for slide 9

- [Build/test screenshot](../Testing/Android_Test_Results.png)
- [Raw Gradle output](../Testing/Android_Build_Test_Results.txt)
- [Last clean emulator output](../Testing/Android_Emulator_Test_Results.txt)
- [Verification, merge status and handoff message](../Testing/Android_Verification.md)

The requested build/test command passed on the merged Android code. A forced `--rerun-tasks` verification provides fresh JVM execution. Both APKs build and **121 JVM tests pass**. The last clean emulator run has **13 passing UI checks** (9 phone, 4 watch); its actual timestamps are preserved.

## Final accuracy check

- Show #32 as closed and the Android code as merged through PR #38, not merely local or awaiting push.
- Show Kaleb's nearby feature as team-confirmed tested and done; keep that attribution distinct from new automated evidence.
- Ticket #31 is still open on GitHub unless separately reconciled. Do not label that ticket closed without changing/verifying its actual state.
- Do not claim Android physical Nearby acceptance, a client release, user-feedback results or a video upload that has not occurred.
- Reconcile old wording in the report draft and add the real unlisted URL only after recording/upload.
