# Floating Screen Utility

A production-grade Android application for **screen recording**, **screenshot capture**, and a **draggable floating overlay window** — all built with modern Android best practices.

---

## ✦ Features

| Feature | Details |
|---|---|
| Screen Recording | HD/FHD/QHD via MediaProjection API — saves MP4 to `Movies/FloatingScreenUtility/` |
| Screenshot Capture | Instant or delayed (3s / 5s / 10s) — saves PNG to `Pictures/FloatingScreenUtility/` |
| Floating Window | Draggable bubble via `WindowManager`, snaps to screen edges |
| Audio Recording | Microphone, Internal Audio (Android 10+), Both, or None |
| Pause / Resume | Recording can be paused and resumed (Android 7+) |
| History Screen | Search, filter, rename, share, delete all media |
| Settings | Quality, FPS, audio, opacity, theme, auto-start |
| MVVM + Clean Arch | Domain / Data / UI layers with Hilt DI |
| Room Database | Metadata persisted locally with DAO + Flow |
| DataStore | Settings persisted with Jetpack DataStore Preferences |
| Foreground Service | Required notifications during all capture operations |
| Scoped Storage | Full Android 10–15 scoped storage compliance |

---

## ✦ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Async | Kotlin Coroutines + Flow |
| Database | Room 2.6 |
| Preferences | DataStore Preferences |
| Background | Foreground Services + WorkManager |
| Images | Coil |
| Logging | Timber |
| Build System | Gradle Kotlin DSL with Version Catalog |
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |

---

## ✦ Project Structure

```
app/src/main/kotlin/com/floatingscreen/
├── FloatingScreenApp.kt              # Application class, Hilt, notification channels
├── ui/
│   ├── MainActivity.kt               # Single activity entry point
│   ├── MainViewModel.kt              # Central VM: MediaProjection, state coordination
│   ├── AppNavGraph.kt                # Compose Navigation
│   ├── theme/
│   │   └── Theme.kt                  # MaterialTheme, color palette, dark/light
│   ├── components/
│   │   └── FloatingBubbleContent.kt  # Compose floating controls UI
│   └── screens/
│       ├── home/HomeScreen.kt        # Dashboard: record/screenshot/floating toggle
│       ├── history/
│       │   ├── HistoryScreen.kt      # Media list, search, preview, share, delete
│       │   └── HistoryViewModel.kt   # Filter, multi-select, search state
│       ├── settings/SettingsScreen.kt
│       └── permission/PermissionScreen.kt
├── service/
│   ├── ScreenRecordService.kt        # Foreground service: MediaRecorder + VirtualDisplay
│   ├── ScreenshotService.kt          # Foreground service: ImageReader + VirtualDisplay
│   └── FloatingWindowService.kt      # WindowManager overlay with drag + snap
├── media/
│   ├── ScreenRecorder.kt             # MediaRecorder wrapper, dimension calc
│   └── ScreenshotCapture.kt         # ImageReader → Bitmap → PNG save
├── data/
│   ├── local/
│   │   ├── database/AppDatabase.kt
│   │   ├── dao/MediaRecordDao.kt
│   │   └── entity/MediaRecordEntity.kt
│   └── repository/
│       ├── MediaRepositoryImpl.kt
│       └── SettingsRepositoryImpl.kt
├── domain/
│   ├── model/Models.kt               # MediaRecord, RecordingState, AppSettings, enums
│   ├── repository/Repositories.kt    # Interfaces
│   └── usecase/UseCases.kt
├── di/AppModules.kt                  # Hilt: DatabaseModule, RepositoryModule
├── permissions/PermissionManager.kt
└── utils/
    ├── BootReceiver.kt               # Auto-start on boot
    └── FileUtils.kt                  # Path helpers, size formatter
```

---

## ✦ Build Instructions

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17 (bundled with Android Studio) |
| Android SDK | API 35 |
| Gradle | 8.7 (auto-downloaded via wrapper) |

### 1. Clone / Open Project

```bash
# If cloning:
git clone <repo-url>
cd FloatingScreenUtility

# Or open existing:
# File → Open → select FloatingScreenUtility folder
```

### 2. Sync Gradle

Android Studio will auto-sync on open. If not:

```
File → Sync Project with Gradle Files
```

Or from terminal:

```bash
./gradlew dependencies
```

### 3. Build Debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Build Release APK

#### a) Create a keystore (first time only)

