# Spec Delta

## Purpose

Helps the presenter keep a talk within its time: a planned duration per main branch, their total, and a
count-up timer in presentation mode that highlights the main branch the presenter should be at.

## ADDED Requirements

### Requirement: Planned duration per main branch
A main branch SHALL have an optional planned duration in whole minutes from 1 to 180. A new main branch
SHALL have no duration. Subtopics and the central topic SHALL NOT have a duration of their own.

#### Scenario: New branch
- **WHEN** the user adds a main branch
- **THEN** it has no planned duration

#### Scenario: Subtopic
- **WHEN** the user selects a subtopic
- **THEN** no duration can be set on it

### Requirement: Setting a duration
Choosing the duration action on a main topic SHALL open a dialog to enter the duration in minutes,
prefilled with the current duration. Confirming an empty value or 0 SHALL remove the duration. Values
outside 1 to 180 SHALL NOT be accepted. Setting or removing a duration SHALL be one undoable change.

#### Scenario: Set a duration
- **WHEN** the user chooses the duration action on a main topic, enters 5 and confirms
- **THEN** the main topic has a planned duration of 5 minutes

#### Scenario: Remove a duration
- **WHEN** the user opens the duration dialog of a main topic with a duration, clears the value and confirms
- **THEN** the main topic has no planned duration

#### Scenario: Undo
- **WHEN** the user changes a main topic's duration from 5 to 8 minutes and taps undo
- **THEN** the main topic has a duration of 5 minutes again

### Requirement: Duration is dropped when a main branch becomes a subtopic
When a main branch is moved to a deeper level, its duration SHALL be removed. Moving a main branch to
another position or side among the main branches SHALL keep its duration.

#### Scenario: Main branch moved under a topic
- **WHEN** the user drags a main branch with a duration of 5 minutes onto a topic of another branch
- **THEN** the moved topic has no duration, also after dragging it back to the central topic

#### Scenario: Reorder keeps the duration
- **WHEN** the user swaps a main branch with a duration with its neighbour
- **THEN** it keeps its duration in its new position

### Requirement: Duration shown on the map
A main topic with a duration SHALL show it as an extra line under its text, such as "5 min". The central
topic SHALL show the total of all main branch durations the same way, and no line when no main branch has
a duration. The extra line SHALL be part of the topic's size, so the layout makes room for it.

#### Scenario: Durations and total
- **WHEN** a map has main branches with 3, 5 and 8 minutes and one main branch without a duration
- **THEN** those three main topics show "3 min", "5 min" and "8 min" under their text, the fourth shows no extra line and the central topic shows "16 min"

#### Scenario: No durations
- **WHEN** no main branch has a duration
- **THEN** no topic shows a duration line

### Requirement: Durations only for the presenter
Durations and the total SHALL be shown in the editor and in presentation mode, and SHALL NOT be shown in
PDF and PNG exports or in the file thumbnail.

#### Scenario: Export a timed map
- **WHEN** a map with planned durations is exported to PDF
- **THEN** the PDF shows the map without any duration lines

### Requirement: Schedule
The main branches with a duration SHALL get consecutive time slots in the order of the main branches,
starting at 0:00, each as long as its duration. Main branches without a duration SHALL get no slot. The
end of the last slot SHALL equal the total.

#### Scenario: Slots
- **WHEN** main branches A (3 min), B (no duration), C (5 min) and D (8 min) are in that order
- **THEN** A is scheduled from 0:00 to 3:00, C from 3:00 to 8:00, D from 8:00 to 16:00, and B has no slot

#### Scenario: Reorder changes the schedule
- **WHEN** the user swaps A and C in that map
- **THEN** C is scheduled from 0:00 to 5:00 and A from 5:00 to 8:00

### Requirement: Presentation timer
Presentation mode SHALL show a timer when at least one main branch has a duration, and no timer
otherwise. The timer SHALL count up from 0:00 and SHALL start running only when the presenter taps it. A
tap SHALL pause or resume it and a long press SHALL reset it to 0:00, paused. Elapsed time SHALL be shown
as minutes and seconds, with hours added from one hour on.

#### Scenario: Start the timer
- **WHEN** the presenter starts presentation mode on a map with durations and taps the timer
- **THEN** the timer, which showed 0:00, starts counting up

#### Scenario: Pause and resume
- **WHEN** the running timer shows 2:10 and the presenter taps it, waits a minute and taps it again
- **THEN** it showed 2:10 during the pause and continues from 2:10

#### Scenario: Reset
- **WHEN** the presenter long-presses the timer at 6:12
- **THEN** it shows 0:00 and is paused

#### Scenario: Map without durations
- **WHEN** the presenter starts presentation mode on a map without durations
- **THEN** no timer is shown

### Requirement: Timer lifetime
The timer SHALL keep its elapsed time and running state when the screen rotates and while the presenter
zooms, pans or focuses on topics. Leaving presentation mode SHALL reset it.

#### Scenario: Rotate while running
- **WHEN** the timer is running at 4:00 and the device is rotated
- **THEN** the timer keeps running from 4:00

#### Scenario: Present again
- **WHEN** the presenter leaves presentation mode with the timer at 7:30 and starts presenting again
- **THEN** the timer shows 0:00 and is paused

### Requirement: Scheduled branch
While the elapsed time is within a main branch's time slot, that main branch SHALL be the scheduled
branch: it SHALL be highlighted on the map with a glow in its own branch colour, and the timer SHALL show
its text next to the elapsed time. Only presentation mode SHALL show the highlight.

#### Scenario: Highlight follows the clock
- **WHEN** the timer passes 3:00 in the map with A (0:00-3:00) and C (3:00-8:00)
- **THEN** the glow moves from A to C and the timer shows C's text

#### Scenario: Focused on another branch
- **WHEN** the presenter is focused on branch D while the elapsed time is in C's slot
- **THEN** the timer still shows C's text

#### Scenario: Paused at 0:00
- **WHEN** the timer shows 0:00 and has not been started
- **THEN** the first main branch with a duration is highlighted

### Requirement: Over time
Once the elapsed time reaches the total, the timer SHALL keep counting, SHALL be shown in red and SHALL
show no branch name, and no main branch SHALL be highlighted.

#### Scenario: Past the total
- **WHEN** the total is 16 minutes and the timer reaches 16:05
- **THEN** the timer shows 16:05 in red without a branch name and no branch glows
