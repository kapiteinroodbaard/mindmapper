# Mindmapper

A free and open source (GPLv3) mind-mapping app for Android, made for preparing and giving presentations
on a tablet. No ads, no tracking, no account and no internet access.

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

# Optional: also copy every built APK to this folder (overwrites the same version).
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
