# map-background Specification

## Purpose
Defines a map's own background: a colour and/or an image chosen per map, how it is shown on screen and
in exports, and the map appearance that goes with it.

## Requirements

### Requirement: Default background follows the theme
A map without a background colour and without a background image SHALL be drawn on the light or dark
map background that matches the app theme, and SHALL switch along when the theme changes.

#### Scenario: New map in dark mode
- **WHEN** the system is in dark mode and the user creates a new map
- **THEN** the map is drawn on the dark map background

#### Scenario: Theme changes
- **WHEN** a map without its own background is open and the system switches from light to dark mode
- **THEN** the map background turns dark

### Requirement: Background colour
The user SHALL be able to give a map a background colour from a colour selector and to remove it again.
A map with a background colour SHALL be drawn on that colour in light and in dark mode.

#### Scenario: Fixed colour
- **WHEN** the user sets a map's background to light yellow and the system switches to dark mode
- **THEN** the map is still drawn on light yellow

#### Scenario: Back to automatic
- **WHEN** the user removes the background colour of a map without a background image
- **THEN** the map background follows the app theme again

### Requirement: Background image
The user SHALL be able to give a map a background image picked from the device and to remove it again.
The image SHALL cover the whole visible map area while keeping its aspect ratio, cropping what does not
fit, and SHALL stay in place when the map is panned or zoomed. A background colour, when also set, SHALL
show only where the image is transparent.

#### Scenario: Pan over a background image
- **WHEN** a map has a background image and the user pans and zooms the map
- **THEN** the topics move and the background image stays where it is

#### Scenario: Image in dark mode
- **WHEN** a map has a background image and the system switches between light and dark mode
- **THEN** the background image is unchanged

### Requirement: Appearance fits the background
On a map with its own background, the default text colour and the branch palette SHALL be those of the
light appearance when the background is light and of the dark appearance when it is dark, independent of
the app theme. For an image, its average brightness SHALL decide.

#### Scenario: Dark background colour in light mode
- **WHEN** the system is in light mode and a map's background colour is dark blue
- **THEN** subtopic text on that map is drawn light

#### Scenario: Bright photo in dark mode
- **WHEN** the system is in dark mode and a map's background image is a bright photo
- **THEN** subtopic text on that map is drawn dark

### Requirement: Background in presentation mode
Presentation mode SHALL show the map with the same background as the editor.

#### Scenario: Present a map with a background image
- **WHEN** the user presents a map that has a background image
- **THEN** the presentation shows that background image behind the map
