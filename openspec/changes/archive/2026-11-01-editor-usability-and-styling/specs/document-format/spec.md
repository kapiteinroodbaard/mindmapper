# Spec Delta

## MODIFIED Requirements

### Requirement: Stored map content
`map.json` SHALL store the layout kind, the central topic's shape and colour, the trunk width and taper,
and for every topic its text, its bold, italic and underline formatting when set, icon, image (reference,
display width and aspect ratio), the side of a main branch when the user chose one, and its child topics
in order.

#### Scenario: Round trip
- **WHEN** a map is saved and opened again
- **THEN** the layout, central shape and colour, trunk width and taper, and every topic's text, formatting, icon, image, image size, side and child order are identical to before saving

## ADDED Requirements

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
