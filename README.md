# Football Training Companion

An Android 13+-ready training companion app that guides football lovers through workouts, scores their sessions with a CameraX-powered recognizer, and keeps every result (plus the supporting training video) neatly organized. The personal center doubles as a lightweight profile hub where you can swap avatars, review your progress, and export or replay practice clips.

## Feature Highlights

| Area | What you get |
| --- | --- |
| **Onboarding** | Email/password login & registration with light validation and persistent session state. |
| **Training** | CameraX preview + Recorder capture for drills (shooting, dribbling, passing), simulated scoring & coaching tips, one-tap record saving. |
| **Video Replay** | ExoPlayer-based playback of every saved session, path validation, and helpful error prompts if a file goes missing. |
| **History & Analytics** | Reverse-chronological training log stored locally via `SPUtils`, including timestamp, drill type, counts, and average score. |
| **Personal Center** | Account banner, per-user avatar stored in app-private storage, help dialog, logout, and the full training-record list with quick access to replays. |

## Screens & Flow

1. **Splash & Login** → routes to `MainActivity` once `SPUtils.isLogin` is true.
2. **Home Tabs** (BottomNavigation):
   - *Train*: live preview, start/stop recognition, CameraX recording, per-mode stats, save action.
   - *Results*: historical cards (score, tips, navigation to detail screens).
   - *Mine*: combined profile + log view described above.
3. **Playback** → `VideoPlayerActivity` opens with ExoPlayer/PlayerView, media controls, and file path hint.

## Project Layout

```
Football/
├── app/
│   ├── src/main/java/com/example/football/
│   │   ├── ui/login/...           # Auth screens
│   │   ├── ui/main/...            # Tab navigation + fragments
│   │   ├── ui/train/...           # CameraX recognizer + video player
│   │   └── utils/SPUtils.kt       # SharedPreferences helpers
│   └── src/main/res/...           # Material 3 themed resources
├── gradle/                        # Wrapper + versions catalog
└── README.md                      # 👈 You are here
```

## Requirements

- Android Studio Iguana or newer (AGP 9 compatible)
- JDK 11 (Gradle wrapper already targets it)
- Android SDK 34+ with Google APIs
- Physical device or emulator running Android 10 (API 29) or newer (CameraX/Recorder tested down to API 26)

## Quick Start

1. **Clone & Sync**
   ```bash
   git clone <your-repo-url>
   cd Football
   ./gradlew --version
   ```
2. **Open in Android Studio** → let it import the Gradle settings (uses `libs.versions.toml`).
3. **Build Debug APK**
   ```bash
   ./gradlew clean assembleDebug
   ```
4. **Run**
   - From Android Studio: select *app* configuration → choose device → Run.
   - CLI deploy (USB debug enabled):
     ```bash
     ./gradlew installDebug
     adb shell am start -n com.example.football/.ui.splash.SplashActivity
     ```
5. **First Login**
   - Register any account on the device once.
   - New sessions auto-open the Main screen until you tap Logout in Mine.

## Tips for Contributors

- **CameraX tuning**: adjust quality or enable audio inside `TrainFragment.startVideoRecording()`.
- **Playback stability**: `VideoPlayerActivity` already uses ExoPlayer, so drop in analytics or DRM modules if needed.
- **State storage**: `SPUtils` centralizes simple flags/strings; expand with Room if you need multi-user sync.
- **Testing**: `./gradlew testDebug` for JVM tests, `./gradlew connectedDebugAndroidTest` for instrumented runs.

Happy training ⚽️

