# Think Twice

A native Android quiz app in English and Indonesian: 880 questions across 22 topics, each with an explanation, a daily Landmark Hunt, a weekly spotlight,
three game modes, streaks, levels and badges. Written in Kotlin with Jetpack Compose.

## Open in Android Studio

1. **File → Open…** and pick this folder.
2. Wait for Gradle sync to finish (first sync downloads dependencies).
3. Choose a device or emulator and press **Run ▶**.

Requirements: Android Studio Narwhal (2025.1) or newer, JDK 17+ (bundled with Android Studio).

## Stack

| Layer | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3, Sunghyun Sans type scale; English + Indonesian (`values-in`) |
| Navigation | Navigation Compose with type-safe routes (kotlinx.serialization) |
| State | ViewModel + StateFlow, `collectAsStateWithLifecycle`, SavedStateHandle |
| Storage | Room (quiz history, mistakes, challenge progress), DataStore (settings, profile) |
| Content | `app/src/main/assets/content.en.json` + `content.id.json` (same ids and answer keys), loaded off the main thread |
| Background | WorkManager daily reminder |

## Project layout

```
app/src/main/java/com/miqu/thinktwice/
├── MainActivity.kt, ThinkTwiceApp.kt   entry points + dependency container
├── data/      content, Room database, DataStore settings, legacy import
├── domain/    pure game rules: levels, streaks, badges, quiz plans
├── reminder/  daily notification
└── ui/        theme, shared components, one package per screen
```

## Build from the command line

```
./gradlew assembleDebug          # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit tests
```

Release signing is optional: create `keystore.properties` in the project root with
`storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Without it, release builds use the debug key.

## Upgrading from version 1

On first launch the app imports quiz history, mistakes and profile from version 1 (`think_twice.db`)
if it is installed with the same signing key. The old files are read only and left untouched.
