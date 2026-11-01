# Spec Delta

## MODIFIED Requirements

### Requirement: Stored map content
`map.json` SHALL store the layout kind, the central topic's shape and colour, the trunk width and taper,
and for every topic its text, its bold, italic and underline formatting when set, icon, image (reference,
display width and aspect ratio), the side of a main branch when the user chose one, the style of a main
branch when it is not standard, and its child topics in order.

#### Scenario: Round trip
- **WHEN** a map is saved and opened again
- **THEN** the layout, central shape and colour, trunk width and taper, and every topic's text, formatting, icon, image, image size, side, branch style and child order are identical to before saving

### Requirement: Compatible map content additions
The trunk settings, topic formatting and branch styles SHALL be optional in `map.json`, so files without
them open with the defaults, and adding them SHALL NOT raise `formatVersion`. Out-of-range trunk values
SHALL be clamped to the allowed range when a file is read. An unknown branch style value SHALL be read as
the standard style instead of rejecting the file.

#### Scenario: Open a file from an earlier version
- **WHEN** the user opens a file saved before trunk settings, formatting and branch styles existed
- **THEN** the map opens with trunk width 12, taper 1, default formatting and every branch in the standard style

#### Scenario: Out-of-range trunk width
- **WHEN** a file stores a trunk width of 100
- **THEN** the map opens with trunk width 40

#### Scenario: Unknown branch style
- **WHEN** a file stores a branch style this version of the app does not know
- **THEN** the map opens and that branch is drawn in the standard style
