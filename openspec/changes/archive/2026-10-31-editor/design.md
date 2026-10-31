# Design

## Context

See proposal.md. Uses the immutable model (change 2) and `MapLayout` + renderer (change 3).

## Goals / Non-Goals

**Goals:** touch-first editing on tablets; all edit logic testable in `core`.

**Non-Goals:** keyboard shortcuts, multi-select, search, free positioning, collapsing.

## Decisions

### Edit operations in `core`
Pure functions on `MindMap` (`addChild`, `addSiblingAfter`, `setText`, `delete`, `setImage`,
`setIcon`, `move(topicId, parentId, index, side)`, `setLayout`, `setCentralShape`, `setCentralColor`).
`move` rejects moving into the own subtree.

### History
`EditHistory` keeps a list of `MindMap` snapshots (max 100) with an index. Because the model is
immutable, a snapshot costs only the changed path. Text edits are one entry per confirmed edit.

### Drop target resolution in `core`
`resolveDrop(layout, map, dragged, point)` → `Reparent(parent)`, `InsertBefore/After(sibling)`,
`ToSide(side)` or `None`, using the topic box under the point: upper 25 % → before, lower 25 % → after,
middle → child; on the central topic the half (left/right) decides the side. Testable without UI.

### Editor UI
A `ViewModel` holds document state (map, history, selection, file URI, dirty flag). The map is a
Compose `Canvas` that calls the shared renderer via `drawIntoCanvas { it.nativeCanvas }` with a
`(scale, offset)` transform. Gestures: `detectTransformGestures` for pan/zoom, `detectTapGestures` for
select/double-tap, `detectDragGesturesAfterLongPress` for moving topics with a translucent ghost and a
drop indicator. Topic actions live in a bottom toolbar; map actions in the top bar overflow menu.
Text editing uses a dialog with a wrapping text field; the keyboard's action key confirms.

## Risks / Trade-offs

- [Gesture conflicts between pan and long-press drag] → Long-press starts a drag only on a topic;
  elsewhere the gesture pans.
