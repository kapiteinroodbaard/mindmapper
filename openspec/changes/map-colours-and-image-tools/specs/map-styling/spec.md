# Spec Delta

## MODIFIED Requirements

### Requirement: Automatic branch colours
Each main branch SHALL get a colour based on its position in the clockwise order, from a fixed palette
ordered so that neighbouring branches contrast clearly. In the radial layout the first and last branch
are also neighbours and SHALL NOT get the same colour. All topics and branches under a main branch SHALL
use its colour. Reordering branches SHALL reassign colours. This palette scheme SHALL be the default
branch colour scheme of a map; a main branch with a custom colour SHALL NOT take part in it.

#### Scenario: Neighbours differ
- **WHEN** a map has any number of main branches
- **THEN** no two neighbouring main branches have the same colour

#### Scenario: Reorder branches
- **WHEN** the user swaps the positions of branch 2 and branch 3
- **THEN** each takes the colour belonging to its new position

### Requirement: Light and dark appearance
The map SHALL use a light appearance (light background, dark text) when the app is in light mode and a
dark appearance when the app is in dark mode, with palette colours adjusted for good contrast in each.
This SHALL apply only to maps without a background of their own; a map with its own background SHALL use
the appearance that fits that background.

#### Scenario: Dark mode
- **WHEN** the system is in dark mode and a map is opened
- **THEN** the map background is dark and topic text is light

## ADDED Requirements

### Requirement: Shades colour scheme
A map SHALL offer a second automatic branch colour scheme, shades, as an alternative to the palette.
With shades, the main branches SHALL get shades of the central topic's colour, running evenly from light
to dark in clockwise order, and SHALL follow when the central topic's colour changes.

#### Scenario: Red central topic
- **WHEN** a map with a red central topic and four main branches uses the shades scheme
- **THEN** the four branches are drawn in four different shades of red, the first in clockwise order the lightest and the last the darkest

#### Scenario: Central colour changes
- **WHEN** the map uses shades and the user changes the central topic's colour from red to green
- **THEN** the branches are redrawn in shades of green

### Requirement: Custom branch colour
The user SHALL be able to give a main branch a colour of its own from a colour selector, and to set it
back to automatic. A main branch with a custom colour SHALL be drawn in that colour, with all topics and
branches under it, in either colour scheme and in light and dark appearance.

#### Scenario: Custom colour
- **WHEN** the user gives a main branch a custom purple colour
- **THEN** that branch and all its subtopics are drawn purple, and the other branches keep their automatic colours

#### Scenario: Back to automatic
- **WHEN** the user sets that branch's colour back to automatic
- **THEN** it is drawn in the automatic colour of its position again
