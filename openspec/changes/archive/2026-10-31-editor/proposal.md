# Proposal

## Why

Users need to build and restructure maps quickly with touch on a tablet: add and edit topics, attach
images and icons, move branches around, and recover from mistakes. This change adds the editor
(change 4 of 6).

## What Changes

- Editor screen showing the map with pan and pinch zoom, and fit-to-screen.
- Select a topic by tapping; add child and sibling topics; edit text; delete topics with their subtree.
- Set or remove a topic's image (photo picker) and change its width; set or remove its icon.
- Change the central topic's shape and colour and the map's layout kind.
- Drag a topic to make it a child of another topic, to reorder it among siblings, or (for main branches
  in radial layout) to move it to the other side. No free positioning.
- Undo and redo for every change to the map.

## Capabilities

### New Capabilities
- `map-editing`: changing the map's content and structure in the editor.
- `undo-redo`: reverting and reapplying changes.
- `map-navigation`: moving around the map on screen (pan, zoom, fit).

### Modified Capabilities
- None.

## Impact

- `core`: edit operations on the immutable model, undo/redo history, drop-target resolution.
- `app`: editor screen, gestures, dialogs, toolbar; integrates storage actions from change 2.
