# Spec Delta

## Purpose

Defines how the app starts, which top-level screens exist, and how it behaves across orientations and
screen sizes, so later features have a stable place to live.

## ADDED Requirements

### Requirement: Supported Android versions
The app SHALL install and run on Android 15 (API level 35) and later, and SHALL NOT be installable on
earlier versions.

#### Scenario: Install on Android 15
- **WHEN** the user installs the app on a device running Android 15
- **THEN** the installation succeeds and the app launches to the start screen

#### Scenario: Install on Android 14
- **WHEN** the user attempts to install the app on a device running Android 14 or earlier
- **THEN** the installation is refused by the system

### Requirement: Start screen
The app SHALL open on a start screen that shows the app name and offers navigation to the About
screen. The start screen is the home for the document actions added by later changes.

#### Scenario: Launch from the launcher
- **WHEN** the user launches the app from the launcher
- **THEN** the start screen is shown

#### Scenario: Navigate to About
- **WHEN** the user selects "About" on the start screen
- **THEN** the About screen is shown

### Requirement: About screen
The About screen SHALL show the app name, the installed version, the licence (GNU GPL v3) and a link to
the source code repository that opens in the user's browser.

#### Scenario: About shows version and licence
- **WHEN** the About screen is shown
- **THEN** it displays the app name, the version name of the installed build and "GNU GPL v3"

#### Scenario: Open source code link
- **WHEN** the user selects the source code link
- **THEN** the system opens the repository URL in an external app able to handle web links

#### Scenario: Back from About
- **WHEN** the user presses back on the About screen
- **THEN** the start screen is shown again

### Requirement: Semantic version number
The app's version name SHALL follow Semantic Versioning (`MAJOR.MINOR.PATCH`, for example `1.0.0`), and
the same version SHALL be shown on the About screen and used in the name of every built APK.

#### Scenario: Version shown matches the build
- **WHEN** an APK built as version `1.0.0` is installed and the About screen is opened
- **THEN** the About screen shows version `1.0.0`

#### Scenario: Higher version upgrades in place
- **WHEN** version `1.1.0` is installed over version `1.0.0`
- **THEN** the system treats it as an upgrade and keeps the app's data

### Requirement: Orientation and screen size support
The app SHALL be usable in landscape and portrait on tablets and phones, with no content cut off or
overlapping at any supported size.

#### Scenario: Tablet in landscape
- **WHEN** the app runs on a tablet in landscape orientation
- **THEN** all screens use the available width and every control is reachable

#### Scenario: Phone in portrait
- **WHEN** the app runs on a phone in portrait orientation
- **THEN** all screens are fully visible, scrolling where content exceeds the screen height

### Requirement: State survives configuration changes
The app SHALL keep the current screen and its state when the device rotates, the window is resized,
or the system theme or language changes.

#### Scenario: Rotate on About screen
- **WHEN** the About screen is shown and the device is rotated
- **THEN** the About screen is still shown after rotation

#### Scenario: Resize in multi-window
- **WHEN** the app is resized in split-screen mode
- **THEN** the current screen stays shown and adapts to the new size
