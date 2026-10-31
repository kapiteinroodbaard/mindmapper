# presentation Specification

## Purpose
Defines the full-screen, read-only view the presenter uses as a guide while giving a presentation.

## Requirements

### Requirement: Full-screen presentation
The editor SHALL offer "Present", which shows the map full screen with the system bars hidden and no
editing controls, starting with the whole map in view.

#### Scenario: Start presenting
- **WHEN** the user selects "Present" in the editor
- **THEN** the map fills the screen, the status and navigation bars are hidden and the whole map is visible

### Requirement: Focus on a topic
Tapping a topic in presentation mode SHALL animate the view so that topic and its subtopics fill the
screen as far as possible.

#### Scenario: Tap a main branch
- **WHEN** the presenter taps a main topic
- **THEN** the view smoothly zooms to that topic with all its subtopics visible

### Requirement: Back to overview
Tapping empty space or selecting the overview button SHALL animate back to the whole map. The back
gesture SHALL return to the overview when focused, and leave presentation mode when already at the
overview.

#### Scenario: Back gesture while focused
- **WHEN** the presenter is focused on a topic and uses the back gesture
- **THEN** the whole map is shown again

#### Scenario: Back gesture at overview
- **WHEN** the presenter is at the overview and uses the back gesture
- **THEN** the editor is shown again

### Requirement: Read-only
Presentation mode SHALL NOT allow any change to the map; pan and pinch zoom SHALL remain available.

#### Scenario: Long-press in presentation
- **WHEN** the presenter long-presses or double-taps a topic
- **THEN** the map is not changed
