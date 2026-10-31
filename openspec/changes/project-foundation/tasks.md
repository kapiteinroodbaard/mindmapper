# Tasks

## 1. Project scaffolding

- [x] 1.1 Add Gradle wrapper (with `distributionSha256Sum`), `settings.gradle.kts` (repositories `google()` and `mavenCentral()` only), root `build.gradle.kts` and `gradle/libs.versions.toml` with pinned versions; verify `./gradlew help` succeeds
- [x] 1.2 Create the `app` and `core` modules: Kotlin, Compose enabled, `minSdk 35`, latest stable `compileSdk`/`targetSdk`, namespace/application ID `io.github.wlenaerts.mindmapper`; verify `./gradlew assembleDebug` produces an APK
- [x] 1.3 Configure reproducibility settings (`dependenciesInfo` excluded from APK/bundle, `vcsInfo.include = false`, no `BuildConfig` timestamps); verify two consecutive clean `assembleRelease` builds (unsigned) produce byte-identical APKs
- [x] 1.4 Define the version once as `app.version=0.1.0` in `gradle.properties`, derive `versionName` and `versionCode` from it and fail the build for a non-`MAJOR.MINOR.PATCH` value or minor/patch above 99; verify `0.1.0` builds and `0.1` and `1.100.0` fail
- [x] 1.5 Produce APKs named `mindmapper-v<version>.apk` (release) and `mindmapper-v<version>-debug.apk` (debug) in `app/build/outputs/mindmapper/`; verify the files have these names
- [x] 1.6 Add a copy task after `assembleRelease`/`assembleDebug` that copies the APK to the folder in `apk.outputDir` from `local.properties`, overwriting an existing file and skipping when the property is absent; set `apk.outputDir=/home/wlenaerts/Sync/apps/Apks` locally and verify the APK appears there
- [x] 1.7 Sign release builds with the keystore from `local.properties` (`signing.*`) when configured and leave them unsigned otherwise; verify `apksigner verify` succeeds with a (throwaway) keystore and fails without one
- [x] 1.8 Add `.gitignore` (including `local.properties` and keystores), GPLv3 `LICENSE` and a `README.md` with build, test, versioning, signing and APK-folder instructions; verify the README commands run as written

## 2. Theme and edge-to-edge

- [x] 2.1 Implement the app theme with fixed light and dark `ColorScheme`s selected by the system dark-mode setting (no dynamic colour); verify with a Robolectric Compose test under `night` and `notnight` qualifiers
- [x] 2.2 Enable edge-to-edge in the activity and let every screen's `Scaffold` apply system-bar and cutout insets; verified on a device in group 7

## 3. Localization

- [x] 3.1 Add `values/strings.xml` (English) and `values-nl/strings.xml` (Dutch) for all start and About screen texts, and enable `generateLocaleConfig`; verify the generated locale config lists `en` and `nl`
- [x] 3.2 Configure lint so `MissingTranslation`, `ExtraTranslation` and `HardcodedText` are errors; verify by temporarily removing one Dutch string that `./gradlew lint` fails, then restore it
- [x] 3.3 Add a Robolectric UI test that runs the start screen under the `nl`, `en` and unsupported `de` locales, asserting the expected tagline each time

## 4. Start and About screens

- [x] 4.1 Set up Navigation Compose with type-safe routes for the start and About screens, with back returning to start; verify with a UI test covering start → About → back
- [x] 4.2 Build the start screen (app name, About action, area reserved for document actions) for landscape tablets and portrait phones; verify with UI tests at tablet-landscape and phone-portrait sizes
- [x] 4.3 Build the About screen showing app name, installed version name, "GNU GPL v3" and a source code link that fires an `ACTION_VIEW` intent; verify with a UI test that the intent is sent with the configured URL
- [x] 4.4 Ensure state survives configuration changes; verify with `StateRestorationTester` that the About screen is still shown after recreation

## 5. Privacy guardrails

- [x] 5.1 Add a Gradle task, wired into `check`, that fails when the merged release manifest contains `INTERNET`, `ACCESS_NETWORK_STATE` or a dangerous permission; verify it fails when `INTERNET` is temporarily added and passes without it
- [x] 5.2 Review the resolved release dependency tree for proprietary, analytics, crash-reporting or ad libraries and record the result in `README.md` under "Dependencies"

## 6. F-Droid metadata

- [x] 6.1 Add `fastlane/metadata/android/en-US/` and `nl-NL/` with `title.txt`, `short_description.txt` (≤ 80 characters), `full_description.txt` and `changelogs/100.txt`

## 7. Integration check

- [x] 7.1 Run `./gradlew check`; verify lint, unit tests, Robolectric UI tests and the permission check pass
- [ ] 7.2 Manual check on a tablet (not via ADB): install the APK from the APK folder, confirm it opens in airplane mode without permission or sign-in prompt, that no control sits under the status/navigation bar, and that Settings › Apps › Mindmapper › Language offers English and Dutch
