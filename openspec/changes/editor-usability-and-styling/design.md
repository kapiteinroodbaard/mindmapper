# Design

## Context

See proposal.md for motivation and the delta specs for the required behaviour. Relevant current state:

- The model (`core`) is immutable; every edit goes through `EditHistory`, so new edits are undoable for free.
- `MapLayouter.balanceSides` tries every split point of the clockwise order and minimises the difference
  in summed subtree heights, but ignores the 28-unit gap between main branches. A `side` chosen by
  dragging is kept on the topic in every layout and is never cleared.
- `TopicMetrics` builds one `StaticLayout` per (text, depth) with width = the widest unwrapped line,
  capped at the maximum width, centred for the central topic. The box is then measured from
  `getLineWidth`, so wrapped lines (narrower than the layout width) are drawn shifted right by half the
  difference.
- `MapRenderer.ribbon` tapers linearly from the topic centre; main branches use 12 → 5 units.
- The editor shows a bottom `TopicToolbar` for the selected topic; style lives in the overflow menu.
- The title is derived from the display name of the document's URI.

## Goals / Non-Goals

**Goals:**
- One renderer for screen, PDF and PNG keeps working; all new styling goes through `TopicMetrics` and
  `MapRenderer`.
- Files stay readable by the previous app version.

**Non-Goals:**
- Formatting parts of a text (per-word rich text).
- A per-branch trunk setting; the trunk is per map.
- Animations for the on-map actions.

## Decisions

### Text formatting as three optional topic fields
`Topic` gets `bold: Boolean?`, `italic: Boolean?` and `underline: Boolean?`. `bold == null` means the
level's default (bold at depth 0 and 1), so existing files and new topics keep today's look and a main
branch can still be made regular. `italic`/`underline` are stored as `null` when off so `map.json` stays
small (`explicitNulls = false`). Alternative: a markup syntax inside `text` — rejected because formatting
is per object and markup would leak into exports' file names and the text editor.

`TopicMetrics` caches paints and layouts by `(depth, Format)`; `Content` exposes the layout's paint so the
renderer colours the right paint instead of a per-depth one.

### Centring by ink extent
Measure the text with `min(getLineLeft)` / `max(getLineRight)` instead of `getLineWidth`, and shift the
drawing origin by `-inkLeft`. This centres wrapped lines in the box without changing the layout width
(which would make boxes wider than their text). Non-central topics are left-aligned, so `inkLeft` is 0
there and nothing changes.

### Balancing counts gaps; layout switch clears sides
`balanceSides` adds `verticalGap(1) * (count - 1)` to each side before comparing, matching what `stack`
actually draws. `setLayout` clears `side` on all main branches when the kind changes. Alternative:
ignore chosen sides only after a switch from the right layout — rejected: it needs extra state and the
user experiences "switching" as starting over.

### Trunk: width and taper per map, tapering from the shape edge
`MindMap` gets `trunkWidth` (5–40, default 12) and `trunkTaper` (1–5, default 1). The ribbon for a main
branch keeps `trunkWidth` while the curve is inside the central topic's box, then interpolates
`w1 + (w0 - w1) * (1 - p)^taper` over the rest (p = progress after the exit point). Starting the taper at
the edge makes the chosen width visible; with taper 1 it remains linear. End widths and deeper levels
are unchanged, so a branch is still never thicker than its parent. Values are clamped on edit and on
read.

### On-map actions as a Compose overlay
The actions are Compose buttons in an overlay above the `MapCanvas`, positioned from the selected box's
rect converted with the current `Viewport` (`screen = layout * scale + offset`). Alternatives: drawing
handles in the Canvas and hit-testing them — rejected, it duplicates button behaviour, would leak into
PDF/PNG and needs its own touch handling.

```
             +-------------------------------+
             | [Aa] [img] [*] [palette] [x]  |   action bar: above, or below when no room
             +-------------------------------+
   (+) <--  [        Selected topic        ]  --> (+)   subtopic handle(s), outer side
                           (+)                          topic-after handle (not on centre)
```

- The overlay is laid out with a custom placement modifier that centres each element on an anchor point
  and clamps it inside the canvas. It is not shown when the topic's rect lies fully outside the canvas
  and is hidden while a drag is in progress.
- Overlay elements only consume touches on the buttons themselves; the canvas keeps pan, zoom, tap and
  drag elsewhere.
- Every button keeps the content description of the old toolbar button ("Add subtopic", "Add topic
  after", …), so existing labels and translations are reused. The central-topic style button uses the
  existing "Central topic style" string; the left/right handles share "Add subtopic".
- `TopicToolbar` stays as a composable taking a list of actions and renders nothing for an empty list.
- `addChild` gets an optional side for main branches, used by the central topic's left/right handles.

### Format toggles in the text dialog
The text dialog gets three toggle buttons (B, I, U) showing the effective formatting, and the text field
previews it. On confirm, text and formatting are applied as one edit so undo reverts both together.
`bold` is stored as `null` when it equals the level default.

### Rename through the document provider
`DocumentRepository.rename(uri, name)` uses `DocumentsContract.renameDocument` for document URIs and
`File.renameTo` for `file:` URIs (tests), returning the new URI or null. On success the editor switches
to the new URI, re-takes persistable permission, replaces the old recent-files entry and updates the
title. A never-saved document only stores the name in editor state, which already feeds the "Save as"
suggestion. When the URI is not writable or the provider refuses, the editor shows "Renaming is not
possible here. Use Save as to save under a new name." Invalid names: empty after trimming or containing
any of `\ / : * ? " < > |` or a newline.

## Risks / Trade-offs

- [Some providers return a new URI after renaming and drop the old permission] → take a persistable
  permission on the returned URI and remove the old recent entry.
- [Overlay overlaps neighbouring topics on dense maps] → the bar is small and only shown for the
  selection; the user can pan or zoom. Placement flips below when there is no room above.
- [Overlay position lags a frame behind fast pans] → positions are derived from the same viewport state
  the canvas draws with, so they update in the same composition.
- [Robolectric cannot exercise real document providers] → rename success/failure logic is tested with
  `file:` URIs and a read-only case; provider behaviour is verified on the emulator.

## Migration Plan

No migration: new `map.json` fields are optional with defaults, older apps ignore them
(`ignoreUnknownKeys`), and `formatVersion` stays 1. Rollback is reverting the change; files written by
the new version still open in the old one, losing only formatting and trunk settings.
