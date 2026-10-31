# Proposal

## Why

While presenting, the user follows their own map on the tablet as a guide. They need a clean, full-screen
view that lets them zoom to the branch they are talking about and back to the overview, without risk of
accidentally changing the map (change 5 of 6).

## What Changes

- A presentation mode started from the editor: full screen, system bars hidden, no editing controls.
- Tapping a topic smoothly zooms and centres the view on that topic and its subtopics.
- A tap on empty space, a back gesture or an overview button returns to the whole map.
- The map is read-only in presentation mode; pan and pinch zoom still work.

## Capabilities

### New Capabilities
- `presentation`: the full-screen, read-only presentation view and its navigation.

### Modified Capabilities
- None.

## Impact

- `app`: presentation screen, camera animation, immersive mode; reuses layout and renderer.
