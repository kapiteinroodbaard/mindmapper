# Spec Delta

## MODIFIED Requirements

### Requirement: Style is dropped when a main branch becomes a subtopic
When a main branch is moved to a deeper level, its own style and its custom colour SHALL be removed, and it
SHALL be drawn in the style and colour of its new main branch. Moving a main branch to another position or
side among the main branches SHALL keep its style and custom colour.

#### Scenario: Main branch moved under a topic
- **WHEN** the user drags a plant main branch onto a topic of a standard branch and later drags it back to the central topic
- **THEN** it is drawn as standard under the other branch, and as standard again after moving it back

#### Scenario: Reorder keeps the style
- **WHEN** the user swaps a lightning main branch with its neighbour
- **THEN** it keeps the lightning style in its new position

#### Scenario: Custom colour dropped
- **WHEN** the user drags a main branch with a custom colour onto a subtopic of another branch
- **THEN** it is drawn in the colour of that other branch

### Requirement: Choosing a branch style
Choosing the style action on a main topic SHALL open a style picker with one preview tile per branch
style, drawn the way that style looks in the branch's own colour and without text labels. The current
style SHALL be marked. Picking a tile SHALL apply that style at once, and the change SHALL be undoable.
The same picker SHALL offer the branch's colour: automatic or a colour from a colour selector, applied at
once and undoable.

#### Scenario: Pick a style
- **WHEN** the user selects a main topic, chooses its style action and taps the lightning tile
- **THEN** the branch is redrawn in the lightning style and the lightning tile is marked as current

#### Scenario: Undo
- **WHEN** the user changes a branch's style and then taps undo
- **THEN** the branch is drawn in its previous style again

#### Scenario: Pick a branch colour
- **WHEN** the user picks green in the branch style picker
- **THEN** the branch and the preview tiles are redrawn in green
