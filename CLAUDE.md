@import .claude/project.md

# Music Assistant Companion

## Overview
Kotlin Multiplatform (KMP) Compose app — Android + iOS client for Music Assistant home audio server. Targets both phone and Android TV (Shield).

## Architecture
- **Compose Multiplatform 1.9.3** with Material 3 / Expressive design
- **KMP targets:** `androidMain`, `iosMain`, `commonMain`
- **DI:** Koin
- **Networking:** Ktor (WebSockets for real-time player state)
- **Image loading:** Coil 3
- **Serialization:** kotlinx.serialization
- **Logging:** Kermit
- **Navigation:** AndroidX Navigation3

## Key Directories
```
composeApp/src/commonMain/kotlin/io/music_assistant/client/
├── ui/compose/home/       # Main UI screens
│   ├── HomeScreen.kt      # Main screen, mini/expanded player layout, TV sidebar container
│   ├── PlayersPager.kt    # Player pager, progress bar, volume dialog
│   ├── PlayersTopBar.kt   # Top bar with speaker picker
│   ├── CollapsibleQueue.kt # Queue list
│   ├── TvNavigationRail.kt # Custom TV sidebar nav (full-width rows, not NavigationRail)
│   ├── TvNowPlayingWidget.kt # Horizontal now-playing in sidebar
│   ├── LandingPage.kt     # Library cards grid
│   └── PlayerItems.kt     # Compact/Full player item composables
├── ui/compose/library/
│   ├── LibraryScreen.kt   # Library tabs, TV sticky header with art fades + favorites chip
│   └── AdaptiveMediaGrid.kt # Responsive grid for media items
├── ui/compose/item/
│   └── ItemDetailsScreen.kt # Item details, TV header with art fades + placeholder icons
├── ui/theme/Theme.kt      # Theme, colors, HeaderFontFamily (Outfit)
├── data/model/            # Data model classes (serialization)
│   └── client/AppMediaItem.kt # Sealed class: Artist, Album, Track, Playlist, Podcast, PodcastEpisode, RadioStation
└── ...
composeApp/src/commonMain/composeResources/
├── font/                  # outfit_bold.ttf, outfit_light.ttf
└── ...
```

## Building

### Local debug build
```bash
export JAVA_HOME="/c/Users/anthony/jdk-17.0.18+8"
export ANDROID_HOME="/c/Users/anthony/Android/Sdk"
cd /c/Users/anthony/music-assistant-companion
./gradlew assembleDebug
```
APK output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### Local release build
```bash
export JAVA_HOME="/c/Users/anthony/jdk-17.0.18+8"
export ANDROID_HOME="/c/Users/anthony/Android/Sdk"
./gradlew assembleRelease
```
APK output: `composeApp/build/outputs/apk/release/composeApp-release.apk`
- Release keystore: `release.keystore` (gitignored), alias `mass-companion`
- R8 minification enabled (~9MB vs ~26MB debug)

### Install on devices
```bash
# Shield TV (always connected)
adb connect 192.168.0.127:5555
adb -s 192.168.0.127:5555 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Phone (wireless — port changes, needs re-pairing via Settings > Developer > Wireless debugging)
adb pair <ip>:<pairing-port>    # enter pairing code
adb connect <ip>:<connect-port>
adb -s <ip>:<port> install -r <apk>
```

## Release Process
1. Bump `versionCode` and `versionName` in `composeApp/build.gradle.kts`
2. Commit and push to main
3. Tag with semantic version: `git tag v1.x.x && git push origin v1.x.x`
4. CI workflow (`.github/workflows/release-apk.yml`) builds release APK and creates GitHub Release
5. Or trigger manually via workflow_dispatch with version input

### GitHub Secrets (configured)
- `RELEASE_KEYSTORE_BASE64` — base64-encoded release.keystore
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

## CI Workflows
- **`release-apk.yml`** — Triggered by `v*` tags or manual dispatch. Builds signed release APK, creates GitHub Release.
- **`debug-apk-release.yml`** — Manual only (`workflow_dispatch`). Builds debug APK with timestamp tag.

## Important Patterns
- `Modifier.conditional()` is a custom extension — its lambda is NOT @Composable. Hoist any `MaterialTheme` reads before the modifier chain.
- TV uses D-pad focus system (`FocusRequester`, `onPreviewKeyEvent`) — don't break focus handling.
- Phone mini player is a Box overlay (floating, frosted glass) — not part of a Column layout.
- `isTV` flag controls phone vs TV layout branches throughout the UI.
- TV sidebar is a custom Column (not Material NavigationRail) — 210dp wide, rounded container with `surfaceContainerLow` background.
- TV library/item-details headers use 4-sided gradient fades over artwork (left, top, bottom, right).
- `iconForMediaItem()` helper maps AppMediaItem subtypes to Material icons (used for placeholder art).
- TV favorites chip lives inside `TvLibraryStickyHeader`, not as a standalone element above it.

## Current Version
- v2026.02.17 (versionCode 4)
- Repo: https://github.com/anthonymkz/music-assistant-companion
