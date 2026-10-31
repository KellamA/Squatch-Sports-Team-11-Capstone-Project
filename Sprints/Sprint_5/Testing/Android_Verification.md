# Sprint 5 Android verification

Verified code commit: `c4b0c76` — Android/Wear OS merged into `main` in [PR #38](https://github.com/KellamA/Squatch-Sports-Team-11-Capstone-Project/pull/38). Issue #32 is closed.

## Implementation locations

- Shared rules, persistence and transport: `android/shared/src/main/java/com/squatchsports/training/shared/MatchEngine.kt`, `OneOnOneRepository.kt`, `MatchProtocol.kt`, `NearbyMatchCoordinator.kt` and `NearbyMatchProtocol.kt`.
- Phone store/screens: `android/app/src/main/java/com/squatchsports/training/data/OneOnOneStore.kt`, `ui/OneOnOneScreens.kt`, `ui/NearbyPlayersScreen.kt` and the `nearby` package.
- Wear OS: `android/wear/src/main/java/com/squatchsports/training/presentation/MatchScreen.kt` and the phone/watch connectivity listeners/outboxes.

## Team-confirmed iOS status

Kaleb’s nearby iPhone feature is considered **tested and complete**, per Kellam Adams’s explicit confirmation. This is team-provided status, not a new assistant-run Swift test session or a fabricated test count. GitHub follow-up ticket #31 remains open and was not closed by the Android merge.

## Requested command

```bash
cd android
./gradlew :app:assembleDebug :wear:assembleDebug testDebugUnitTest
```

**Result: BUILD SUCCESSFUL.** A second run added `--rerun-tasks` to force fresh execution. Both debug APKs were produced.

## Automated results

| Suite | Passed | Failures / errors |
|---|---:|---:|
| `ExampleUnitTest` | 1/1 | 0 / 0 |
| `NearbyPermissionsTest` | 5/5 | 0 / 0 |
| `GamificationPayloadsTest` | 6/6 | 0 / 0 |
| `GoalTrackerTest` | 10/10 | 0 / 0 |
| `MatchEngineTest` | 21/21 | 0 / 0 |
| `MatchProtocolTest` | 12/12 | 0 / 0 |
| `ModelsTest` | 3/3 | 0 / 0 |
| `NearbyMatchCoordinatorTest` | 19/19 | 0 / 0 |
| `NearbyMatchPayloadsTest` | 8/8 | 0 / 0 |
| `OneOnOneRepositoryTest` | 15/15 | 0 / 0 |
| `StreakCalculatorTest` | 17/17 | 0 / 0 |
| `XpCalculatorTest` | 4/4 | 0 / 0 |

**JVM total: 121/121 passed.**

| Emulator UI run | Passed | Failures / errors |
|---|---:|---:|
| `app` | 9/9 | 0 / 0 |
| `wear` | 4/4 | 0 / 0 |

**Emulator total: 13/13 passed (9 phone, 4 watch).** These results are from a clean uninterrupted rerun. An earlier exploratory UI run was interrupted by concurrent manual emulator inspection and is not the reported acceptance run.

## Paired phone/watch emulator smoke

Both emulators reported `Peer:[...,true,true]` after restoring their existing transport bridge. This was tested in the real apps, separately from the stateless watch UI tests.

| Action | Observed result |
|---|---|
| Start match on phone | Phone and watch show Kellam 0 / Kaleb 0. |
| Watch: Kellam +2 | Both phone and watch show 2–0. |
| Phone: Kaleb +2 | Both phone and watch show 2–2. |
| Watch: Kellam +2 | Both show 4–2; presentation screenshots captured. |
| Scroll watch and Undo last play | Both return to 2–2; confirms Undo is reachable by scrolling. |
| Phone: End without saving | Phone returns to 1v1 hub with no result; watch clears the match and shows training stats. |

The screenshot match is a temporary demo fixture, not player/user feedback. It was discarded without saving a win/loss result. The round watch uses a vertical scroll view; the second watch screenshot shows controls after scrolling.

## Evidence files

- [Results screenshot](Android_Test_Results.png)
- [Results page](Android_Test_Results.html) — renders the actual logs/XML; not a terminal screenshot.
- [Build and fresh unit-test logs](Android_Build_Test_Results.txt)
- [Clean emulator UI-test logs](Android_Emulator_Test_Results.txt)
- [JSON receipt, suites, APK hashes and limits](Android_Test_Results.json)
- App captures: [`../PPT/Assets/`](../PPT/Assets/)

## Remaining validation / submission work

- Nearby Bluetooth/Wi-Fi discovery/connection needs two physical Android phones.
- Android-to-iPhone interoperability is not implemented.
- Kaleb’s nearby iPhone feature is team-confirmed tested and complete; no new assistant-run Swift test counts or measurements are claimed.
- Client TestFlight / Play internal distribution and player user testing are still pending.
- Android/Wear OS 1v1 is merged through PR #38 (merge commit `c4b0c76`). GitHub issue #32 is closed; this is verified publication status, not a claim of a production release.
- The report draft on `melvin-sprint5-docs` (`86171e9`) predates this update. Reconcile its old pending/local-only wording with the actual Android merge and Kellam’s confirmation that Kaleb’s nearby feature is tested and complete.
- Issue #32 has **8 story points**, as labeled on GitHub, and is now closed by the Android merge.

## Suggested message to Melvin

> Android/Wear OS 1v1 and nearby Android-to-Android play are merged into main in PR #38; issue #32 is closed. Both APKs build; 121 unit tests and the last clean 13 emulator UI tests pass. Paired-emulator scoring in both directions, Undo and cancel/clear also passed. Kaleb’s nearby iPhone feature is considered tested and complete per Kellam’s confirmation. Screenshot: `Sprints/Sprint_5/Testing/Android_Test_Results.png`. Deck: `Sprints/Sprint_5/PPT/Sprint5_Review.pptx`. Remaining: Android physical Nearby QA, client rollout and the recording; no Android–iPhone cross-play.
