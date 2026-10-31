# Squatch Sports for Android

This Android Studio project is the Kotlin/Jetpack Compose port of the iOS and watchOS Squatch Sports apps.

## Modules

- `app` — Android phone app (API 26+)
- `wear` — Wear OS companion app (API 30+)
- `shared` — workout/gamification models, 1v1 rules and persistence, court positions, and phone/watch protocols

Both application modules use the application ID `com.squatchsports.training` so the Wear OS Data Layer can authenticate communication between them.

## Run

1. Open this `android` directory in Android Studio.
2. Allow Gradle sync to finish.
3. Create or select a phone emulator and run the `app` configuration.
4. Create a paired Wear OS emulator and run the `wear` configuration.
5. Open both apps. Start a workout on the phone, then log shots by swiping the center pad on the watch:
   - up — make
   - down — miss
   - right — swish

The phone workout screen also includes the same Quick Log controls as the iOS prototype for testing without watch gestures.

## 1v1 matches

1. Open **Dashboard → 1v1 → Add Friend**, enter a name, then tap **Play**.
2. Pick your color, scoring system, target, win-by-two, make-it-take-it, and first possession. Tap **Start Match**.
3. Log points, fouls, turnovers, steals, and possession changes on the phone. Undo reverses the last play, including a winning basket.
4. On Wear OS, swipe horizontally (or tap the player-switch button) between your plays and your opponent's. Each page has scoring, Miss, Foul, TO, and Undo controls. Save a finished match from either device.
5. **Leave Match → Pause and leave** keeps the game for later. Resume from the hub. **End without saving** discards it. Saved results update overall and per-friend records and recent games.

The phone is authoritative. Friends, settings, active events, and completed games survive process restarts. Watch commands are saved before sending, retried in order, and removed only after an application acknowledgement; processed IDs are saved atomically with the match to prevent duplicate scoring after a restart. A `WearableListenerService` handles match batches even when the phone activity is closed. State revisions prevent late snapshots from resurrecting an older scoreboard. The watch keeps the screen awake during foreground match play, but does not automatically launch or record a fitness workout.

Friends are local names, not online accounts. As on iOS, detailed game/head-to-head screens, additional foul penalties, and watch steal controls are not implemented yet. Regular **workout shots** still use the older foreground-only message protocol described below; the durable 1v1 queue does not change that protocol.

## Nearby 1v1 (two Android phones)

1. On **both phones**, open **Dashboard → 1v1 → Nearby Players → Find Nearby Players**. Allow the requested permissions. Enable Bluetooth and Wi-Fi; Android 12L and older also require Location to be enabled. Google Play services must be installed and up to date; an internet connection is not required for play.
2. On **one phone only**, tap **Connect** next to your opponent. Compare the displayed connection code on both phones and tap **Codes Match — Accept** only if the codes match. Reject unexpected requests. The phone that requested the connection hosts the game.
3. The host chooses colors, scoring, target, win-by-two, make-it-take-it and first possession, then taps **Start Nearby 1v1**. Both phones open the same match, with opposite player perspectives.
4. Log plays on either phone or either paired Wear OS watch. Scoring, possession, fouls, turnovers, steals, undo, save and cancellation synchronize. A saved result counts as a win for one player and a loss for the other.
5. If disconnected, reopen Nearby Players and reconnect to the **same opponent's phone**. Guest plays remain queued (the guest scoreboard waits for host confirmation); host plays and results are retained. **Pause / reconnect** is available on the game-over card too. Guest cancellation/save requests require host acknowledgement; they are not silently treated as completed while offline. If the host is permanently unavailable, **Cannot reconnect? Abandon local copy** offers a separate confirmation to discard only this phone's game/queued plays; it does not cancel the opponent's game or save a result.
6. **Disconnect** closes the peer connection without deleting the saved match. Searching stops when leaving the discovery screen or backgrounding the app; an established connection is retained while the process is alive. Keep both phone apps open during nearby play. Reconnect after a process restart.

Implementation: Google Nearby Connections, encrypted point-to-point transport with explicit code confirmation. The shared `NearbyMatchCoordinator` serializes guest commands at the host, persists deduplication IDs/outboxes/session roles, rejects older snapshots, and imports the host event log without echoing it. Complete snapshots recover missed plays and undo operations after reconnecting. Long logs use bounded stream payloads rather than exceeding Nearby's 32 KiB byte-payload limit. Both phones continue publishing the confirmed scoreboard to their watches.

