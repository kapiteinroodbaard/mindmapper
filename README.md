# Mindmapper

A free and open source (GPLv3) mind-mapping app for Android, made for preparing and giving presentations
on a tablet. No ads, no tracking, no account and no internet access.

## Features

- Central topic in seven shapes with a colour selector
- Tapered branches (thick at the centre, thinner per level) with automatic colours per main branch;
  adjustable trunk width and taper
- A style per main branch: standard, blurred, plant (leaves and tendrils), lightning or straight; the
  style shapes the branch, the main topic's box and all of its subtopics
- Topic actions right on the map: "+" handles to add subtopics and topics, and an action bar on the
  selected topic
- Bold, italic and underlined topic text
- Automatic layout around the central topic (balanced, clockwise) or to the right of it
- Text, an image (embedded, resizable) and an icon per topic
- Drag to reorder, move to the other side or attach to another topic; undo/redo
- Presentation mode: full screen, tap a branch to zoom in, back to the overview
- Presentation timing: plan a duration per main branch (the central topic shows the total); while
  presenting, a count-up timer makes the branch you should be at glow
- Office-style files: open and save `.mindmap` files anywhere, rename them from the editor, autosave,
  recent files with thumbnails
- Export to PDF (one page) and PNG, save or share
- English and Dutch, light and dark theme

## File format

A `.mindmap` file is a ZIP archive:

```
map.json        the mind map (with "formatVersion"), readable JSON
images/*.webp   embedded images, downscaled to at most 1600 px
thumbnail.png   preview used by the recent-files list
```

Product decisions and roadmap: [docs/exploration-notes.md](docs/exploration-notes.md).
Specifications and changes: [openspec/](openspec/).

## Requirements

- JDK 17 or newer (`JAVA_HOME` must point to it)
- Android SDK with platform 37 (`sdk.dir` in `local.properties`)
- Devices: Android 15 (API 35) or newer

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

APKs are written to `app/build/outputs/mindmapper/` as `mindmapper-v<version>.apk` (release) and
`mindmapper-v<version>-debug.apk` (debug).

## Test

```bash
./gradlew check
```

Runs lint (missing translations are errors), JVM unit tests for `:core`, Robolectric UI tests for
`:app`, and a check that fails when the release manifest requests network or dangerous permissions.

## Versioning

The version is defined once as `app.version` in `gradle.properties` and follows Semantic Versioning
(`MAJOR.MINOR.PATCH`, minor and patch 0–99). `versionName`, `versionCode`
(`major*10000 + minor*100 + patch`), the About screen and the APK name all derive from it.

## Machine-specific settings (`local.properties`, not committed)

```properties
sdk.dir=/path/to/Android/Sdk

# Optional: also copy release APKs to this folder (overwrites the same version).
apk.outputDir=/path/to/Apks

# Optional: sign release builds. Without this, release APKs are unsigned (as F-Droid expects).
signing.storeFile=/path/to/release.jks
signing.storePassword=...
signing.keyAlias=...
signing.keyPassword=...
```

Create a keystore once with:

```bash
keytool -genkeypair -keystore ~/keys/mindmapper-release.jks -alias mindmapper -keyalg RSA -keysize 4096 -validity 36500
```

Keep a backup of the keystore outside the repository: without it, installed builds cannot be upgraded.

## Dependencies

Release runtime dependencies are limited to AndroidX (including Jetpack Compose and Navigation), the
Kotlin standard library, kotlinx.serialization and Guava's `listenablefuture` stub (pulled in by
AndroidX). All are Apache 2.0 licensed. There are no Google Play Services, Firebase, analytics, crash
reporting or advertising libraries.

## Licence

GNU General Public License v3.0 — see [LICENSE](LICENSE).
