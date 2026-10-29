# Sprint 5 – 1v1 Match Feature

**Owners:** Melvin Sanare, Kaleb Kebede
**Branch:** `sprint5-1v1-match`

Two friends play 1v1. One phone keeps score for both players: Red vs Blue scoreboard, the training court with both players on it, points, fouls, turnovers, steals, possession, and a win/loss record against each friend.

## What is done (Melvin)

| Area | File | Status |
|---|---|---|
| Match rules: scoring (1s and 2s or 2s and 3s), play to N, win by 2, make it take it, possession, fouls, turnovers, steals, undo | `OneOnOne/MatchEngine.swift` | ✅ |
| Friends, match in progress (survives closing the app), finished matches, win/loss records | `OneOnOne/OneOnOneStore.swift` | ✅ |
| 1v1 home: your record, friends with head-to-head records, resume a paused match, recent games | `OneOnOne/OneOnOneHomeView.swift` | ✅ |
| Match setup: opponent, colors, rules, coin flip for first ball | `OneOnOne/MatchSetupView.swift` | ✅ |
| Match screen: Red/Blue scoreboard, court with player markers and the ball, controls, game-over card | `OneOnOne/MatchView.swift` | ✅ |
| Court drawing shared by the watch and the 1v1 screen | `CourtPosition.swift` (`SimplifiedCourtView`) | ✅ |
| Dashboard "1v1" card | `DashboardHomeView.swift` | ✅ |
| **Apple Watch scorekeeping:** the match appears on the watch when Start is tapped on the iPhone. Page 1 logs your plays (+1/+2 or +2/+3, Miss, Foul, TO), swipe up for your opponent's. Mini scoreboard with possession, Undo, Save from the watch | `ContentView.swift` (watch, `WatchMatchView`) | ✅ |
| Phone ↔ watch sync: the iPhone keeps the match and rules and sends the scoreboard; the watch sends plays (queued if the iPhone is out of range, never counted twice); the watch asks for the match when it opens or reconnects | `SharedWorkout.swift`, `OneOnOne/WatchMatchBridge.swift` | ✅ |
| Tests: 22 unit tests + 2 UI tests that play a full match | `MatchEngineTests.swift`, `OneOnOneStoreTests.swift`, `OneOnOneUITests.swift` | ✅ |

### How the match works

A match is a list of events, for example `.score(.red, points: 2)`, `.foul(.blue)`, `.turnover(.red)`, `.steal(.blue)`, or `.possession(.red)` (ball changes hands after a missed shot). `MatchEngine.state(for:settings:)` replays the list to get the scoreboard, possession, and winner. Undo removes the last event. To add a new kind of play, add a `MatchEvent` case and handle it in `MatchEngine.apply`, then add a test in `MatchEngineTests.swift`.

Possession rules:
* Made basket → the other player gets the ball (or the scorer keeps it with "Make it, take it").
* Foul → the player who was fouled gets the ball.
* Turnover (lost the ball dribbling, out of bounds, travel) → the other player gets the ball.
* Steal → the stealer gets the ball, and it counts as a turnover for the other player.
* Missed shot → tap "Missed shot – ball to …" if the defender ends up with it.

## What is left (Kaleb, plus anything Melvin did not get to)

1. **Foul rules.** Fouls are counted now. Decide with the team whether fouls should do more, for example a foul limit, or a free throw / point for the fouled player after N fouls. Add the rule to `MatchSettings` and `MatchEngine`, and show it on the scoreboard.
2. **Match detail screen.** Tapping a game in "Recent Games" should open a play-by-play list (`match.events`) and a stats comparison (points, fouls, turnovers, steals).
3. **Head-to-head screen per friend.** Tapping a friend could show every game against them and totals.
4. **Keep the watch app open during play.** Today the watch app must be open when the match starts, and watchOS may return to the watch face between plays. Starting an `HKWorkoutSession` (HealthKit) when a match starts would launch the watch app from the iPhone and keep it on screen. It also needs the HealthKit capability added in Xcode.
5. **Steals on the watch.** The watch has Miss, Foul, and TO; steals are logged on the iPhone for now.
6. **Android / Wear OS port.** Port `MatchEngine.swift` and `OneOnOneStore.swift` to the Kotlin `shared` module (same pattern as `Streaks.kt`), with the same tests, then the Compose screens.
7. **Online friends (needs a client decision).** Friends are saved on one phone today. Friending another person's account requires sign-in and a server (for example CloudKit or Firebase). Keep the `OneOnOneStore` API and swap its storage when this is decided.

## How we share the code

1. Both of us branch off `sprint5-1v1-match`, never off `main`:
   ```bash
   git fetch origin
   git switch sprint5-1v1-match && git pull
   git switch -c kaleb-1v1-fouls     # one branch per task
   ```
2. Work on one task per branch and commit small pieces often.
3. Push your branch and open a pull request **into `sprint5-1v1-match`**. The other person reviews it.
4. Before starting new work, pull `sprint5-1v1-match` again to get the other person's merged changes.
5. When the feature is ready for the demo, open one pull request from `sprint5-1v1-match` into `main`.

To avoid merge conflicts, each task should mostly touch its own files. New screens go in new files inside `OneOnOne/` (Xcode adds them to the iPhone app automatically). If two people need the same file, for example `MatchEngine.swift` for new foul rules, say so in the team chat first.

`sprint5-1v1-match` was started from `melvin-sprint4-platform-parity`. Once that pull request is merged into `main`, run `git rebase origin/main` on `sprint5-1v1-match` (or merge `main` into it).

## Testing

* Unit and UI tests: in Xcode, **Product → Test** (⌘U), or run `MatchEngineTests`, `OneOnOneStoreTests`, and `OneOnOneUITests`.
* The UI tests launch the app with `-uiTesting`, which uses empty, separate storage so they never touch real data.
* Manual check: Dashboard → 1v1 → Add Friend → Play → Start, then log a few plays, try Undo, leave and resume, and finish a game.
