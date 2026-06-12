# Prezzence Kotlin Port Status

This project is a native Android/Kotlin port of Prezzence. It is now structured as a production-candidate native port, but it must not replace the React Native Play Store app until release signing, production env values, and real-device QA are complete.

## Implemented

- Native Kotlin screen flow for landing, home, questions, settings, room setup, interview, answer result, history, notifications, subscription, device QA, and visual parity route mapping.
- Native Duix avatar wrapper with remote model download/cache and local playback.
- Native TextToSpeech file generation for Duix audio input.
- Native whisper.cpp transcription path for release builds: standalone JNI library, local whisper.cpp/ggml source tree, 16 kHz PCM capture, first-run multilingual `ggml-base.bin` model download/cache, and background transcription when `PREZZENCE_ENABLE_WHISPER_CPP=true`.
- Android `SpeechRecognizer` remains available only as the default debug fallback when native whisper is not enabled.
- Local answer scoring fallback plus backend answer scoring when signed in.
- Camera presence coach using CameraX plus MediaPipe FaceLandmarker and PoseLandmarker for on-device face, eye, head, posture, and expression-energy signals.
- Local persisted session state.
- Local progress, session history, and session delete flow.
- Supabase email/password sign-in, sign-up, and password reset paths.
- Backend session creation, answer scoring, session history listing, feedback submission, account deletion, resume profile, notifications, and delete sync when a bearer token is available.
- Configurable Play Store applicationId, versionCode, versionName, backend URL, Supabase URL/key, release signing inputs, multilingual whisper model name/URL, and subscription product id.
- `verifyPrezzenceReleaseEnv` Gradle task for checking production release environment before building a Play replacement artifact.

## Kotlin Parity Pass Added

- Native screens now cover sign-up, password reset, language selection, account deletion, feedback, Google Play subscription management, device QA, privacy, and terms surfaces.
- Interview pause/repeat/clarify controls are wired instead of inert buttons.
- Camera permission denial no longer disables the camera coach preference; it answers without coach and keeps the setting recoverable.
- Release verification blocks a claimed 100% Play replacement unless `PREZZENCE_ENABLE_WHISPER_CPP=true`, native whisper.cpp sources/JNI exist, and a subscription product id is configured.

## Remaining Before Shipping As 100% Replacement

- Provide production Supabase URL/key and the real Play release keystore values.
- Build and verify a signed release AAB with `PREZZENCE_PLAY_APPLICATION_ID=com.pollecode.prezzence`.
- Run the in-app Device QA screen on physical Android devices for microphone capture, first-run whisper model download, native JNI load, CameraX provider access, Duix reachability, and older/low-end device performance.
- Verify Google Play Billing against the real Play Console subscription product/base plan and licensed tester accounts.
- Use `Settings > Visual parity` and `VISUAL_PARITY_AUDIT.md` for screenshot-based review against the React Native routes.

## Play Store Release Notes

For a Play Store replacement build, use:

```powershell
cd C:\Users\LENOVO\Documents\MyProjects\prezzenceKotlin
$env:PREZZENCE_PLAY_APPLICATION_ID='com.pollecode.prezzence'
$env:PREZZENCE_VERSION_CODE='3'
$env:PREZZENCE_VERSION_NAME='1.0.1'
$env:PREZZENCE_SUPABASE_URL='<production supabase url>'
$env:PREZZENCE_SUPABASE_ANON_KEY='<production anon key>'
$env:PREZZENCE_UPLOAD_STORE_FILE='<path to release keystore>'
$env:PREZZENCE_UPLOAD_STORE_PASSWORD='<password>'
$env:PREZZENCE_UPLOAD_KEY_ALIAS='<alias>'
$env:PREZZENCE_UPLOAD_KEY_PASSWORD='<password>'
$env:PREZZENCE_ENABLE_WHISPER_CPP='true'
$env:PREZZENCE_SUBSCRIPTION_PRODUCT_ID='prezzence_pro'
.\gradlew.bat verifyPrezzenceReleaseEnv :app:bundleRelease --no-daemon
```

## Current Audit

See `CURRENT_REWRITE_AUDIT.md` for the latest built-vs-left status and `VISUAL_PARITY_AUDIT.md` for route-by-route visual parity coverage.

## Current Debug APK

`app/build/outputs/apk/debug/app-debug.apk`
