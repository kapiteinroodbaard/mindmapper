# Design

## Context

The map model (`core`) is immutable and serialised by kotlinx.serialization into `map.json`, with
`explicitNulls = false`, so nullable fields left null never reach the file and older app versions ignore
unknown keys. Rendering (`app/render`) is one `MapRenderer` shared by screen, PDF, PNG and thumbnails.
Colours come from a `MapTheme` (Light/Dark) picked in `MapCanvas` from the system theme; exports always
use `MapTheme.Light` on white. Branch colours are palette slots (`TopicBox.colorSlot`) assigned
clockwise by `MapLayouter.colorSlots`. Images are stored as WebP under `images/` and listed by
`MindMap.imagePaths()`, which decides what is written on save. The image dialog (`ImageDialog`) offers
width, replace and remove; topic actions are Compose overlays positioned from the layout (`TopicActions`).

## Goals / Non-Goals

**Goals:**
- One place that decides a topic's branch colour and text colour, used by every output.
- One place that decides the map's appearance (theme) from the system theme and the map's background.
- Image tools that track the image while the layout reflows during a resize.

**Non-Goals:**
- Custom colours for subtopic branches (only main branches get a custom branch colour).
- Background images that pan and zoom with the map, tiling, or opacity settings.
- A free colour wheel: colours are picked from a swatch selector, as for the central topic today.

## Decisions

### Model fields
- `Topic.textColor: Int?` (any topic) and `Topic.branchColor: Int?` (main branches only, like `style`),
  both `#RRGGBB` via a nullable variant of `HexColorSerializer`.
- `MindMap.branchColors: BranchColorScheme` (`palette` default, `shades`), with an unknown-value-tolerant
  serializer like `BranchStyleSerializer`; `MindMap.background: Int?` and `MindMap.backgroundImage: String?`
  (an `images/…webp` path, the same import pipeline as topic images).
- `imagePaths()` includes the background image, so save/load, missing-image checks and cleanup work
  unchanged. `normalizeMainBranchFields` and `move` also clear `branchColor` off non-main topics.
- `formatVersion` stays 1: all fields are optional and default to today's behaviour.

### Colour resolution
- Branch colour of a box = the main branch's `branchColor` if set, else the scheme colour: palette →
  `theme.branchColor(colorSlot)` (unchanged); shades → a shade of `centralColor` by clockwise position.
- Shades are computed in HSL from the central colour's hue and saturation, lightness spread evenly from
  0.78 (first branch) to 0.28 (last), saturation at least 0.35 so grey central colours still give a
  visible ramp. One branch gets the middle lightness. The clockwise position is the index in the
  colour-slot order, so `MapLayouter` exposes the clockwise position per main branch next to the slot.
- A small `BranchColors` helper resolves `topicId → colour` once per draw from map, layout and theme;
  the renderer uses it instead of `theme.branchColor(box.colorSlot)` everywhere (branch, box, underline,
  drag ghost, style preview tiles).
- Text colour: `topic.textColor` if set, else today's rule (contrasting with the box fill for depth 0/1,
  `theme.text` otherwise). The icon is tinted with the same colour.

### Appearance from the background
- `MapTheme.forMap(map, systemDark, imageLuminance)`: without a background → Light/Dark by system as
  today. With a background colour only → Light or Dark chosen by that colour's luminance (same threshold
  as `contrastingTextColor`), with `background` replaced by the chosen colour. With an image → Light or
  Dark by the image's average luminance; the background colour (or the theme's) is drawn under it.
- The average luminance is computed once per image path by scaling the decoded bitmap to 1×1, cached in
  the renderer. Alternative considered: storing it in `map.json` — rejected, it is derived data.

### Drawing the background
- On screen, `MapCanvas` draws the background colour, then the image scaled to cover the canvas
  (centre-crop) in screen space before applying the viewport transform, so it stays put while panning
  and zooming. Presentation mode uses `MapCanvas`, so it follows for free.
- Exports draw the same cover-scaled image over the whole page/bitmap before the map transform; maps
  without a background keep white + light theme. The thumbnail follows the export.

### Text colour and branch colour UI
- `TextDialog` gets a row of colour swatches with an "Automatic" chip; the result carries the colour
  together with text and format so one confirm is one undo step.
- `BranchStyleDialog` gets an "Automatic" chip plus swatches; picking applies at once (like styles) and
  the preview tiles redraw in the chosen colour.
- `StyleDialog` (central topic) gets a colour scheme choice (Palette / Shades) and a Background section:
  Automatic, swatches (including light and dark neutrals), "Choose image…" and "Remove image".
- A shared swatch list extends `CentralColors` with white, black and light tints so text and backgrounds
  have usable choices.

### Numbered icons
- Material Symbols Rounded `counter_0`–`counter_9` (filled) as `mm_icon_counter_N.xml`, registered in
  `IconSet` under the keys `counter_0`…`counter_9` and listed first in the picker so numbering is quick.

### Image tools
- `MapRenderer.imageRect(map, layout, topicId)` returns the image rectangle in layout units using the
  same `TopicMetrics.content` offsets as drawing, so the tools line up exactly.
- `TopicActions` (only for the selected topic with an image) places four 20 dp round handles centred on
  the image corners and a small delete button inside the image's top-right corner.
- Dragging a handle: the width follows the drag projected on the corner's outward diagonal,
  `Δw = (sx·dx + sy·dy·aspect) / 2 / scale`, clamped to 48–480. During the drag the view model applies
  the width without pushing history (`previewImageWidth`); on release it pushes one history entry from
  the width before the drag (`commitImageWidth`). Because the layout reflows, handles are repositioned
  from the new layout each frame, so they stay on the corners.
- The action bar's image button is only shown when the topic has no image. `ImageDialog`, its strings
  and the "replace" path are removed.

## Risks / Trade-offs

- [Shades with very light or dark central colours give low contrast against the background] → lightness
  range is fixed and independent of the central lightness; box text uses the contrasting text colour.
- [Average luminance misjudges busy photos] → only the default text colour depends on it; the user can
  set text colours explicitly.
- [Live resize reflows the layout every frame on huge maps] → layout is already recomputed per edit; the
  drag only changes one width, and this is the same cost as editing text.
- [Older app versions open newer files] → they ignore the new keys and show automatic colours and the
  theme background; the background image stays in the container but is dropped by an older app on save.
  Accepted, as with branch styles.
