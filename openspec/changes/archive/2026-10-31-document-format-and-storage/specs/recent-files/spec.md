# Spec Delta

## Purpose

Lets the user reopen recently used mind maps from the start screen in one tap, like the recent-files
list in Office.

## ADDED Requirements

### Requirement: Recent files list
The start screen SHALL list recently opened or saved documents, most recent first, each with its file
name, a thumbnail of the map and when it was last used. The list SHALL keep at most 15 unpinned entries.

#### Scenario: Reopen a recent file
- **WHEN** the user taps an entry in the recent files list
- **THEN** that document opens in the editor

#### Scenario: List is full
- **WHEN** a 16th unpinned document is opened
- **THEN** the least recently used unpinned entry is removed from the list

### Requirement: Pinning
The user SHALL be able to pin and unpin entries. Pinned entries SHALL be shown above the others and
SHALL NOT be removed automatically.

#### Scenario: Pin an entry
- **WHEN** the user pins an entry
- **THEN** it moves to the pinned group at the top and stays there regardless of how many files are opened

### Requirement: Inaccessible entries
When a recent document can no longer be opened (moved, deleted or access revoked), the app SHALL show a
message explaining that the file is no longer available and offer to remove it from the list.

#### Scenario: File was moved
- **WHEN** the user taps an entry whose file was moved elsewhere
- **THEN** the app shows that the file is no longer available and offers "Remove from list"

### Requirement: Remove entries
The user SHALL be able to remove any entry from the list without affecting the file itself.

#### Scenario: Remove an entry
- **WHEN** the user removes an entry from the list
- **THEN** the entry disappears and the file on storage is unchanged
