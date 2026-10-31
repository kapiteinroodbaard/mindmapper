# Tasks

## 1. Document model and format (core)

- [x] 1.1 Add the immutable model (`MindMap`, `Topic`, `TopicImage`, `LayoutKind`, `CentralShape`, `Side`) and a `newMindMap()` factory; verify with unit tests that a new map has one central topic
- [x] 1.2 Add JSON serialization with `formatVersion` 1 and the ZIP reader/writer (`map.json`, `images/`, `thumbnail.png`); verify with a round-trip unit test including images
- [x] 1.3 Reject invalid containers (not a ZIP, missing `map.json`, invalid JSON, missing image, newer version) with typed errors; verify with unit tests per case

## 2. Storage in the app

- [x] 2.1 Add a document repository that reads and writes `.mindmap` files through `ContentResolver`, building the archive in memory before writing; verify with a Robolectric test that "Save as" to a file URI writes a readable container with thumbnail
- [x] 2.2 Add image import through the photo picker with downscaling to ≤ 1600 px and WebP compression; verify the size rule with a unit test and on the emulator that a 3200×2000 image is stored as a 1600×1000 WebP
- [x] 2.3 Implement New, Open, Save and Save as in the start screen and editor with persistable permissions and fallback to Save as for read-only documents; verified on the emulator (save to Downloads, reopen, open via picker)
- [x] 2.4 Implement autosave (2 s debounce and on stop, only for writable saved documents, preference default on) and the unsaved-changes dialog; verified on the emulator that an edit is written to the file without pressing Save

## 3. Recent files

- [x] 3.1 Add the recent-files store (max 15 unpinned, pinned on top, remove) with unit tests for ordering, limit and pinning
- [x] 3.2 Show the list with thumbnails on the start screen, including the unavailable-file message and "Remove from list"; verify with a Robolectric UI test and on the emulator by moving a file

## 4. File association

- [x] 4.1 Add `VIEW`/`SEND` intent filters and content validation on incoming URIs; verify on the emulator that opening a non-Mindmapper file shows "This is not a Mindmapper file"
- [ ] 4.2 Manual check on a tablet: open a `.mindmap` file from a file manager, choose Mindmapper with "Always", and confirm later taps open it directly
