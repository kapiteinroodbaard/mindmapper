# Spec Delta

## MODIFIED Requirements

### Requirement: Stored map content
`map.json` SHALL store the layout kind, the central topic's shape and colour, the trunk width and taper,
and for every topic its text, its bold, italic and underline formatting when set, icon, image (reference,
display width and aspect ratio), the side of a main branch when the user chose one, the style of a main
branch when it is not standard, the planned duration of a main branch in seconds when it has one, and
its child topics in order.

#### Scenario: Round trip
- **WHEN** a map is saved and opened again
- **THEN** the layout, central shape and colour, trunk width and taper, and every topic's text, formatting, icon, image, image size, side, branch style, duration and child order are identical to before saving

#### Scenario: Branch without a duration
- **WHEN** a map with a main branch without a duration is saved
- **THEN** `map.json` stores no duration for that branch, and it has no duration after opening the file again
