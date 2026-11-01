# Spec Delta

## MODIFIED Requirements

### Requirement: Topic text
Every topic SHALL show its text, wrapped over multiple lines when it exceeds the topic's maximum width.
In the central topic every line SHALL be centred horizontally in the shape.

#### Scenario: Long text
- **WHEN** a topic's text is longer than fits on one line
- **THEN** the text wraps onto further lines and the topic grows in height

#### Scenario: Long central topic text
- **WHEN** the central topic's text wraps onto several lines
- **THEN** the text block is centred in the central topic, with equal space to its left and right

## ADDED Requirements

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
