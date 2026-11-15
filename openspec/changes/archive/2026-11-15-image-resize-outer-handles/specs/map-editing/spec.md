# Spec Delta

## MODIFIED Requirements

### Requirement: Image tools on the map
While a topic with an image is selected, the image SHALL show a delete button on the image and a round
handle on each of its outer corners: the right corners for a topic right of the central topic, the left
corners for a topic left of it, and all four corners for the central topic. Tapping the delete button SHALL
remove the image. Dragging a handle SHALL resize the image while keeping its aspect ratio, between 48 and 480
units wide, with the map updating during the drag and the handle following the finger horizontally. Each
removal and each completed drag SHALL be one undoable step.

#### Scenario: Remove with the delete button
- **WHEN** the user selects a topic with an image and taps the delete button on the image
- **THEN** the image is removed from the topic, and undo brings it back

#### Scenario: Resize with a corner handle
- **WHEN** the user drags the bottom-right handle of a selected right-side topic's image outward and releases it
- **THEN** the image grows while dragging, keeps its aspect ratio, and a single undo returns it to its previous size

#### Scenario: Not selected
- **WHEN** a topic with an image is not selected
- **THEN** its image shows no delete button and no handles

#### Scenario: Handles on the outer side only
- **WHEN** the user selects a topic left of the central topic that has an image
- **THEN** its image shows handles on its top-left and bottom-left corners and none on its right corners

#### Scenario: Handle stays under the finger
- **WHEN** the user drags an outer handle 100 pixels outward and holds still
- **THEN** the handle ends horizontally within a few pixels of the finger, also when the topic's text is wider than its image

#### Scenario: Central topic image
- **WHEN** the user selects the central topic with an image
- **THEN** its image shows handles on all four corners, and dragging any of them outward grows the image
