# Proposal

## Why

Mindmapper is used as the presenter's guide, but it gives no help with timing: a presenter cannot see
how long each part of the talk should take or whether they are on schedule. Planning a duration per main
branch while preparing, and seeing during the talk which branch the clock says they should be at, lets
the presenter keep the talk within its time.

## What Changes

- Each main branch can have a planned duration in whole minutes, set with a new duration action in the
  main topic's action bar. A main branch without a duration is not part of the schedule.
- The duration is shown as an extra line under the main topic's text ("5 min"). The central topic shows
  the total of all durations the same way. There is no separate total to set: the total is always the sum.
- Durations and the total are shown in the editor and in presentation mode, but not in PDF and PNG
  exports or the file thumbnail.
- The main branches with a duration get consecutive time slots in the order of the main branches.
- Presentation mode shows a count-up timer when at least one main branch has a duration. It starts at
  0:00 and starts running when tapped; a tap pauses or resumes it and a long press resets it. It shows
  the elapsed time and the name of the main branch whose slot contains the elapsed time.
- That scheduled main branch is highlighted on the map with a glow in its own branch colour. Past the
  total, the timer keeps counting, turns red, and no branch is highlighted.
- When a main branch is moved to a deeper level its duration is dropped, like its branch style.
- `map.json` stores the duration of a main branch as an optional field in seconds. The app is not yet
  in use, so no compatibility with older files is needed and `formatVersion` stays 1.

## Capabilities

### New Capabilities
- `presentation-timing`: planned durations per main branch and their total, how they are shown, and the
  count-up timer with the highlighted scheduled branch in presentation mode.

### Modified Capabilities
- `map-editing`: the action bar of a main topic also offers the duration action.
- `document-format`: the stored map content includes the duration of a main branch.

## Impact

- `core`: `Topic` model (optional `durationSeconds`), `MapEdits` (set duration, drop it on a move to a
  deeper level), a schedule helper that turns durations into time slots, `MindMapArchive` round trip,
  tests.
- `app`: `TopicMetrics` and `MapRenderer` (duration line under the text, total on the central topic,
  optional, off for exports and thumbnail; glow highlight of a main branch), `TopicActions` and
  `EditorDialogs` (duration action and dialog), `EditorViewModel` (undoable set duration, presentation
  timer state that survives rotation), `PresentationScreen` (timer pill and highlight), strings in
  English and Dutch.
- No new dependencies; no network or permissions.
