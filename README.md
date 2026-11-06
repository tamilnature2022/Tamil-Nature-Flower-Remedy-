# TamilNatureShanmugam Android project — v0.2

This Android Studio project wraps the bilingual Tamil + English Bach Flower screening app in an Android WebView. The bundled app includes 77 screening items across the seven Bach emotional categories and a separate PSS-10 section.

## Build APK with Android Studio
1. Install Android Studio and Android SDK Platform 35.
2. Extract this ZIP and open the `TamilNatureShanmugam_Android` folder in Android Studio.
3. Wait for Gradle sync and accept any requested SDK installation.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK online with GitHub Actions (no local Android SDK needed)
1. Create a new GitHub repository.
2. Extract this ZIP and upload the *contents of the `TamilNatureShanmugam_Android` folder* to the repository root (so `app/`, `build.gradle.kts`, and `.github/` are at the root).
3. Push to `main`/`master`, or open **Actions → Build Android APK → Run workflow**.
4. When the workflow completes, open its run, find **Artifacts**, and download `TamilNatureShanmugam-debug-apk`.

The workflow builds a **debug APK for testing**, not a signed Play Store release. A release APK/AAB requires a private signing key and a separate signed-release setup.

## App status and safety
- This is a prototype, not a validated diagnostic or treatment tool.
- The 77-question remedy mapping is a research draft and requires expert review and pilot validation.
- PSS-10 is a perceived-stress measure, not a diagnosis. Items 4, 5, 7, and 8 are reverse-scored.
- Test only with dummy data until consent, privacy, security, backup, and data-deletion procedures are reviewed.
- Supabase cloud use requires the user's own project URL and public anon/publishable key. Never use a service-role/secret key in the app.
