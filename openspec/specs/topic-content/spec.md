# topic-content Specification

## Purpose
Defines what a topic can contain (text, an image and an icon) and how that content determines the
topic's size on the map.

## Requirements

### Requirement: Topic text
Every topic SHALL show its text, wrapped over multiple lines when it exceeds the topic's maximum width.
In the central topic every line SHALL be centred horizontally in the shape.

#### Scenario: Long text
- **WHEN** a topic's text is longer than fits on one line
- **THEN** the text wraps onto further lines and the topic grows in height

#### Scenario: Long central topic text
- **WHEN** the central topic's text wraps onto several lines
- **THEN** the text block is centred in the central topic, with equal space to its left and right

### Requirement: Topic image
A topic SHALL support at most one image, shown above its text at a display width the user can change between 48 and
480 units while keeping the image's aspect ratio.

#### Scenario: Resize image
- **WHEN** the user changes a topic's image width from 160 to 320
- **THEN** the image is drawn twice as wide and high, and the layout makes room for it

#### Scenario: Resize beyond the limits
- **WHEN** the user drags an image's corner handle far outward
- **THEN** the image stops growing at a width of 480 units

### Requirement: Topic icon
A topic SHALL support at most one icon from a bundled set of at least 50 icons, shown before its text in the topic's
text colour. The set SHALL include ten numbered icons: a filled circle containing a digit, for each digit
from 0 to 9.

#### Scenario: Add icon
- **WHEN** the user gives a topic the star icon
- **THEN** a star is shown to the left of the topic text

#### Scenario: Number a topic
- **WHEN** the user gives a topic the numbered icon 3
- **THEN** a filled circle with the digit 3 is shown to the left of the topic text

### Requirement: Text formatting
A topic's text SHALL be drawable bold, italic, underlined, or any combination, for the whole text of the
topic. Without a choice, the central topic and main branches SHALL be bold and other topics regular,
neither italic nor underlined. The topic's size SHALL follow the formatted text.

#### Scenario: Underlined subtopic
- **WHEN** a subtopic is set to underline
- **THEN** its text is drawn underlined in addition to the branch line beneath the topic

#### Scenario: Default formatting
- **WHEN** a new topic is added without changing its formatting
- **THEN** it is bold if it is a main branch and regular if it is a subtopic

### Requirement: Text colour
A topic's text SHALL be drawable in a colour the user chooses. Without a choice the text SHALL be drawn in
the automatic colour: a colour that contrasts with the fill for the central topic and main topics, and
the map appearance's text colour for subtopics. The topic's icon SHALL be drawn in the same colour as its
text.

#### Scenario: Red subtopic text
- **WHEN** the user sets a subtopic's text colour to red
- **THEN** its text and its icon are drawn in red

#### Scenario: Back to automatic
- **WHEN** the user sets a main topic's text colour back to automatic
- **THEN** its text is drawn again in the colour that contrasts with its box