**Compatibility:** Android-to-Android only. The iOS implementation uses Apple's MultipeerConnectivity, which cannot communicate with this transport. Cross-platform play requires migrating iOS to the same transport/protocol (or adding a shared backend); this change does not modify iOS. Friends/results are still stored locally, without accounts or cloud sync.

Nearby permissions are requested only when the user searches: Bluetooth on Android 12+, Nearby Wi-Fi on Android 13+, local network access on Android 17+, and location on older Android versions. There is no file/media permission or automatic peer acceptance. See [Google's permission requirements](https://developers.google.com/nearby/connections/android/get-started).

**Release check:** use two physical Android phones (and paired watches if available). Verify code rejection/acceptance, a full game from both phones/watches, undo, disconnect/reconnect with queued plays, a process restart, save and cancellation. Emulator screen tests and in-memory two-peer protocol tests do not validate physical Bluetooth/Wi-Fi discovery or radio transport.

## Troubleshooting phone/watch emulator connections

Run `app` on the phone and `wear` on the watch. Keep **both apps open before starting
a workout**: the current implementation listens for messages while each activity
is started and does not replay a missed workout-start message. A watch showing
“Waiting for workout” is not proof that the devices are connected.

### Google's watch companion app crashes on the phone

If Logcat reports `com.google.android.apps.wear.companion` crashing with
`SecurityException` / `android.permission.BLUETOOTH_CONNECT`, enable **Nearby
devices** in that companion app's Android permission settings. This is not a
Squatch crash. Grant the permission to the **watch companion app** itself; Squatch's separate Nearby 1v1 permissions do not fix that companion-app crash.

For the local Mac emulator setup, the equivalent commands are below. First use
`adb devices -l` to confirm the phone's serial; emulator serials can change after
a restart.

```bash
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
"$ADB" devices -l
PHONE=emulator-5554 # Replace with the phone emulator's current serial.
"$ADB" -s "$PHONE" shell pm grant com.google.android.apps.wear.companion android.permission.BLUETOOTH_CONNECT
"$ADB" -s "$PHONE" shell pm grant com.google.android.apps.wear.companion android.permission.BLUETOOTH_SCAN
```

Reopen the companion app after granting these permissions.

### Previously paired emulators are disconnected

Use Android Studio's **Device Manager → Pair Wearable** to pair/reconnect the
phone and watch. If an existing pair remains disconnected after an emulator or
ADB restart, restore the local ADB transport bridge:

```bash
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
"$ADB" devices -l
PHONE=emulator-5554 # Confirm this is the phone, not the watch.
WATCH=emulator-5556 # Confirm this is the watch.
PORT=$("$ADB" -s "$PHONE" forward tcp:0 tcp:5601)
"$ADB" -s "$WATCH" reverse tcp:5601 "tcp:$PORT"
"$ADB" -s "$WATCH" shell am broadcast -a com.google.android.gms.wearable.EMULATOR --es operation refresh-emulator-connection

# Both devices should report their peer with true,true.
"$ADB" -s "$PHONE" shell am broadcast -a com.google.android.gms.wearable.EMULATOR --es operation get-pairing-status
"$ADB" -s "$WATCH" shell am broadcast -a com.google.android.gms.wearable.EMULATOR --es operation get-pairing-status
```

This bridge is temporary, not a replacement for initial pairing. If it is lost,
reconnect with Android Studio or repeat the bridge commands. To verify Squatch,
open both apps, start a workout on the phone, and swipe up on the watch's center
pad: the phone's make count should increase. A phone Quick Log miss should also
increase the watch's attempt count.

## Verification

From this directory:

```bash
./gradlew :app:assembleDebug :wear:assembleDebug testDebugUnitTest lintDebug

# Select each emulator/device with its adb serial:
ANDROID_SERIAL=<phone-serial> ./gradlew :app:connectedDebugAndroidTest
ANDROID_SERIAL=<watch-serial> ./gradlew :wear:connectedDebugAndroidTest
```

1v1 unit coverage includes both scoring systems, possession, win-by-two, game point, undo/reopening, records, persistence, invalid payloads, stale-match rejection, and offline FIFO delivery/deduplication across process restarts. Phone Compose tests play complete matches with both color/scoring choices and exercise pause/resume, using isolated match storage. Wear OS Compose tests cover both players' controls, paging, offline status, and saving a finished game without sending real Data Layer traffic. Nearby tests additionally cover two serialized peers, host ordering, watch-command forwarding, duplicate/lost messages, reconnect/restart recovery, opposite win/loss records, cancellation, permission policy, and explicit connection confirmation. Paired-device/physical-device transport should also be checked before release.
