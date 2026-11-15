# Proposal

## Why

A topic's box is anchored on the side facing its parent, so its image can only grow outward. Today every
corner of the image has a resize handle; dragging an inner corner makes the image grow the other way, and
the handle drifts away from the finger. That feels broken.

## What Changes

- Resize handles appear only on the image's two outer corners: right for topics right of the centre, left for
  topics left of the centre. The central topic, which grows to both sides, keeps all four.
- While dragging, the image width is adjusted so that the handle's horizontal position follows the finger,
  also when the image is narrower than the topic's text and therefore grows to both sides of its centre.
- Vertical finger movement no longer changes the size.
- No change to the delete button, the width limits (48–480), live updating or undo behaviour.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `map-editing`: the image tools show handles on the outer corners only, and a dragged handle stays under the
  finger horizontally.

## Impact

- `app`: `TopicActions` (`ImageHandles`, `CornerHandle`, `ImageTools`), `EditorScreen` (passes the topic's
  side and depth), tests in `ColourAndImageToolsTest` and on the emulator.
- No model, format or renderer change.
