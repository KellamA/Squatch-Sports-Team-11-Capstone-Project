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

## Verification

From this directory:

```bash
./gradlew :app:assembleDebug :wear:assembleDebug testDebugUnitTest lintDebug
```
