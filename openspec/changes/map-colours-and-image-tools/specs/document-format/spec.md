# Spec Delta

## MODIFIED Requirements

### Requirement: Stored map content
`map.json` SHALL store the layout kind, the central topic's shape and colour, the trunk width and taper,
the branch colour scheme when it is not the palette, the background colour and background image when set,
and for every topic its text, its bold, italic and underline formatting when set, its text colour when
set, icon, image (reference, display width and aspect ratio), the side of a main branch when the user
chose one, the style of a main branch when it is not standard, the custom colour of a main branch when it
has one, the planned duration of a main branch in seconds when it has one, and its child topics in order.

#### Scenario: Round trip
- **WHEN** a map is saved and opened again
- **THEN** the layout, central shape and colour, trunk width and taper, colour scheme, background colour and image, and every topic's text, formatting, text colour, icon, image, image size, side, branch style, branch colour, duration and child order are identical to before saving

#### Scenario: Branch without a duration
- **WHEN** a map with a main branch without a duration is saved
- **THEN** `map.json` stores no duration for that branch, and it has no duration after opening the file again

#### Scenario: Background image stored in the container
- **WHEN** a map with a background image is saved
- **THEN** the image is stored in the container next to the topic images, and it is removed from the container once the background image is removed and the map is saved again

### Requirement: Compatible map content additions
The trunk settings, topic formatting, branch styles, text colours, branch colours, colour scheme and
background SHALL be optional in `map.json`, so files without them open with the defaults, and adding them
SHALL NOT raise `formatVersion`. Out-of-range trunk values SHALL be clamped to the allowed range when a
file is read. An unknown branch style value SHALL be read as the standard style, and an unknown colour
scheme as the palette, instead of rejecting the file.

#### Scenario: Open a file from an earlier version
- **WHEN** the user opens a file saved before trunk settings, formatting and branch styles existed
- **THEN** the map opens with trunk width 12, taper 1, default formatting and every branch in the standard style

#### Scenario: Out-of-range trunk width
- **WHEN** a file stores a trunk width of 100
- **THEN** the map opens with trunk width 40

#### Scenario: Unknown branch style
- **WHEN** a file stores a branch style this version of the app does not know
- **THEN** the map opens and that branch is drawn in the standard style

#### Scenario: Open a file without colours or background
- **WHEN** the user opens a file saved before text colours, branch colours and backgrounds existed
- **THEN** the map opens with automatic text and branch colours, the palette scheme and a background that follows the app theme