```bash
keytool -genkey -v \
  -keystore floating_screen.jks \
  -alias floating_screen \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

#### b) Add signing config to `app/build.gradle.kts`

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../floating_screen.jks")
            storePassword = "YOUR_STORE_PASSWORD"
            keyAlias = "floating_screen"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... existing config
        }
    }
}
```

#### c) Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### 5. Install to Device

```bash
# Debug
adb install app/build/outputs/apk/debug/app-debug.apk

# Release
adb install app/build/outputs/apk/release/app-release.apk
```

### 6. Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### 7. Generate AAB (for Play Store)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

---

## ✦ Required Permissions — User Explanation

| Permission | Why Required |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Floating bubble rendered over all other apps |
| `FOREGROUND_SERVICE` | Required to keep recording alive in background |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Android 14+ requirement for projection services |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 14+ requirement when recording mic |
| `RECORD_AUDIO` | Microphone capture during screen recording |
| `POST_NOTIFICATIONS` | Android 13+ — show foreground service notification |
| `READ_MEDIA_VIDEO` / `READ_MEDIA_IMAGES` | Android 13+ — read saved files in history |
| `WAKE_LOCK` | Prevent CPU sleep during long recordings |
| `VIBRATE` | Haptic feedback on capture actions |
| `RECEIVE_BOOT_COMPLETED` | Auto-start floating widget on boot |

All screen capture requires explicit user consent via Android's **MediaProjection system dialog** — this cannot be bypassed and is an OS-level security requirement.

---

## ✦ Architecture Notes

### MediaProjection Flow

```
User taps "Start Recording"
    → MainViewModel checks MediaProjection token
    → If missing: Activity starts system dialog via MediaProjectionManager
    → User accepts → resultCode + data stored in ViewModel
    → Intent sent to ScreenRecordService (foreground)
    → Service creates MediaProjection → VirtualDisplay → MediaRecorder surface
    → Recording streams to file
    → Stop → MediaRecorder.stop() → file saved → Room DB updated
```

### Floating Window

```
User enables floating window
    → PermissionManager checks SYSTEM_ALERT_WINDOW
    → FloatingWindowService started as foreground
    → WindowManager.addView(TYPE_APPLICATION_OVERLAY)
    → OnTouchListener handles drag + snap-to-edge animation
    → Controls panel expands/collapses with visibility toggle
```

### Clean Architecture Layers

```
UI (Compose + ViewModel)
    ↕ StateFlow / SharedFlow
Domain (UseCases, Models, Repository interfaces)
    ↕ Suspend functions / Flow
Data (Room DAOs, DataStore, File system, Services)
```

---

## ✦ Compatibility Matrix

| Android Version | API | Support |
|---|---|---|
| Android 10 | 29 | ✅ Full support (minimum) |
| Android 11 | 30 | ✅ Scoped storage |
| Android 12 | 31 | ✅ Splash screen, exact alarms |
| Android 13 | 33 | ✅ Granular media permissions, notification permission |
| Android 14 | 34 | ✅ Foreground service type declarations |
| Android 15 | 35 | ✅ Target SDK, edge-to-edge |

---

## ✦ Known Limitations

1. **Internal audio capture** requires Android 10+ and is limited to apps that allow audio sharing (most streaming services opt out via `USAGE_MEDIA` flags).
2. **Pause/Resume** recording requires Android 7.0+ (`MediaRecorder.pause()`).
3. The **floating window** requires `SYSTEM_ALERT_WINDOW` — this is a sensitive permission Android shows a dedicated settings page for (not a runtime dialog).
4. MediaProjection tokens are **single-use per consent** — if the service is killed, a new consent dialog is required.
5. Some OEMs (Xiaomi, Huawei, OPPO) aggressively kill background services — users should add the app to battery optimization whitelist.

---

## ✦ OEM Battery Optimization

For reliable background recording on OEM ROMs:

| OEM | Setting Location |
|---|---|
| Xiaomi / MIUI | Security → Battery Saver → No restrictions |
| Samsung / One UI | Battery → Background usage limits → Never sleeping |
| Huawei / EMUI | Battery → App launch → Manage manually, enable all |
| OnePlus | Battery → Battery Optimization → Don't optimize |
| OPPO / ColorOS | Battery → Energy Saver → Custom → allow all |

---

## ✦ License

```
MIT License

Copyright (c) 2024 Floating Screen Utility

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
```
