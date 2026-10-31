# Spec Delta

## Purpose

Defines how the user moves around a map on screen in the editor.

## ADDED Requirements

### Requirement: Pan and zoom
The user SHALL be able to pan the map with one finger and zoom with a pinch gesture, between 20 % and
400 %.

#### Scenario: Pinch to zoom
- **WHEN** the user pinches outwards on the map
- **THEN** the map zooms in around the pinch point, up to 400 %

### Requirement: Fit to screen
When a map is opened, and when the user selects "Fit to screen", the whole map SHALL be shown centred
on the screen.

#### Scenario: Open a map
- **WHEN** a map is opened in the editor
- **THEN** the complete map is visible
