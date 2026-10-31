# localization Specification

## Purpose
Defines which languages the app's user interface is available in and how the active language is
selected, so every user-facing text is translatable from the start.

## Requirements

### Requirement: Supported UI languages
The app SHALL provide its complete user interface in English and Dutch. English SHALL be the fallback
language.

#### Scenario: System language Dutch
- **WHEN** the system language is Dutch and the app is opened
- **THEN** all app texts are shown in Dutch

#### Scenario: System language English
- **WHEN** the system language is English and the app is opened
- **THEN** all app texts are shown in English

#### Scenario: Unsupported system language
- **WHEN** the system language is neither Dutch nor English (for example German)
- **THEN** all app texts are shown in English

### Requirement: Per-app language choice
The app SHALL appear in Android's per-app language settings with English and Dutch as options, and
SHALL use the language chosen there instead of the system language.

#### Scenario: Override to English on a Dutch system
- **WHEN** the system language is Dutch and the user selects English for this app in the system's app language settings
- **THEN** the app shows all texts in English

### Requirement: No hard-coded user-facing text
Every text shown to the user SHALL come from translatable resources that exist in both languages.

#### Scenario: Missing translation is detected
- **WHEN** a user-facing text exists in English but not in Dutch
- **THEN** the project's lint check fails
