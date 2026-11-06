# Tasks

## 1. Model, format and schedule (core)

- [x] 1.1 Add optional `Topic.durationSeconds`; verify with `MindMapArchiveTest` that a duration round-trips, that a branch without a duration has no `durationSeconds` key in `map.json`, and that `formatVersion` stays 1
- [x] 1.2 Add `MapEdits.setDuration` (main branches only, null or 0 removes, minutes stored as seconds) and clear the duration in `move` when a main branch leaves the root while keeping it on reorder or side change; verify with `MapEditsTest`, including that a new topic added after a timed main branch has no duration
- [x] 1.3 Add the schedule function (slots in main branch order, branches without duration skipped, total) and the scheduled-topic lookup for an elapsed time (null at or beyond the total); verify with unit tests for the A/B/C/D example from the spec, a reorder, no durations and the exact slot boundaries

## 2. Durations on the map (app)

- [x] 2.1 Add the duration label to `TopicMetrics.content` (own duration on main topics, total on the central topic, centred there, smaller regular text under the topic text) and include it in the measured size; verify with a `RenderTest` that a timed main topic and the central topic are taller than without durations and that branch-style boxes still contain their padded content
- [x] 2.2 Add `showDurations` to `MapRenderer.layout` and draw the label only when the layout includes it; make `MapExporter` lay out without durations for PDF, PNG and thumbnail; verify with `MapExporterTest`/`RenderTest` that an export of a timed map has the same bounds as the same map without durations
- [x] 2.3 Add English and Dutch strings for the duration label ("5 min"); verify with `LocalizationTest` that both languages have every new string

## 3. Setting a duration (app)

- [x] 3.1 Show the duration action on main topics before delete in `TopicActions`, not on subtopics or the central topic; verify with a Robolectric UI test for the action bars of a main topic, a subtopic and the central topic
- [x] 3.2 Add the duration dialog (numeric field prefilled with the current minutes, empty or 0 removes, outside 1 to 180 disables confirm) and `EditorViewModel.setDuration` as one undoable edit; verify with an `EditorViewModelTest` for set, remove and undo, and a UI test that 181 cannot be confirmed

## 4. Timer and highlight in presentation (app)

- [x] 4.1 Add `highlightId` to `MapRenderer.draw` and `MapCanvas`, drawing a glow in the branch colour with highlight alphas in `MapTheme` for light and dark; verify with a `RenderTest` that drawing with a highlight on a standard and a blurred branch succeeds and only changes pixels around that main topic
- [x] 4.2 Add the presentation timer state to `EditorViewModel` (accumulated time plus start of the current run on the monotonic clock; start, pause, resume, reset), reset when presentation mode is entered; verify with an `EditorViewModelTest` using a fake clock for pause/resume, reset and that a second entry starts at 0:00
- [x] 4.3 Add the timer pill to `PresentationScreen` (shown only when the schedule is not empty, tap toggles, long press resets, `m:ss`/`h:mm:ss`, scheduled branch text, red and nameless over time) and pass the scheduled topic as `highlightId`; verify with a Robolectric UI test that a map without durations has no timer and that tapping starts it, and with a test that rotation keeps the running timer
- [x] 4.4 Update the README features with presentation timing; verify the README lists it in English as the other features

## 5. Integration

- [x] 5.1 Run `./gradlew check` and verify it passes
- [x] 5.2 On the emulator, verify the full flow in light and dark mode: set durations on three of four main branches, check the lines and the total, undo, reorder, present, start the timer, see the glow move between slots, pause, rotate, go past the total (red, no glow), leave and re-enter (reset), and export a PDF without duration lines
