# Proposal

## Why

The shades branch colour scheme always runs from the same light shade to the same dark shade, whatever the
map is drawn on. On a dark background the darkest branch almost disappears (seen with a navy background),
and on white the lightest branch is faint.

## What Changes

- The shades scheme picks its lightness range from the map's appearance: lighter shades on a dark
  background, darker shades on a light one.
- Every shade keeps a minimum contrast with the background the map is drawn on; a shade that would fall
  below it is moved away from the background's lightness.
- The order stays light to dark in clockwise order, and the shades stay in the central topic's hue.
- No change to the palette scheme, custom branch colours or the file format.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `map-styling`: the shades colour scheme adapts to the background so every branch stays visible.

## Impact

- `core`: `BranchShades` (lightness range per appearance, contrast check against a background colour).
- `app`: `MapRenderer.schemeColor` passes the appearance and the effective background (colour, or the
  background image's average colour) to the shades computation; exports and thumbnails follow because
  they use the same renderer.
