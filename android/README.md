# Squatch Sports for Android

This Android Studio project is the Kotlin/Jetpack Compose port of the iOS and watchOS Squatch Sports apps.

## Modules

- `app` — Android phone app (API 26+)
- `wear` — Wear OS companion app (API 30+)
- `shared` — workout models, court positions, shot codes, and phone/watch message protocol

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

## Troubleshooting phone/watch emulator connections

Run `app` on the phone and `wear` on the watch. Keep **both apps open before starting
a workout**: the current implementation listens for messages while each activity
is started and does not replay a missed workout-start message. A watch showing
“Waiting for workout” is not proof that the devices are connected.

### Google's watch companion app crashes on the phone

If Logcat reports `com.google.android.apps.wear.companion` crashing with
`SecurityException` / `android.permission.BLUETOOTH_CONNECT`, enable **Nearby
devices** in that companion app's Android permission settings. This is not a
Squatch crash and does not require adding Bluetooth permissions to Squatch.

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
```
