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

### Requirement: Topic icon
A topic SHALL support at most one icon from a bundled set of at least 50 icons, shown before its text in the topic's
text colour.

#### Scenario: Add icon
- **WHEN** the user gives a topic the star icon
- **THEN** a star is shown to the left of the topic text

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
