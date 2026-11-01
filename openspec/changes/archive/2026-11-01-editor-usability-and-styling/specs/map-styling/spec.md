# Spec Delta

## MODIFIED Requirements

### Requirement: Tapered branches
Branches SHALL be drawn as smooth curves that are thickest where they leave the central topic and get
thinner at every level, so a branch is never thicker than the branch it grows from.

#### Scenario: Three levels
- **WHEN** a map has main topics with subtopics and sub-subtopics
- **THEN** the lines to sub-subtopics are thinner than those to subtopics, which are thinner than the main branches

## ADDED Requirements

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
