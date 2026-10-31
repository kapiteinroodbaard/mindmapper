# Design

## Context

See proposal.md. Builds on the `app`/`core` split from `project-foundation`. No permissions may be added
(spec `privacy`), so all access goes through the Storage Access Framework and the photo picker.

## Goals / Non-Goals

**Goals:**
- One document model, immutable, shared by layout, rendering, editing, presentation and export.
- A format simple enough to document and read with standard tools (ZIP + JSON).

**Non-Goals:**
- Detecting external changes to an open file, cloud sync, import from other tools.

## Decisions

### Immutable document model in `core`
`MindMap(layout, centralShape, centralColor, root: Topic)` and
`Topic(id, text, icon?, image?, side?, children)`. Edits return a new `MindMap`; this makes undo/redo
(change 4) a simple snapshot stack and keeps rendering free of races. Topic ids are random UUID strings.

### File layout
```
example.mindmap (ZIP)
├── map.json        {"formatVersion":1,"layout":"radial","centralShape":"rounded_rectangle",
│                    "centralColor":"#3D5A80","root":{"id":…,"text":…,"children":[…]}}
├── images/<id>.webp
└── thumbnail.png   (≤ 512 px, light theme)
```
JSON via kotlinx.serialization with `ignoreUnknownKeys` so newer minor additions don't break reading.
`formatVersion` is bumped only for incompatible changes; reading migrates older versions.
Images: `{"path":"images/<id>.webp","width":240,"aspect":1.5}` (width in layout units = dp).

### Writing safely
Save writes the whole ZIP to the target URI opened in `"wt"` (truncate) mode after the archive is fully
built in memory, so a failure while building never leaves a half-written file. Maps are small (images
are capped), so in-memory building is fine.

### Image compression
Decode with `ImageDecoder` and a target size so the longest side is ≤ 1600 px, then encode as lossy WebP
(quality 85), which keeps transparency. Stored under the topic's image id.

### Storage Access Framework
- Open: `ACTION_OPEN_DOCUMENT` with `*/*` (providers do not know a `.mindmap` MIME type); content is
  validated after picking.
- Save as: `ACTION_CREATE_DOCUMENT` with `application/octet-stream` and a suggested `<title>.mindmap`.
- Persistable read/write permission is taken for every picked URI so recent files and autosave keep
  working after restarts.
- URIs from `ACTION_VIEW`/`ACTION_SEND` may be read-only and non-persistable; the document then records
  `writable = false` and Save falls back to Save as.

### Autosave
A debounce of 2 seconds after the last change, plus a save in `onStop`. Only when the document has a
writable URI and the app-wide autosave preference (default on) is enabled.

### Recent files
Stored as JSON in `SharedPreferences`: `uri`, `displayName`, `lastUsed`, `pinned`. Thumbnails are read
from the file's `thumbnail.png` on demand and cached in memory; a read failure marks the entry
unavailable.

### File association
Intent filters for `VIEW` and `SEND` with `application/octet-stream`, `application/zip`,
`application/x-zip-compressed` and `application/vnd.mindmapper`, plus `file`/`content` path patterns for
`.mindmap`. Because these MIME types are broad, the app verifies the content (`map.json` +
`formatVersion`) and shows "This is not a Mindmapper file" otherwise.

## Risks / Trade-offs

- [Broad MIME filters make the app appear for any ZIP] → Accepted; required because file managers
  rarely report a type for unknown extensions. The content check prevents damage.
- [Some providers rename `x.mindmap` to `x.mindmap.bin`] → The content check, not the extension,
  decides; the name is cosmetic.
- [Large images inflate memory] → Capped at 1600 px on insert.
