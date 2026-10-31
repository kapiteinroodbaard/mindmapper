# Design

## Context

Empty repository; see proposal.md for motivation and `docs/exploration-notes.md` for the full product
direction. Requirements are in `specs/`. This change only sets up the skeleton, but its choices
(module layout, application ID, build settings, dependency policy) are expensive to change once the
app is published on F-Droid, so they are decided here.

## Goals / Non-Goals

**Goals:**
- A project structure that changes 2–6 can extend without restructuring.
- A build that F-Droid can reproduce from source with no extra steps.
- Guardrails (lint, tests) that catch violations of the privacy and localization specs automatically.

**Non-Goals:**
- Any mind-map domain code, file handling or Canvas rendering.
- Store listing screenshots, or actually submitting to F-Droid.
- CI configuration for a specific hosting service; `./gradlew check` is the contract.

## Decisions

### Two modules: `app` and `core`
`core` is a plain Kotlin/JVM module for everything that does not need Android: the mind-map model,
layout, colour assignment and file format. `app` holds the activity, Compose UI, Android rendering
(Canvas, PDF, PNG) and storage access. This keeps the algorithmic code fast to unit-test on the JVM.
*Alternative:* one module with packages. Rejected: nothing would stop Android types leaking into the
layout code, and the split costs almost nothing.

### Kotlin + Jetpack Compose, Material 3, Navigation Compose
Compose is the current Android UI standard and is fully Apache 2.0. Navigation Compose with type-safe
routes handles start ↔ about now and the editor/presentation screens later.
*Alternative:* Views/XML. Rejected: more code and worse fit for the custom map canvas.

### SDK levels
`minSdk = 35`, `compileSdk`/`targetSdk` = latest stable SDK at implementation time (36 at the time of
writing). Raising `minSdk` to 35 lets the code use all modern APIs (photo picker, per-app languages,
predictive back) without compatibility branches.

### Fixed colour scheme, no dynamic colour
The app defines its own light and dark `ColorScheme` instead of Material You dynamic colour, so the UI
and, later, the mind map look identical on every device (spec `app-appearance`). The theme reads
`isSystemInDarkTheme()`; the mind-map renderer in change 3 will receive the same flag.

### Localization through string resources + `localeConfig`
All strings live in `values/strings.xml` (English, fallback) and `values-nl/strings.xml`.
`generateLocaleConfig = true` in `androidResources` exposes the languages to Android's per-app language
settings without custom UI. Lint's `MissingTranslation` and `HardcodedText` checks are raised to
errors so the spec's "lint fails" scenario holds.

### Dependency policy and reproducibility
- Repositories: only `google()` and `mavenCentral()`.
- All versions in `gradle/libs.versions.toml`; no dynamic (`+`) versions.
- Gradle wrapper committed with `distributionSha256Sum`.
- `dependenciesInfo { includeInApk = false; includeInBundle = false }` (F-Droid rejects the encrypted
  dependency metadata blob).
- `vcsInfo.include = false` and no build timestamps in `BuildConfig`, to keep builds reproducible.
- A small Gradle check task fails the build if the merged manifest contains `INTERNET`,
  `ACCESS_NETWORK_STATE` or any dangerous permission, enforcing the `privacy` spec mechanically.

### Version scheme
`versionName` follows semantic versioning (`0.1.0` for this change); `versionCode` is derived as
`major*10000 + minor*100 + patch`. The About screen reads both from `BuildConfig`/package info.
The version is defined once (`app.version` in `gradle.properties`) and everything else derives from
it. Minor and patch are therefore limited to 0–99; the build fails on a version outside that range or
not matching `MAJOR.MINOR.PATCH`.

### APK naming and copy to the personal APK folder
- Built APKs are named `mindmapper-v<version>.apk` for release and `mindmapper-v<version>-debug.apk`
  for debug, so a debug build never overwrites a release with the same version.
- After `assembleRelease`/`assembleDebug`, a Gradle `Copy` task places the renamed APK in a destination
  folder. The destination is read from the `apk.outputDir` property in `local.properties` (not
  committed); on this machine it is `/home/wlenaerts/Sync/apps/Apks`. Without the property, the copy is
  skipped, so F-Droid and other contributors build unaffected.
- The APK is **copied**, not moved: Gradle's own install and test tasks expect it in `build/outputs`.
- An existing file with the same name in the destination is overwritten (rebuilding the same version).
*Alternative:* hard-coding the path in `build.gradle.kts`. Rejected: breaks every other machine and
the F-Droid build server.

### Local release signing
Release APKs copied to the APK folder must be installable, so they are signed when a keystore is
configured in `local.properties` (`signing.storeFile`, `signing.storePassword`, `signing.keyAlias`,
`signing.keyPassword`). Without it the release build stays unsigned, which is what F-Droid expects
(it signs with its own key). The keystore and its passwords are never committed.

### F-Droid metadata
`fastlane/metadata/android/en-US/` and `nl-NL/` with `title.txt`, `short_description.txt`,
`full_description.txt` and a `changelogs/<versionCode>.txt`. The F-Droid build recipe itself lives in
the fdroiddata repository and is out of scope.

### Testing
JVM unit tests with JUnit in `core`; Compose UI tests in `app` run on the JVM with Robolectric
(`./gradlew check`). Tests on ADB-connected devices or emulators are not used for now (user decision),
so behaviour that Robolectric cannot reproduce faithfully (real edge-to-edge insets, the system app
language screen) is verified by hand on a device.

## Risks / Trade-offs

- [minSdk 35 excludes most devices in use today] → Accepted by the user; the app targets their own
  devices first. Lowering it later is possible because no API-35-only design is baked into the specs.
- [Edge-to-edge is mandatory for targetSdk 35+ and easy to get wrong on tablets with cutouts] →
  Use `enableEdgeToEdge()` and window-insets padding in a shared scaffold; covered by a UI test.
- [Compose BOM upgrades can break reproducible builds] → Pin exact versions in the catalog; upgrades
  are deliberate commits.
- [APKs signed with the local key cannot be updated by F-Droid builds (different signature), and vice
  versa] → Expected; choose one source per device. Losing the local keystore means uninstalling to
  upgrade, so keep a backup of it outside the repository.
- [Placeholder start screen looks empty until change 2] → Acceptable; the app is not released before
  change 2 at the earliest.

## Open Questions

- **Application ID and package name**: `io.github.wlenaerts.mindmapper`, matching the user's other
  apps; confirm before the first public release, because F-Droid identifies the app by it.
- **Source repository URL** for the About screen link: placeholder
  `https://github.com/wlenaerts/mindmapper` in `AppInfo.SOURCE_CODE_URL`; update once the repository
  is hosted.
- **App name**: "Mindmapper" is used as the working title.
