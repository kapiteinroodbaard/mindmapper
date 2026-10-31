# Spec Delta

## Purpose

Guarantees that the app works fully offline, collects nothing about its user and contains only free
software, so it respects users and qualifies for F-Droid without anti-features.

## ADDED Requirements

### Requirement: No network access
The app SHALL NOT request the `INTERNET` permission or any other network-related permission, and SHALL
work fully without a network connection. Opening a web link in another app is allowed.

#### Scenario: Inspect permissions
- **WHEN** the installed app's permissions are inspected
- **THEN** no network permission is listed

#### Scenario: Use the app in airplane mode
- **WHEN** the device is in airplane mode
- **THEN** every feature of the app works as it does online

### Requirement: No tracking or data collection
The app SHALL NOT contain analytics, crash reporting, advertising or any other component that records
or transmits information about the user or their usage.

#### Scenario: Dependency audit
- **WHEN** the app's dependency list is reviewed
- **THEN** it contains no analytics, crash reporting or advertising library

### Requirement: No accounts
The app SHALL be fully usable without creating, signing in to or linking any account.

#### Scenario: First launch
- **WHEN** the app is launched for the first time
- **THEN** the start screen is shown without any sign-in or registration step

### Requirement: Only free software dependencies
The app SHALL be built only from source code and libraries under licences compatible with GPLv3, and
SHALL NOT depend on Google Play Services, Firebase or other proprietary components.

#### Scenario: Build from source without proprietary SDKs
- **WHEN** the app is built from a clean checkout using only public FOSS repositories (Maven Central and Google's AndroidX repository)
- **THEN** the build succeeds and produces an installable APK

### Requirement: No runtime permissions
The app SHALL NOT request any dangerous (runtime) permission; file access SHALL go through
system-provided pickers.

#### Scenario: First use requires no permission prompt
- **WHEN** the user uses the app for the first time
- **THEN** no permission dialog is shown
