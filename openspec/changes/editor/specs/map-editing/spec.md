# Spec Delta

## Purpose

Defines how the user changes a mind map's content and structure with touch in the editor.

## ADDED Requirements

### Requirement: Select a topic
Tapping a topic SHALL select it and show the actions for that topic; tapping empty space SHALL clear the
selection.

#### Scenario: Tap a topic
- **WHEN** the user taps a topic
- **THEN** the topic is highlighted and topic actions are available

### Requirement: Add topics
With a topic selected, the user SHALL be able to add a child topic and, for any topic except the
central topic, a sibling topic directly after it. The new topic SHALL be selected with its text open for
editing.

#### Scenario: Add a child
- **WHEN** the central topic is selected and the user adds a child
- **THEN** a new main branch appears as the last main branch and its text editor opens

### Requirement: Edit text
Double-tapping a topic, or choosing "Edit text", SHALL open its text for editing; confirming SHALL update
the topic and the layout.

#### Scenario: Rename a topic
- **WHEN** the user double-taps a topic, types new text and confirms
- **THEN** the topic shows the new text

### Requirement: Delete topics
The user SHALL be able to delete any topic except the central topic; its subtopics SHALL be deleted
with it.

#### Scenario: Delete a branch
- **WHEN** the user deletes a main topic with three subtopics
- **THEN** the main topic and its three subtopics are removed

### Requirement: Image and icon
The user SHALL be able to add, replace and remove a topic's image, change the image width, and set or
remove the topic's icon from the icon picker.

#### Scenario: Add an image
- **WHEN** the user chooses "Image" on a topic and picks a photo
- **THEN** the photo is shown in the topic

### Requirement: Map style settings
The user SHALL be able to change the central topic's shape and colour and switch between radial and
right layout.

#### Scenario: Change colour
- **WHEN** the user picks a new colour for the central topic
- **THEN** the central topic is redrawn in that colour

### Requirement: Drag to restructure
Long-pressing and dragging a topic (not the central topic) SHALL move it with its subtree. Dropping it
on the middle of another topic SHALL make it that topic's last child; dropping it on the upper or lower
edge of a topic SHALL place it before or after that topic as a sibling. A topic SHALL NOT be dropped
onto itself or its own descendants.

#### Scenario: Reparent
- **WHEN** the user drags topic A onto the middle of topic B
- **THEN** A and its subtopics become the last children of B

#### Scenario: Reorder
- **WHEN** the user drags a topic onto the upper edge of its sibling
- **THEN** the topic moves directly before that sibling

#### Scenario: Invalid drop
- **WHEN** the user drops a topic onto one of its own subtopics
- **THEN** nothing changes

### Requirement: Move main branch to the other side
In the radial layout, dropping a main branch before or after a main branch on the other side, or onto
the central topic on the other side, SHALL put it on that side.

#### Scenario: Move to left
- **WHEN** the user drags a right-side main branch onto the left half of the central topic
- **THEN** the branch is placed on the left side
