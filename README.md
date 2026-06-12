# Prezzence Kotlin

Native Android/Kotlin rewrite workspace for Prezzence.

This project is separate from the React Native app so it can be built and tested without breaking the Play Store beta app.

Current scope:
- Kotlin-only Android app shell
- Native navigation across splash, landing, home, questions, room setup, interview, and result screens
- Duix SDK module copied into the project
- Camera/audio permissions wired for the interview flow

Next migration phases:
1. Mount the Duix avatar renderer into the interview card.
2. Move on-device transcription and camera presence scoring into native Kotlin services.
3. Port Supabase auth/session/history with OkHttp/Kotlin coroutines.
4. Replace placeholder cards with production UI and data models.
