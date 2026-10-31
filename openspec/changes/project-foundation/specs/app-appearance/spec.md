# Spec Delta

## Purpose

Defines the app's visual theme and how it follows the system light/dark setting; later changes reuse
this setting for the appearance of the mind map itself.

## ADDED Requirements

### Requirement: Light and dark theme follow the system
The app SHALL use a light theme when the system is in light mode and a dark theme when the system is
in dark mode. The app SHALL NOT offer its own theme setting.

#### Scenario: System in dark mode
- **WHEN** the system dark theme is enabled and the app is opened
- **THEN** all app screens use the dark theme

#### Scenario: System in light mode
- **WHEN** the system dark theme is disabled and the app is opened
- **THEN** all app screens use the light theme

#### Scenario: Theme changes while the app is open
- **WHEN** the user switches the system theme while the app is in the foreground
- **THEN** the app switches to the matching theme without restarting and without losing the current screen

### Requirement: Consistent app colours
The app SHALL use its own fixed colour scheme for light and dark mode, so it looks the same on every
device regardless of wallpaper-based system colours.

#### Scenario: Different wallpapers
- **WHEN** the app is opened on two devices with different wallpapers and the same system theme
- **THEN** both show the same app colours

### Requirement: Edge-to-edge display
The app SHALL draw edge-to-edge behind the system bars while keeping its content and controls clear of
the status bar, navigation bar and display cutouts.

#### Scenario: Device with gesture navigation
- **WHEN** the app runs on a device using gesture navigation
- **THEN** no control is placed under the gesture area or status bar
