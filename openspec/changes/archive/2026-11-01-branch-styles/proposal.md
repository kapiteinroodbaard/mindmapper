# Proposal

## Why

Telling branches apart at a glance is a core value of a mind map. Position, hierarchy and colour already
separate branches, but every branch is drawn the same way: a smooth tapered curve to a rounded box. A
style per main branch adds a fourth, strong visual cue, which helps most when presenting.

## What Changes

- Each main branch can have one of five branch styles: **standard** (today's look), **blurred**,
  **plant**, **lightning** and **straight**. A style sets, as one choice, the line to the main topic, the
  shape of the main topic's box, and the lines and underlines of all topics in that branch.
- The whole subtree of a main branch uses its style; subtopics cannot override it. A branch without a
  style is drawn as standard. When a main branch is moved to a deeper level, its style is dropped.
- Main topics get the style (palette) action in their action bar, next to delete. It opens a style picker
  with one preview tile per style, drawn by the map renderer in the branch's own colour, without text
  labels.
- Styles look the same on screen, in PDF and PNG exports and in the thumbnail.
- `map.json` stores the style on a main branch as an optional field; files without it open unchanged and
  `formatVersion` stays the same. An unknown style value opens as standard.

## Capabilities

### New Capabilities
- `branch-styles`: the available branch styles, how each looks, how a main branch's style applies to its
  subtree, and how the style is chosen.

### Modified Capabilities
- `map-styling`: "Tapered branches" now describes the standard look; other styles keep the tapering but
  not necessarily the smooth curve.
- `map-editing`: the action bar of a main topic also offers the style action.
- `document-format`: the stored map content includes a main branch's style, as an optional, compatible
  addition.

## Impact

- `core`: `Topic` model (new optional `style` field and `BranchStyle` enum), `MapEdits` (set style, drop
  style on move to a deeper level), `MapLayouter` (style per box, sized boxes per style),
  `MindMapArchive` (lenient decoding of unknown styles), tests.
- `app`: `MapRenderer` (style-specific lines, decorations, main topic shapes, underlines), `TopicMetrics`
  (box sizing per style), `TopicActions` (style action on main topics), `EditorDialogs` (branch style
  picker), `EditorViewModel` (set style, undoable).
- No new dependencies; no network or permissions.
