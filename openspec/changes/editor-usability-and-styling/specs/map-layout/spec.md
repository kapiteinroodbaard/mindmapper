# Spec Delta

## MODIFIED Requirements

### Requirement: Radial layout
In the radial layout, main branches SHALL be placed on the right and left of the central topic in
clockwise order starting at the top right: down the right side, then up the left side. Branches without
a chosen side SHALL be divided so the stacked height of both sides, including the spacing between the
branches, is as equal as possible while keeping that order.

#### Scenario: Four equal branches
- **WHEN** a radial map has four main branches with equal subtrees
- **THEN** branches 1 and 2 are on the right (1 above 2) and branches 3 and 4 on the left (4 above 3)

#### Scenario: One large branch
- **WHEN** branch 1 has ten subtopics and branches 2, 3 and 4 have none
- **THEN** branch 1 is alone on the right and branches 2–4 are on the left

#### Scenario: Many small branches
- **WHEN** branch 1 has twelve subtopics and is followed by twelve main branches without subtopics
- **THEN** some of the small branches are placed on the right below branch 1, so the two sides differ in height by at most a quarter

### Requirement: Chosen side
A main branch with a chosen side SHALL be placed on that side in the radial layout, and the remaining
branches SHALL be balanced around it. The right layout SHALL ignore chosen sides. Switching the map's
layout kind SHALL clear the chosen side of every main branch.

#### Scenario: Branch moved to the left
- **WHEN** the user has placed branch 1 on the left side
- **THEN** branch 1 is drawn on the left and the other branches are rebalanced

#### Scenario: Switch back to radial
- **WHEN** a radial map with chosen sides is switched to the right layout and back to radial
- **THEN** no branch has a chosen side and all branches are balanced as if newly added
