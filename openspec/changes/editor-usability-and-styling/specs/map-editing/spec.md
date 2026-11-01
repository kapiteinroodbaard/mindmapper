# Spec Delta

## MODIFIED Requirements

### Requirement: Select a topic
Tapping a topic SHALL select it and show the actions for that topic; tapping empty space SHALL clear the
selection.

#### Scenario: Tap a topic
- **WHEN** the user taps a topic
- **THEN** the topic is highlighted and its actions are shown on the map next to the topic

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

## ADDED Requirements

### Requirement: Topic actions on the map
The actions for the selected topic SHALL be shown on the map, attached to that topic, and SHALL follow
it when the map is panned or zoomed. A "+" handle on the topic's outer side SHALL add a subtopic. A "+"
handle below a topic other than the central topic SHALL add a topic after it. An action bar next to the
topic SHALL offer edit text, image, icon and, except on the central topic, delete.

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

### Requirement: Add a main branch on a chosen side
In the radial layout, the selected central topic SHALL show a "+" handle on both its left and right
side; the new main branch SHALL be placed on the side whose handle was used. In the right layout only
the right handle SHALL be shown.

#### Scenario: Add on the left
- **WHEN** the map uses the radial layout and the user taps the left "+" handle of the central topic
- **THEN** a new main branch is added on the left side and its text editor opens

#### Scenario: Right layout
- **WHEN** the map uses the right layout and the central topic is selected
- **THEN** only the right "+" handle is shown

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
