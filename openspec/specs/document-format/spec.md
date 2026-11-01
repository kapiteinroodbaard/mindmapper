# document-format Specification

## Purpose
Defines the `.mindmap` file: a self-contained container holding the mind map and all of its images, in
an open, versioned format that other tools can read.

## Requirements

### Requirement: Self-contained container
A `.mindmap` file SHALL be a ZIP archive containing `map.json` with the complete mind map, every image
used by the map under `images/`, and a `thumbnail.png` preview of the map.

#### Scenario: Save a map with images
- **WHEN** a map with two topic images is saved
- **THEN** the resulting file is a ZIP that contains `map.json`, both images under `images/` and `thumbnail.png`

#### Scenario: Copy the file to another device
- **WHEN** the `.mindmap` file is copied to another device with the app and opened there
- **THEN** the map and all its images are shown, without needing any other file

### Requirement: Format version
`map.json` SHALL contain a `formatVersion` number. The app SHALL open files with a version it supports
and SHALL refuse files with a higher version than it supports, with a message asking to update the app.

#### Scenario: Open a file from a newer app version
- **WHEN** the user opens a file whose `formatVersion` is higher than the app supports
- **THEN** the app does not open it and shows a message that a newer version of the app is needed

### Requirement: Stored map content
`map.json` SHALL store the layout kind, the central topic's shape and colour, the trunk width and taper,
and for every topic its text, its bold, italic and underline formatting when set, icon, image (reference,
display width and aspect ratio), the side of a main branch when the user chose one, and its child topics
in order.

#### Scenario: Round trip
- **WHEN** a map is saved and opened again
- **THEN** the layout, central shape and colour, trunk width and taper, and every topic's text, formatting, icon, image, image size, side and child order are identical to before saving

### Requirement: Invalid files are rejected safely
The app SHALL reject a file that is not a valid `.mindmap` container (not a ZIP, no `map.json`, invalid
JSON, or a missing referenced image) with a clear message and SHALL NOT modify that file.

#### Scenario: Open a random ZIP
- **WHEN** the user opens a ZIP file without `map.json`
- **THEN** the app shows "This is not a Mindmapper file" and the file is left unchanged

#### Scenario: Open a damaged file
- **WHEN** the user opens a file whose `map.json` cannot be parsed
- **THEN** the app shows that the file is damaged and stays on the start screen

### Requirement: Compatible map content additions
The trunk settings and topic formatting SHALL be optional in `map.json`, so files without them open with
the defaults, and adding them SHALL NOT raise `formatVersion`. Out-of-range trunk values SHALL be
clamped to the allowed range when a file is read.

#### Scenario: Open a file from an earlier version
- **WHEN** the user opens a file saved before trunk settings and formatting existed
- **THEN** the map opens with trunk width 12, taper 1 and default formatting

#### Scenario: Out-of-range trunk width
- **WHEN** a file stores a trunk width of 100
- **THEN** the map opens with trunk width 40
