# Spec Delta

## MODIFIED Requirements

### Requirement: Tapered branches
Branches SHALL be thickest where they leave the central topic and get thinner at every level, so a branch
is never thicker than the branch it grows from. In the standard branch style they SHALL be drawn as smooth
curves; other branch styles SHALL keep this tapering in their own line shape.

#### Scenario: Three levels
- **WHEN** a map has main topics with subtopics and sub-subtopics
- **THEN** the lines to sub-subtopics are thinner than those to subtopics, which are thinner than the main branches

#### Scenario: Tapering in another style
- **WHEN** a main branch with subtopics uses the lightning style
- **THEN** its zigzag lines to subtopics are thinner than its main zigzag line
