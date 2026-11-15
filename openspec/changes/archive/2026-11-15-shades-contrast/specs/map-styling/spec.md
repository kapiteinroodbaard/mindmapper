# Spec Delta

## MODIFIED Requirements

### Requirement: Shades colour scheme
A map SHALL offer a second automatic branch colour scheme, shades, as an alternative to the palette.
With shades, the main branches SHALL get shades of the central topic's colour, running evenly from light
to dark in clockwise order, and SHALL follow when the central topic's colour changes. The range of shades
SHALL depend on the background the map is drawn on: lighter shades on a dark background and darker shades
on a light background, so that every shade has a contrast ratio of at least 1.5:1 with that background.

#### Scenario: Red central topic
- **WHEN** a map with a red central topic and four main branches uses the shades scheme
- **THEN** the four branches are drawn in four different shades of red, the first in clockwise order the lightest and the last the darkest

#### Scenario: Central colour changes
- **WHEN** the map uses shades and the user changes the central topic's colour from red to green
- **THEN** the branches are redrawn in shades of green

#### Scenario: Dark background
- **WHEN** a map with a blue central topic and four main branches uses shades on a dark navy background
- **THEN** the darkest branch still has a contrast ratio of at least 1.5:1 with the navy background, and the four shades are lighter than the same map's shades on white

#### Scenario: White background
- **WHEN** the same map is drawn on white
- **THEN** the lightest branch has a contrast ratio of at least 1.5:1 with white

#### Scenario: App theme switches
- **WHEN** a map without a background of its own uses shades and the system switches from light to dark mode
- **THEN** the branches are redrawn in the lighter range that fits the dark background
