# Tasks

## 1. Editing logic (core)

- [ ] 1.1 Implement edit operations on `MindMap` with unit tests for add, edit, delete, image, icon, style and layout changes
- [ ] 1.2 Implement `move` (reparent, reorder, side) rejecting moves into the own subtree, with unit tests
- [ ] 1.3 Implement `EditHistory` (100 entries, redo cleared on new change) with unit tests
- [ ] 1.4 Implement drop-target resolution from layout boxes with unit tests for child, before, after, side and invalid drops

## 2. Editor screen (app)

- [ ] 2.1 Add the editor `ViewModel` (document, history, selection, dirty state) with unit tests
- [ ] 2.2 Draw the map in a Compose Canvas with pan, pinch zoom (20–400 %) and fit-to-screen; verify fit on open with a Robolectric test
- [ ] 2.3 Add tap to select, double-tap to edit, the topic toolbar (child, sibling, text, image, icon, delete) and dialogs; verify add/edit/delete with a Robolectric UI test
- [ ] 2.4 Add map settings (central shape, colour, layout) and undo/redo in the top bar; verify with a Robolectric UI test
- [ ] 2.5 Add long-press drag with ghost and drop indicator using the core drop resolution; verify on a device
