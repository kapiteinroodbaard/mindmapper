# Design

## Context

`Topic` already carries fields that only matter on main branches (`side`, `style`), and `MapEdits.move`
clears them when a topic leaves the root. Topic sizes come from `TopicMetrics.measure` through
`MapRenderer.layout(map)`. `EditorViewModel` computes one layout for its state, and the editor and
`PresentationScreen` both draw with it. `MapExporter` calls `renderer.layout(map)` and `renderer.draw`
itself for PDF, PNG and the thumbnail. The blurred branch style already draws a glow around a main box
(`drawGlow`, per-appearance alphas in `MapTheme`).

`PresentationScreen` keeps its view state (`focusedId`, `viewport`) in local `remember` state and shares
`EditorViewModel` with the editor. The requirements are in `specs/presentation-timing/spec.md`.

## Goals / Non-Goals

**Goals:**
- One source of truth: durations on main branches. Total and slots are always derived.
- Durations change the layout in the editor and presentation in the same way, so both look alike.
- A timer that keeps correct time while paused, after rotation and when the app redraws rarely.

**Non-Goals:**
- Durations on subtopics, a separately set total limit, seconds in the duration input.
- Alarms, vibration or sound when a slot or the total ends.
- Keeping the timer across leaving presentation mode, app restarts or process death.
- Detecting where the presenter actually is (for example from the focused topic).

## Decisions

### Model: optional `durationSeconds` on `Topic`

`Topic.durationSeconds: Int?`, null when not set, so `explicitNulls = false` leaves it out of
`map.json`. Seconds in the file keep room for finer input later; the UI uses whole minutes (60 to 10800
seconds). No lenient decoding or version change: the app is not in use yet (see proposal).

*Alternative:* store minutes. Rejected: seconds are the natural unit for the timer and cost nothing.

`MapEdits.setDuration(id, minutes: Int?)` sets it on main branches only and stores null for null or 0.
`MapEdits.move` clears `durationSeconds` where it already clears `side` and `style`. Like `side`,
`addSiblingAfter` creates a new topic without a duration.

### Schedule as a pure core function

A `core` function turns a map into a `Schedule`: an ordered list of `(topicId, startSeconds,
endSeconds)` for main branches with a duration, plus `totalSeconds`. A second function returns the
scheduled topic id for an elapsed time (`start <= t < end`), or null at or beyond the total. Both are
plain Kotlin and unit tested in `core`; the renderer and the presentation screen only consume them.

### Duration line: part of the topic content, optional per layout

`TopicMetrics.content` gets an optional duration label: for a main branch its own minutes, for the
central topic the total, nothing otherwise. The label is a second, smaller text line (same colour as the
topic text, regular weight, about 75% of the level's text size) below the text, and is included in
`contentHeight`, so `measure` and the layout make room for it. In the central topic it is centred like
the text.

`MapRenderer.layout(map, showDurations: Boolean = true)` passes the flag to measuring; `draw` derives it
from the layout so measuring and drawing cannot disagree (the flag is stored on `MapLayout` or the
renderer draws the label only when the measured box made room for it). `MapExporter` calls it with
`false` for PDF, PNG and thumbnail. Text layouts for the label are cached like the topic text.

*Alternative:* draw the duration as a badge outside the box. Rejected: it would collide with branches
and neighbours, since the layout does not know about it.

### Highlight: a `highlightId` parameter on `MapRenderer.draw`

`draw` gets `highlightId: String? = null`. For that main box it draws a glow in the branch colour behind
the box, reusing `drawGlow` with a highlight-specific, stronger set of alphas in `MapTheme` (light and
dark), so it is visible on a blurred branch too. `MapCanvas` passes it through; only
`PresentationScreen` sets it.

*Alternative:* an icon on the scheduled topic. Rejected in exploration: a glow is visible at overview
zoom and does not change the layout.

### Timer state in `EditorViewModel`, based on the monotonic clock

A small `PresentationTimer` state holder (elapsed seconds accumulated before the current run, plus the
`SystemClock.elapsedRealtime()` at which the current run started, or null when paused) lives in
`EditorViewModel`, so it survives rotation. Elapsed time is computed from the clock, never by counting
ticks, so it stays correct when frames are skipped. `PresentationScreen` resets it when it is first shown
(entering presentation mode) and not on recomposition after rotation; reset is keyed on entering, for
example from the navigation callback `onPresent`.

The screen re-reads the elapsed time with a `LaunchedEffect` loop that updates about every 200 ms while
running, which is enough to show seconds and move the highlight on time.

### Timer pill UI

A Compose pill at the top start corner, opposite the existing buttons. Tap toggles running, long press
resets (`combinedClickable`). Text: `m:ss` below an hour, `h:mm:ss` from an hour on, followed by the
scheduled branch's text, truncated to one line. Over time the pill uses the error colour from the
Material theme and shows no name. All strings in English and Dutch.

## Risks / Trade-offs

- [The extra line makes main boxes taller, which shifts the layout in the editor compared with exports]
  -> Accepted: exports are meant without timing; the layout algorithm handles any box size.
- [Branch style box shapes (lens, parallelogram) are sized from content] -> The duration line is part of
  the content, so `BranchGeometry.mainBoxSize` already fits it; checked with the existing fit test.
- [Reset on entering might also fire on recreation of the screen] -> Reset from the action that opens
  presentation mode, not from the composable's first composition; covered by a rotation test.
- [Glow highlight hard to see on a blurred branch] -> Separate, stronger highlight alphas; checked on
  the emulator in light and dark mode.
