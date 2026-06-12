# Current Kotlin Rewrite Audit

Date: 2026-06-09

## Fully Built Locally

- Native Android/Kotlin project builds as a debug APK.
- Core interview flow: splash, landing, auth entry, home, questions, room setup, interview, answer capture, scoring, results, and progress history.
- Supabase/backend config structure with email/password sign-in, sign-up, and password reset.
- Backend session creation, answer scoring, session listing/deletion, resume profile upload/text/delete, notifications mark-read/delete, feedback submission, and account deletion endpoints.
- Duix avatar wrapper with remote model download/cache and local TTS audio converted to 16 kHz mono WAV for playback.
- Duix speech callbacks track the current audio source and clear it after playback end/error.
- Camera coach uses CameraX compatible preview mode plus MediaPipe face and pose landmarkers with rotated/mirrored frame analysis.
- Camera permission denial no longer disables the coach preference; the app can answer without coach and keeps the setting recoverable.
- Local scoring penalizes skipped/off-topic answers and readiness is weighted by full interview completion.
- Native parity screens include sign-up, reset password, language selection, account deletion, feedback, Google Play subscription management, device QA, Visual parity route map, privacy, and terms.
- Native whisper.cpp transcription path is now present: the app builds a standalone `prezzence_whisper` JNI library from local whisper.cpp/ggml sources, records 16 kHz mono PCM with `AudioRecord`, downloads/caches the multilingual base `ggml-base.bin` model, and transcribes through JNI when `PREZZENCE_ENABLE_WHISPER_CPP=true`.
- Answer finishing runs native transcription on a background dispatcher so the UI can show the transcribing state while whisper.cpp works.
- Play replacement release verification checks app id, backend URL, Supabase config, signing inputs, subscription product id, native whisper.cpp enablement, multilingual model selection, model URL, and JNI/source presence.

## Still Not 100% Done

- Signed Play Store AAB cannot be proven without the real release keystore and production Supabase env values.
- Multi-device QA still needs physical Android devices, but the Kotlin app now has a Device QA screen that runs microphone PCM capture, first-run Whisper model download, native Whisper JNI load, CameraX provider open, and Duix endpoint checks on-device.
- Multi-device QA still needs physical Android devices, especially low-end/older phones and devices with camera/audio quirks.
- Google Play Billing purchase, restore, acknowledgement, and entitlement persistence are wired; Play Console must contain the configured `PREZZENCE_SUBSCRIPTION_PRODUCT_ID` product/base plan for live purchase testing.
- Visual parity route coverage is mapped in-app and in `VISUAL_PARITY_AUDIT.md`; pixel-level proof still requires screenshot review on Android devices against the React Native routes.
- Native whisper.cpp now builds and is wired; live multilingual quality still needs the Device QA run and real spoken-answer tests on target devices.

## Current Build Output

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Verified builds:
  - `.\gradlew.bat :app:assembleDebug --no-daemon`
  - `PREZZENCE_ENABLE_WHISPER_CPP=true .\gradlew.bat :app:assembleDebug --no-daemon`

## Release Gate

A Play replacement build must pass:

```powershell
$env:PREZZENCE_PLAY_APPLICATION_ID='com.pollecode.prezzence'
$env:PREZZENCE_SUPABASE_URL='<production supabase url>'
$env:PREZZENCE_SUPABASE_ANON_KEY='<production anon key>'
$env:PREZZENCE_UPLOAD_STORE_FILE='<release keystore>'
$env:PREZZENCE_UPLOAD_STORE_PASSWORD='<password>'
$env:PREZZENCE_UPLOAD_KEY_ALIAS='<alias>'
$env:PREZZENCE_UPLOAD_KEY_PASSWORD='<password>'
$env:PREZZENCE_ENABLE_WHISPER_CPP='true'
.\gradlew.bat verifyPrezzenceReleaseEnv :app:bundleRelease --no-daemon
```

The gate now passes the native whisper source/JNI and subscription product-id checks. It intentionally still fails until the real Play package id, production Supabase values, and release keystore credentials are supplied.