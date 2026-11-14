# Spec Delta

## MODIFIED Requirements

### Requirement: Image and icon
The user SHALL be able to add and remove a topic's image, change the image width, and set or remove the
topic's icon from the icon picker. Removing and resizing an image SHALL be done with tools on the image
itself, not in a dialog.

#### Scenario: Add an image
- **WHEN** the user chooses "Image" on a topic and picks a photo
- **THEN** the photo is shown in the topic

### Requirement: Map style settings
The user SHALL be able to change the central topic's shape and colour, the map's branch colour scheme
(palette or shades) and the map's background colour and image, and switch between radial and right
layout. The central topic's style SHALL be opened from a style action on the selected central topic, not
from the editor's overflow menu.

#### Scenario: Change colour
- **WHEN** the user picks a new colour for the central topic
- **THEN** the central topic is redrawn in that colour

#### Scenario: Open the style from the central topic
- **WHEN** the user selects the central topic and chooses its style action
- **THEN** the style dialog for shape, colour, branch colour scheme, background and trunk opens, and the overflow menu has no style item

#### Scenario: Choose a background image
- **WHEN** the user chooses a background image in the central topic's style dialog and picks a photo
- **THEN** the map is drawn on that photo

### Requirement: Topic actions on the map
The actions for the selected topic SHALL be shown on the map, attached to that topic, and SHALL follow
it when the map is panned or zoomed. A "+" handle on the topic's outer side SHALL add a subtopic. A "+"
handle below a topic other than the central topic SHALL add a topic after it. An action bar next to the
topic SHALL offer edit text, image (only while the topic has no image), icon, a style action on the
central topic and on main topics, a duration action on main topics, and, except on the central topic,
delete.

#### Scenario: Add a subtopic with the handle
- **WHEN** the user selects a right-side main branch and taps the "+" handle on its right
- **THEN** a new subtopic is added as its last child and its text editor opens

#### Scenario: Add a topic after with the handle
- **WHEN** the user selects a subtopic and taps the "+" handle below it
- **THEN** a new topic is inserted directly after it and its text editor opens

#### Scenario: Actions follow the topic
- **WHEN** a topic is selected and the user pans the map
- **THEN** its handles and action bar move with the topic

#### Scenario: Topic out of view
- **WHEN** the selected topic is panned completely out of view
- **THEN** its handles and action bar are not shown

#### Scenario: Main topic actions
- **WHEN** the user selects a main topic
- **THEN** its action bar offers edit text, image, icon, style, duration and delete, with delete last

#### Scenario: Subtopic actions
- **WHEN** the user selects a subtopic
- **THEN** its action bar offers edit text, image, icon and delete, and no style or duration action

#### Scenario: Central topic actions
- **WHEN** the user selects the central topic
- **THEN** its action bar offers no duration action

#### Scenario: Topic with an image
- **WHEN** the user selects a topic that has an image
- **THEN** its action bar offers no image action, and the image tools are shown on the image

### Requirement: Text formatting
The text editor of a topic SHALL offer bold, italic and underline toggles that apply to the whole text
of that topic. The toggles SHALL show the topic's current formatting, including bold as the default for
the central topic and main branches. Changing the formatting SHALL be undoable. The text editor SHALL
also offer the text colour: automatic or a colour from a colour selector, with the current choice marked.

#### Scenario: Make a subtopic bold and italic
- **WHEN** the user opens the text editor of a subtopic, turns on bold and italic and confirms
- **THEN** the subtopic's text is drawn bold and italic

#### Scenario: Turn off default bold
- **WHEN** the user turns off bold for a main branch and confirms
- **THEN** the main branch's text is drawn in regular weight

#### Scenario: Colour the text
- **WHEN** the user opens the text editor of a topic, picks blue as text colour and confirms
- **THEN** the topic's text is drawn blue, and undo restores the previous colour

## ADDED Requirements

### Requirement: Image tools on the map
While a topic with an image is selected, the image SHALL show a delete button on the image and a round
handle on each of its four corners. Tapping the delete button SHALL remove the image. Dragging a corner
handle SHALL resize the image while keeping its aspect ratio, between 48 and 480 units wide, with the
map updating during the drag. Each removal and each completed drag SHALL be one undoable step.

#### Scenario: Remove with the delete button
- **WHEN** the user selects a topic with an image and taps the delete button on the image
- **THEN** the image is removed from the topic, and undo brings it back

#### Scenario: Resize with a corner handle
- **WHEN** the user drags the bottom-right handle of a selected topic's image outward and releases it
- **THEN** the image grows while dragging, keeps its aspect ratio, and a single undo returns it to its previous size

#### Scenario: Not selected
- **WHEN** a topic with an image is not selected
- **THEN** its image shows no delete button and no handles
