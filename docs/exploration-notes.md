# Mindmapper – exploration notes

Decisions from the initial exploration session (2026-10-31). Each roadmap item below becomes its
own OpenSpec change; these notes are the input for writing them.

## Roadmap

| # | Change | Scope |
|---|---|---|
| 1 | `project-foundation` | Kotlin/Compose skeleton, minSdk 35, NL/EN, light/dark, GPLv3, F-Droid hygiene |
| 2 | `document-format-and-storage` | `.mindmap` ZIP, schema version, image compression, save / save as / autosave, recent files, file association |
| 3 | `layout-and-rendering` | Radial + right layout, tapered branches, branch colours, central topic shapes, icons, images |
| 4 | `editor` | Editing, drag to reorder / switch side / reparent, undo/redo, zoom & pan |
| 5 | `presentation-mode` | Full screen, tap to focus & zoom, back to overview, read-only |
| 6 | `export` | PDF and PNG, share |

## Decisions per area

### Usage
- Presentations are given from a tablet. The mind map is the presenter's **personal guide**; it is
  not shown to the audience.

### Presentation mode
- Full screen, read-only.
- Tap a branch: camera animates/zooms to it. A gesture or button returns to the overview.
- Not needed: step-by-step reveal, presenter clicker, second screen, speaker notes.

### Editing
- Undo/redo.
- Drag a node to another parent (reparent).
- Manual positioning is limited to **changing order and side** (left/right) by dragging; the layout
  stays fully automatic. A main branch dragged to the other side overrides auto-balance for that
  branch; the remaining branches are rebalanced.
- No free offset positioning, no search, no keyboard shortcuts, no collapsing.

### Node content
- Text and an optional image per node; image size is adjustable. Placement relative to text is
  up to the design. No camera capture; images come from the system photo picker.
- One icon per node from a bundled set of ~50–100 icons with a free licence (e.g. Material Symbols,
  Apache 2.0). No emoji.
- No relationship lines, boundaries, labels or markers.

### Style and colours
- Central topic: selectable shape (circle, ellipse, square, diamond, rounded rectangle, …) and colour.
- Branches are thick at the trunk and taper as they branch (XMind style): filled Bézier shapes,
  width determined by depth.
- Each main branch automatically gets its own colour; children inherit it. No per-branch override.
- Colour is derived from **position**: reordering branches changes their colours. The palette is
  ordered so that neighbours contrast strongly, including the wrap-around between the first and last
  branch in radial layout and with an odd number of branches.
- Map theme follows the app theme (light/dark). Export is always light, on a white background.

### Layout
- One layout per map: (1) radial, topics surround the central topic; (2) right, topics stacked to
  the right of the central topic.
- Radial: clockwise, starting at the right; left/right balanced by **subtree height**, not branch
  count.
- Nodes are positioned automatically and evenly.

### File format and storage
- `.mindmap` = ZIP with `map.json` (including a schema version for migrations) and `images/`.
- Images are compressed/downscaled on insert to keep files small.
- No import from other tools.
- Office-like: files are opened from and saved to a user-chosen location (Storage Access Framework).
- Explicit save and save as. Autosave is only available after the first explicit save (a location is
  known then); it saves shortly after each change and when the app goes to the background.
- Prompt on unsaved changes.
- Recent files: 10–15 entries with thumbnail and pinning. Entries that are no longer accessible
  (moved, permission revoked) show a message and an option to remove them from the list.
- No cloud folders; no handling of external modification.
- File association: register for `ACTION_VIEW` with `application/octet-stream`, `application/zip`
  and the `.mindmap` extension; verify on open by extension or ZIP contents (`map.json` + version).
  Becoming the default app is the user's choice ("Always") in Android's chooser.
- Open via share intent as well.

### Export
- PDF: one page sized to the map.
- PNG: 2× scale, longest side capped at ~8192 px. No transparent background.
- Always the complete map, light theme.
- Share directly via the Android share sheet.

### Platform
- minSdk 35. Tablets in landscape are a must; phones supported.
- Dutch and English.
- No accessibility work, no stylus support.
