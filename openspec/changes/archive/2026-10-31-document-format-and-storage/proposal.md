# Proposal

## Why

A mind map must live in a single file the user controls: stored where they choose, easy to copy or
back up, with all images inside it, and opened again quickly like Office documents. This change defines
that file and the open/save workflow around it (change 2 of 6, see `docs/exploration-notes.md`).

## What Changes

- A `.mindmap` file format: a ZIP container with `map.json` (with a format version), the embedded
  images under `images/`, and a `thumbnail.png` preview.
- The mind-map document model shared by all later changes (central topic, topics with text, optional
  image and icon, side override for main branches, layout kind, central shape and colour).
- New, Open, Save and Save as via the system file picker; autosave once a file has a location.
- Images inserted from the system photo picker are downscaled and compressed before embedding.
- Prompt for unsaved changes when leaving a document.
- A recent-files list on the start screen with thumbnails and pinning.
- The app registers as a handler for `.mindmap` files so file managers can open them with it.

## Capabilities

### New Capabilities
- `document-format`: the structure and versioning of `.mindmap` files and how invalid files are handled.
- `document-storage`: creating, opening, saving and autosaving documents in user-chosen locations,
  including image embedding and opening from other apps.
- `recent-files`: the list of recently used documents on the start screen.

### Modified Capabilities
- None. The start screen (`app-shell`) gains document actions through `document-storage` and
  `recent-files` without changing its existing requirements.

## Impact

- `core`: document model, JSON serialization and ZIP reader/writer.
- `app`: storage access (Storage Access Framework), photo picker, image compression, recent-files
  persistence, start screen actions, intent filters in the manifest.
- No new permissions: all file access goes through system pickers and granted URIs.
