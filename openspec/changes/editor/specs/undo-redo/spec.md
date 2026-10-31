# Spec Delta

## Purpose

Lets the user revert and reapply changes to the map, so experimenting is safe.

## ADDED Requirements

### Requirement: Undo and redo
Every change to the map SHALL be undoable and redoable, for at least the last 100 changes in the
current editing session. Making a new change after undoing SHALL discard the redo history.

#### Scenario: Undo a delete
- **WHEN** the user deletes a branch and selects Undo
- **THEN** the branch and its subtopics return in their original place

#### Scenario: Redo
- **WHEN** the user undoes a change and then selects Redo
- **THEN** the change is applied again

#### Scenario: New change clears redo
- **WHEN** the user undoes a change and then edits a topic
- **THEN** Redo is no longer available
