# map-styling Specification

## Purpose
Defines the visual style of a map: the central topic's shape and colour, branch colours and thickness,
and the light and dark appearance.

## Requirements

### Requirement: Central topic shape and colour
The central topic SHALL support the shapes circle, ellipse, rectangle, rounded rectangle, diamond,
hexagon and capsule, and a colour chosen from a colour selector. Its text SHALL be drawn in a colour
that contrasts with the fill.

#### Scenario: Change shape
- **WHEN** the user selects the diamond shape for the central topic
- **THEN** the central topic is drawn as a diamond containing its text

### Requirement: Automatic branch colours
Each main branch SHALL get a colour based on its position in the clockwise order, from a fixed palette
ordered so that neighbouring branches contrast clearly. In the radial layout the first and last branch
are also neighbours and SHALL NOT get the same colour. All topics and branches under a main branch SHALL
use its colour. Reordering branches SHALL reassign colours.

#### Scenario: Neighbours differ
- **WHEN** a map has any number of main branches
- **THEN** no two neighbouring main branches have the same colour

#### Scenario: Reorder branches
- **WHEN** the user swaps the positions of branch 2 and branch 3
- **THEN** each takes the colour belonging to its new position

### Requirement: Tapered branches
Branches SHALL be drawn as smooth curves that are thickest where they leave the central topic and get
thinner at every level, so a branch is never thicker than the branch it grows from.

#### Scenario: Three levels
- **WHEN** a map has main topics with subtopics and sub-subtopics
- **THEN** the lines to sub-subtopics are thinner than those to subtopics, which are thinner than the main branches

### Requirement: Light and dark appearance
The map SHALL use a light appearance (light background, dark text) when the app is in light mode and a
dark appearance when the app is in dark mode, with palette colours adjusted for good contrast in each.

#### Scenario: Dark mode
- **WHEN** the system is in dark mode and a map is opened
- **THEN** the map background is dark and topic text is light

### Requirement: Configurable trunk
Each map SHALL have a trunk width, the width of the main branches where they leave the central topic,
between 5 and 40 units (default 12), and a trunk taper between 1 and 5 (default 1). A main branch SHALL
keep its full width up to the edge of the central topic and then thin to its end width; a higher taper
SHALL make it thin out sooner. Both SHALL be set in the central topic's style dialog.

#### Scenario: Thicker trunk
- **WHEN** the user sets the trunk width to 30
- **THEN** every main branch is 30 units wide at the edge of the central topic and still ends at its usual end width

#### Scenario: Faster taper
- **WHEN** the user raises the trunk taper from 1 to 4
- **THEN** the main branches reach a thin width closer to the central topic than before

#### Scenario: Same in exports
- **WHEN** a map with a custom trunk is exported to PDF or PNG
- **THEN** the branches in the export have the same width and taper as on screen
