# Tasks

## 1. Handles

- [x] 1.1 Add the outward directions to `ImageTools` and show handles only there (right corners for right-side topics, left corners for left-side topics, all four for the central topic), derived from the selected `TopicBox` in `EditorScreen`; verify with `ColourAndImageToolsTest` that a left-side topic shows exactly two handles, tagged top-left and bottom-left, and the central topic four
- [x] 1.2 Replace the diagonal delta with horizontal feedback (`width += sx * (fingerX - edgeX) / scale`, clamped) using the current image rect on each drag event; verify with a `ColourAndImageToolsTest` drag on a stateful test host that re-lays out the rect each resize, for both a full-width and a half-width (centred) edge response, that the final outer edge is within 4 px of the finger, and that vertical movement alone does not resize

## 2. Verification

- [x] 2.1 Run all unit tests and check on the emulator a right-side, a left-side and the central topic with an image: handles on the expected corners, the dragged handle following the finger, one undo per drag
