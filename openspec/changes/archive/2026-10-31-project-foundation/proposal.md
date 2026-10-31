# Proposal

## Why

Mindmapper is a greenfield FOSS Android app for building mind maps and using them while presenting, as an
alternative to commercial apps that cost money, track users or require accounts. Before any mind-map
feature can be built, the project needs a buildable, F-Droid-compatible app skeleton that already holds
the non-negotiable product constraints (privacy, minSdk 35, tablet-first, Dutch/English, light/dark), so
every later change builds on them instead of retrofitting them.

This is change 1 of 6 in the roadmap in `docs/exploration-notes.md`.

## What Changes

- New Android project (Gradle Kotlin DSL, version catalog) with one `app` module in Kotlin and Jetpack
  Compose (Material 3), `minSdk 35`, targeting the latest stable SDK.
- Single-activity app with a start screen (placeholder for the later "new / open / recent files"
  actions) and an About screen showing app name, version, licence (GPLv3) and a link to the source code.
- Layout that works in landscape and portrait on tablets and phones, including configuration changes
  without losing state.
- App theme that follows the system light/dark setting.
- Dutch and English UI strings, following the system language with a per-app language override via
  Android's app language settings.
- Privacy guarantees: no network permission, no proprietary libraries, no analytics/crash reporting,
  no accounts.
- F-Droid readiness: GPLv3 `LICENSE`, `fastlane/metadata/android/{en-US,nl-NL}` descriptions, a
  dependency set that contains only FOSS libraries, and reproducible-build-friendly Gradle settings.
- Unit and instrumented test setup plus a CI-independent `./gradlew check` that runs lint and tests.

Out of scope for this change: anything mind-map specific (document format, rendering, editor,
presentation, export). Those are changes 2–6.

## Capabilities

### New Capabilities
- `app-shell`: app entry point, start screen, About screen, navigation, and behaviour across
  orientations and screen sizes.
- `app-appearance`: the app's visual theme and how it follows the system light/dark setting.
- `localization`: supported UI languages and how the active language is chosen.
- `privacy`: guarantees about network access, tracking, accounts and third-party dependencies.

### Modified Capabilities
<!-- None: greenfield project, no existing specs. -->

## Impact

- Creates the whole project structure: `settings.gradle.kts`, `build.gradle.kts`,
  `gradle/libs.versions.toml`, Gradle wrapper, `app/` module, `LICENSE`, `README.md`, `fastlane/`.
- Dependencies limited to AndroidX, Jetpack Compose, Kotlin standard libraries and test libraries
  (JUnit, AndroidX Test); all Apache 2.0 or compatible with GPLv3.
- No runtime permissions and no `INTERNET` permission; later changes must keep it that way.
- Establishes package name, application ID and module layout that changes 2–6 build on.
