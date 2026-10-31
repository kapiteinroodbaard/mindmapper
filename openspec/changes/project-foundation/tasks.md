# Tasks

## 1. Project scaffolding

- [ ] 1.1 Add Gradle wrapper (with `distributionSha256Sum`), `settings.gradle.kts` (repositories `google()` and `mavenCentral()` only), root `build.gradle.kts` and `gradle/libs.versions.toml` with pinned versions; verify `./gradlew help` succeeds
- [ ] 1.2 Create the `app` module: Kotlin, Compose enabled, `minSdk 35`, latest stable `compileSdk`/`targetSdk`, namespace/application ID defined once (see design Open Questions), `versionName 0.1.0` with derived `versionCode`; verify `./gradlew assembleDebug` produces an APK
- [ ] 1.3 Configure reproducibility settings (`dependenciesInfo` excluded from APK/bundle, `vcsInfo.include = false`, no timestamps in `BuildConfig`); verify two consecutive clean `assembleRelease` builds (unsigned) produce byte-identical APKs
- [ ] 1.4 Define the version once as `app.version=0.1.0` in `gradle.properties`, derive `versionName` and `versionCode` from it and fail the build for a non-`MAJOR.MINOR.PATCH` value or minor/patch above 99; verify `0.1.0` builds and `0.1` and `1.100.0` fail
- [ ] 1.5 Name APK outputs `mindmapper-v<version>.apk` (release) and `mindmapper-v<version>-debug.apk` (debug); verify the files in `app/build/outputs/apk/` have these names
- [ ] 1.6 Add a copy task after `assembleRelease`/`assembleDebug` that copies the APK to the folder in `apk.outputDir` from `local.properties`, overwriting an existing file and skipping when the property is absent; set `apk.outputDir=/home/wlenaerts/Sync/apps/Apks` locally and verify the APK appears there, then verify a build without the property succeeds without copying
- [ ] 1.7 Sign release builds with the keystore from `local.properties` (`signing.*`) when configured and leave them unsigned otherwise; verify `apksigner verify` succeeds on the copied release APK and that a build without the properties produces an unsigned APK
- [ ] 1.8 Add `.gitignore` (including `local.properties` and keystores), GPLv3 `LICENSE` and a `README.md` with build, test, versioning, signing and APK-folder instructions; verify the README commands run as written

## 2. Theme and edge-to-edge

- [ ] 2.1 Implement the app theme with fixed light and dark `ColorScheme`s selected by the system dark-mode setting (no dynamic colour); verify with a Compose UI test that toggles dark mode and checks the background colour
- [ ] 2.2 Enable edge-to-edge in the activity and add a shared scaffold that applies system-bar and cutout insets; verify with a UI test that the top app bar and content are not drawn under the status bar

## 3. Localization

- [ ] 3.1 Add `values/strings.xml` (English) and `values-nl/strings.xml` (Dutch) for all start and About screen texts, and enable `generateLocaleConfig`; verify the app appears with both languages in system Settings › Apps › Mindmapper › Language on an API 35 emulator
- [ ] 3.2 Configure lint so `MissingTranslation` and `HardcodedText` are errors; verify by temporarily removing one Dutch string that `./gradlew lint` fails, then restore it
- [ ] 3.3 Add a UI test that runs the start screen under the `nl` and `en` locales and a third unsupported locale (`de`), asserting the expected title text each time

## 4. Start and About screens

- [ ] 4.1 Set up Navigation Compose with type-safe routes for the start and About screens, with system back returning to start; verify with a UI test covering start → About → back
- [ ] 4.2 Build the start screen (app name, About action, empty area reserved for document actions) laid out for landscape tablets and portrait phones; verify with UI tests at tablet-landscape and phone-portrait sizes that all elements are displayed
- [ ] 4.3 Build the About screen showing app name, version name from the installed package, "GNU GPL v3" and a source code link that fires an `ACTION_VIEW` intent; verify with a UI test using Espresso-Intents that the intent is sent with the configured URL
- [ ] 4.4 Ensure state survives configuration changes; verify with a UI test that rotates the device (`StateRestorationTester` or activity recreation) on the About screen and asserts it is still shown

## 5. Privacy guardrails

- [ ] 5.1 Add a Gradle task, wired into `check`, that fails when the merged release manifest contains `INTERNET`, `ACCESS_NETWORK_STATE` or any dangerous permission; verify it fails when `INTERNET` is temporarily added and passes without it
- [ ] 5.2 Review the resolved release dependency tree (`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`) for proprietary, analytics, crash-reporting or ad libraries and record the result in `README.md` under a "Dependencies" section; verify the list contains only AndroidX, Compose and Kotlin artifacts

## 6. F-Droid metadata

- [ ] 6.1 Add `fastlane/metadata/android/en-US/` and `nl-NL/` with `title.txt`, `short_description.txt` (≤ 80 characters), `full_description.txt` and `changelogs/<versionCode>.txt`; verify the files exist and the short descriptions are within the limit

## 7. Integration check

- [ ] 7.1 Run `./gradlew check` and `./gradlew connectedCheck` on an API 35 emulator (tablet profile, landscape); verify all pass, then install the APK in airplane mode and confirm the app opens with no permission or sign-in prompt
