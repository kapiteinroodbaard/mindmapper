# Spec Delta

## MODIFIED Requirements

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
