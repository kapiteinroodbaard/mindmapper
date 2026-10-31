# Tasks

## 1. Document model and format (core)

- [ ] 1.1 Add the immutable model (`MindMap`, `Topic`, `TopicImage`, `LayoutKind`, `CentralShape`, `Side`) and a `newMindMap()` factory; verify with unit tests that a new map has one central topic
- [ ] 1.2 Add JSON serialization with `formatVersion` 1 and the ZIP reader/writer (`map.json`, `images/`, `thumbnail.png`); verify with a round-trip unit test including images
- [ ] 1.3 Reject invalid containers (not a ZIP, missing `map.json`, invalid JSON, missing image, newer version) with typed errors; verify with unit tests per case

## 2. Storage in the app

- [ ] 2.1 Add a document repository that reads and writes `.mindmap` files through `ContentResolver`, building the archive in memory before writing; verify with a Robolectric test against a temporary file URI
- [ ] 2.2 Add image import through the photo picker with downscaling to ≤ 1600 px and WebP compression; verify with a unit test that a 4000×3000 bitmap is stored at 1600×1200
- [ ] 2.3 Implement New, Open, Save and Save as in the start screen and editor with persistable permissions and fallback to Save as for read-only documents; verify on a device
- [ ] 2.4 Implement autosave (2 s debounce and on stop, only for writable saved documents, preference default on) and the unsaved-changes dialog; verify the dirty/save state logic with unit tests

## 3. Recent files

- [ ] 3.1 Add the recent-files store (max 15 unpinned, pinned on top, remove) with unit tests for ordering, limit and pinning
- [ ] 3.2 Show the list with thumbnails on the start screen, including the unavailable-file message and "Remove from list"; verify with a Robolectric UI test

## 4. File association

- [ ] 4.1 Add `VIEW`/`SEND` intent filters and content validation on incoming URIs; verify the merged manifest contains the filters and that a non-Mindmapper ZIP shows the error
