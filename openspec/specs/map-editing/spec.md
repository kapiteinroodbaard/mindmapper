# map-editing Specification

## Purpose
Defines how the user changes a mind map's content and structure with touch in the editor.

## Requirements

### Requirement: Select a topic
Tapping a topic SHALL select it and show the actions for that topic; tapping empty space SHALL clear the
selection.

#### Scenario: Tap a topic
- **WHEN** the user taps a topic
- **THEN** the topic is highlighted and its actions are shown on the map next to the topic

### Requirement: Add topics
With a topic selected, the user SHALL be able to add a child topic and, for any topic except the
central topic, a sibling topic directly after it. The new topic SHALL be selected with its text open for
editing.

#### Scenario: Add a child
- **WHEN** the central topic is selected and the user adds a child
- **THEN** a new main branch appears as the last main branch and its text editor opens

### Requirement: Edit text
Double-tapping a topic, or choosing "Edit text", SHALL open its text for editing; confirming SHALL update
the topic and the layout.

#### Scenario: Rename a topic
- **WHEN** the user double-taps a topic, types new text and confirms
- **THEN** the topic shows the new text

### Requirement: Delete topics
The user SHALL be able to delete any topic except the central topic; its subtopics SHALL be deleted
with it.

#### Scenario: Delete a branch
- **WHEN** the user deletes a main topic with three subtopics
- **THEN** the main topic and its three subtopics are removed

### Requirement: Image and icon
The user SHALL be able to add, replace and remove a topic's image, change the image width, and set or
remove the topic's icon from the icon picker.

#### Scenario: Add an image
- **WHEN** the user chooses "Image" on a topic and picks a photo
- **THEN** the photo is shown in the topic

### Requirement: Map style settings
The user SHALL be able to change the central topic's shape and colour and switch between radial and
right layout. The central topic's style SHALL be opened from a style action on the selected central
topic, not from the editor's overflow menu.

#### Scenario: Change colour
- **WHEN** the user picks a new colour for the central topic
- **THEN** the central topic is redrawn in that colour

#### Scenario: Open the style from the central topic
- **WHEN** the user selects the central topic and chooses its style action
- **THEN** the style dialog for shape, colour and trunk opens, and the overflow menu has no style item

### Requirement: Drag to restructure
Long-pressing and dragging a topic (not the central topic) SHALL move it with its subtree. Dropping it
on the middle of another topic SHALL make it that topic's last child; dropping it on the upper or lower
edge of a topic SHALL place it before or after that topic as a sibling. A topic SHALL NOT be dropped
onto itself or its own descendants.

#### Scenario: Reparent
- **WHEN** the user drags topic A onto the middle of topic B
- **THEN** A and its subtopics become the last children of B

#### Scenario: Reorder
- **WHEN** the user drags a topic onto the upper edge of its sibling
- **THEN** the topic moves directly before that sibling

#### Scenario: Invalid drop
- **WHEN** the user drops a topic onto one of its own subtopics
- **THEN** nothing changes

### Requirement: Move main branch to the other side
In the radial layout, dropping a main branch before or after a main branch on the other side, or onto
the central topic on the other side, SHALL put it on that side.

#### Scenario: Move to left
- **WHEN** the user drags a right-side main branch onto the left half of the central topic
- **THEN** the branch is placed on the left side

### Requirement: Topic actions on the map
The actions for the selected topic SHALL be shown on the map, attached to that topic, and SHALL follow
it when the map is panned or zoomed. A "+" handle on the topic's outer side SHALL add a subtopic. A "+"
handle below a topic other than the central topic SHALL add a topic after it. An action bar next to the
topic SHALL offer edit text, image, icon, a style action on the central topic and on main topics, and,
except on the central topic, delete.

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
- **THEN** its action bar offers edit text, image, icon, style and delete, with delete last

#### Scenario: Subtopic actions
- **WHEN** the user selects a subtopic
- **THEN** its action bar offers edit text, image, icon and delete, and no style action

### Requirement: Add a main branch on a chosen side
In the radial layout, the selected central topic SHALL show a "+" handle on both its left and right
side; the new main branch SHALL be placed on the side whose handle was used. In the right layout only
the right handle SHALL be shown. A main branch added as a topic after another main branch SHALL NOT take
over that branch's side; it SHALL be placed by the automatic balancing.

#### Scenario: Add on the left
- **WHEN** the map uses the radial layout and the user taps the left "+" handle of the central topic
- **THEN** a new main branch is added on the left side and its text editor opens

#### Scenario: Right layout
- **WHEN** the map uses the right layout and the central topic is selected
- **THEN** only the right "+" handle is shown

#### Scenario: Topics after a main branch are balanced
- **WHEN** the user adds a main branch with the right "+" handle and then adds nine topics after it
- **THEN** the nine new branches have no chosen side and the branches are divided over both sides

### Requirement: Topic toolbar holds only remaining actions
Actions shown on the map SHALL NOT also be shown in the editor's bottom toolbar. The toolbar SHALL be
hidden while it has no actions.

#### Scenario: Toolbar hidden
- **WHEN** a topic is selected and every topic action is shown on the map
- **THEN** no bottom toolbar is shown

### Requirement: Text formatting
The text editor of a topic SHALL offer bold, italic and underline toggles that apply to the whole text
of that topic. The toggles SHALL show the topic's current formatting, including bold as the default for
the central topic and main branches. Changing the formatting SHALL be undoable.

#### Scenario: Make a subtopic bold and italic
- **WHEN** the user opens the text editor of a subtopic, turns on bold and italic and confirms
- **THEN** the subtopic's text is drawn bold and italic

#### Scenario: Turn off default bold
- **WHEN** the user turns off bold for a main branch and confirms
- **THEN** the main branch's text is drawn in regular weight

### Requirement: Rename the map
The editor's top bar SHALL show a pencil action next to the map name. It SHALL open a dialog with the
current name, which is the file name without the `.mindmap` extension. The name SHALL NOT be empty and
SHALL NOT contain characters that are invalid in file names.

#### Scenario: Open the rename dialog
- **WHEN** the user taps the pencil next to the map name "Plan"
- **THEN** a dialog opens with the name "Plan" ready for editing

#### Scenario: Invalid name
- **WHEN** the user clears the name or types a "/" in it
- **THEN** the dialog cannot be confirmed
