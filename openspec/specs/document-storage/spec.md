# document-storage Specification

## Purpose
Defines how mind maps are created, opened and saved in locations the user chooses, Office style, and how
images enter a document.

## Requirements

### Requirement: New document
The start screen SHALL offer "New mind map", which opens the editor with a map containing only a
central topic and no file location yet.

#### Scenario: Create a new map
- **WHEN** the user selects "New mind map"
- **THEN** the editor opens with a single central topic and the document is marked as never saved

### Requirement: Open document
The start screen SHALL offer "Open", which lets the user pick a file with the system file picker and
opens it in the editor.

#### Scenario: Open a file from the picker
- **WHEN** the user selects "Open" and picks a valid `.mindmap` file
- **THEN** the editor shows that map and the file is added to the recent files

### Requirement: Save and save as
The editor SHALL offer "Save" and "Save as". "Save" writes to the document's current file; when the
document has no file yet, or the app may no longer write to it, "Save" SHALL behave as "Save as", which
asks for a location and file name with the system file picker, suggesting the `.mindmap` extension.

#### Scenario: First save of a new map
- **WHEN** the user selects "Save" on a never-saved map
- **THEN** the system file picker asks for a location and name, and the map is written there

#### Scenario: Save as a copy
- **WHEN** the user selects "Save as" on a saved map and picks a new name
- **THEN** the map is written to the new file and further saves go to the new file

### Requirement: Autosave
When autosave is enabled (the default) and the document has a writable file, the app SHALL save the
document automatically shortly after each change and when the app goes to the background. A
never-saved document SHALL NOT be autosaved. Autosave SHALL be switchable in the editor menu.

#### Scenario: Change a saved map
- **WHEN** autosave is on, the map has been saved once, and the user edits a topic
- **THEN** the file is updated within a few seconds without user action

#### Scenario: Change a never-saved map
- **WHEN** the user edits a map that was never saved
- **THEN** nothing is written until the user saves explicitly

### Requirement: Unsaved changes prompt
When the user leaves the editor while the document has unsaved changes, the app SHALL ask whether to
save, discard the changes, or stay in the editor.

#### Scenario: Leave with unsaved changes
- **WHEN** the user presses back in the editor with unsaved changes
- **THEN** a dialog offers Save, Discard and Cancel

### Requirement: Embedded images
Images SHALL be chosen with the system photo picker, without any storage permission. Before embedding,
an image larger than 1600 pixels on its longest side SHALL be downscaled to that size and compressed.

#### Scenario: Insert a large photo
- **WHEN** the user adds a 4000×3000 photo to a topic
- **THEN** the image stored in the file is at most 1600 pixels on its longest side

### Requirement: Open from other apps
The app SHALL offer itself to open `.mindmap` files from file managers and other apps (view and share
actions), and SHALL check that the file really is a Mindmapper file before opening it. Choosing the app
as the default handler is left to the system's "Always" choice.

#### Scenario: Open from a file manager
- **WHEN** the user taps a `.mindmap` file in a file manager and chooses Mindmapper
- **THEN** the map opens in the editor

#### Scenario: Read-only access from another app
- **WHEN** a file opened from another app cannot be written
- **THEN** "Save" asks for a new location instead of failing
