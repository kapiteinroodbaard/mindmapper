# Spec Delta

## Purpose

Lets each main branch of a map have its own visual style, so branches can be told apart by the way they
are drawn as well as by their position, level and colour.

## ADDED Requirements

### Requirement: Branch styles
A main branch SHALL have one of the branch styles standard, blurred, plant, lightning and straight. A main
branch without a chosen style SHALL be drawn as standard. A style SHALL determine, as one choice, the line
to the main topic, the shape of the main topic's box, and the lines and underlines of every topic in that
branch.

#### Scenario: New branch
- **WHEN** the user adds a main branch
- **THEN** it is drawn in the standard style

#### Scenario: One choice changes line and box
- **WHEN** the user sets a main branch to the lightning style
- **THEN** both the line to that main topic and the main topic's box change to the lightning look

### Requirement: Style applies to the whole branch
All topics under a main branch SHALL be drawn in the style of that main branch; a subtopic SHALL NOT have
a style of its own. Moving a topic under another main branch SHALL draw it in that branch's style.

#### Scenario: Subtree follows the main branch
- **WHEN** a main branch with subtopics and sub-subtopics is set to the plant style
- **THEN** the lines and underlines of all its subtopics and sub-subtopics are drawn in the plant style

#### Scenario: Move to another branch
- **WHEN** the user drags a subtopic from a standard branch onto a topic in a lightning branch
- **THEN** the subtopic and its own subtopics are drawn in the lightning style

### Requirement: Style is dropped when a main branch becomes a subtopic
When a main branch is moved to a deeper level, its own style SHALL be removed, and it SHALL be drawn in
the style of its new main branch. Moving a main branch to another position or side among the main
branches SHALL keep its style.

#### Scenario: Main branch moved under a topic
- **WHEN** the user drags a plant main branch onto a topic of a standard branch and later drags it back to the central topic
- **THEN** it is drawn as standard under the other branch, and as standard again after moving it back

#### Scenario: Reorder keeps the style
- **WHEN** the user swaps a lightning main branch with its neighbour
- **THEN** it keeps the lightning style in its new position

### Requirement: Standard style
The standard style SHALL draw smooth tapered curves, a rounded rectangle around the main topic and straight
underlines under subtopics, as the map looked before branch styles existed.

#### Scenario: Unchanged look
- **WHEN** a map saved before branch styles existed is opened
- **THEN** all its branches look as they did before

### Requirement: Blurred style
The blurred style SHALL draw branches and underlines with a soft, blurred edge and the main topic's box as
a rounded rectangle with a soft glow. Topic text, icons and images SHALL stay sharp.

#### Scenario: Text stays sharp
- **WHEN** a main branch is set to the blurred style
- **THEN** its line and box have soft edges while its text is as sharp as in the standard style

### Requirement: Plant style
The plant style SHALL draw the main branch as a smooth tapered curve with leaves and curling tendrils
along its middle part, alternating sides, sized to the branch's width at that point. The main topic's box
SHALL be leaf shaped. Lines to subtopics SHALL carry no tendrils, and subtopic underlines SHALL end in a
small leaf.

#### Scenario: Decorations stay clear of topics
- **WHEN** a main branch is set to the plant style
- **THEN** leaves and tendrils are drawn along the branch but not over the central topic or the main topic

#### Scenario: Subtopics
- **WHEN** a plant branch has subtopics
- **THEN** each subtopic's underline ends in a small leaf and the lines to subtopics have no tendrils

### Requirement: Lightning style
The lightning style SHALL draw branches as a tapered zigzag, the main topic's box as a parallelogram and
subtopic underlines as a zigzag.

#### Scenario: Lightning branch
- **WHEN** a main branch is set to the lightning style
- **THEN** the line to it is a zigzag that gets thinner towards the main topic, and its box is a parallelogram

### Requirement: Straight style
The straight style SHALL draw every branch as a straight tapered line from its start to its end point, the
main topic's box as a rectangle with cut-off corners, and straight underlines under subtopics.

#### Scenario: Straight branch
- **WHEN** a main branch is set to the straight style
- **THEN** the lines to it and to its subtopics are straight and its box has cut-off corners

### Requirement: Stable decorations
Irregular parts of a style, such as the zigzag of lightning and the leaves and tendrils of plant, SHALL
depend only on the topic and its position, so they look the same every time the map is drawn and do not
change when an unrelated topic is edited.

#### Scenario: Unrelated edit
- **WHEN** the user edits the text of a topic in another branch
- **THEN** the zigzag of a lightning branch looks exactly as before

### Requirement: Choosing a branch style
Choosing the style action on a main topic SHALL open a style picker with one preview tile per branch
style, drawn the way that style looks in the branch's own colour and without text labels. The current
style SHALL be marked. Picking a tile SHALL apply that style at once, and the change SHALL be undoable.

#### Scenario: Pick a style
- **WHEN** the user selects a main topic, chooses its style action and taps the lightning tile
- **THEN** the branch is redrawn in the lightning style and the lightning tile is marked as current

#### Scenario: Undo
- **WHEN** the user changes a branch's style and then taps undo
- **THEN** the branch is drawn in its previous style again

### Requirement: Styles in exports
Branch styles SHALL look the same on screen, in PDF and PNG exports, in the presentation and in the file
thumbnail.

#### Scenario: Export a styled map
- **WHEN** a map with a blurred branch and a plant branch is exported to PDF
- **THEN** both branches in the PDF look as they do on screen

### Requirement: Styles in light and dark appearance
Every branch style SHALL be clearly visible in both the light and the dark appearance.

#### Scenario: Dark mode
- **WHEN** a map with a blurred branch is shown in dark mode
- **THEN** the blurred branch and its glow are clearly visible against the dark background
