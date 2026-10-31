# Design

## Context

See proposal.md. The model from `document-format-and-storage` is the input. Layout must be testable on
the JVM, while text measurement and drawing need Android.

## Goals / Non-Goals

**Goals:**
- Deterministic layout from (model, topic sizes) → positions.
- One renderer drawing to any `android.graphics.Canvas` (screen, PDF, bitmap).

**Non-Goals:**
- Free positioning, relationship lines, boundaries, collapsing.

## Decisions

### Layout in `core`, measurement injected
`MapLayouter.layout(map, measure: (Topic, depth) -> Size)` returns `MapLayout` with a box per topic id
(x, y, width, height, depth, side, colour slot) and the overall bounds, in layout units (1 unit = 1 dp
on screen, 1 pt in PDF). The app supplies `measure` using `StaticLayout` for text plus icon and image
sizes.

### Tree placement per side
Each side is a classic "tidy tree" to the right (mirrored for left): a subtree's height is
`max(topic height, sum of child subtree heights + gaps)`; children are stacked in order and the stack is
centred on the parent. Horizontal gap shrinks with depth (60, 40, 32 units). Because every subtree
occupies its own vertical band, topics never overlap.

### Radial balancing by subtree height
Main branches in order `b1..bn`. Branches with a chosen side are fixed. For the unfixed ones, choose a
split point `k` so `b1..bk` go right and `bk+1..bn` go left, minimising the height difference including
fixed branches. Order on screen: right side top→bottom, left side bottom→top (clockwise). A single
split point keeps the clockwise order intact, which also keeps colours consistent.

### Colour assignment
Palette of 10 hues ordered by large hue jumps (e.g. red, blue, yellow-green, purple, orange, teal, pink,
green, amber, indigo), each with a light and dark variant. Slot `i = index % 10` in clockwise order; in
radial with `n % 10 == 1` and `n > 1`, the last branch takes the next slot that differs from both
neighbours.

### Topic visuals (XMind-like)
- Central topic: filled shape in the chosen colour, text in white or near-black depending on luminance.
- Main topics (depth 1): filled rounded rectangle in the branch colour with contrasting text.
- Subtopics (depth ≥ 2): no box; text with a coloured underline of the branch colour, the branch
  attaching to the underline's inner end.
- Branch shape: a filled cubic Bézier ribbon from parent edge to child, start width by depth
  (central: 12→6, depth 2: 5→2.5, depth 3+: 2.5→1.5).

### Icons
About 70 Material Symbols (Apache 2.0) converted to vector drawables (`res/drawable/mm_icon_*.xml`),
identified in the file by a stable name (`"star"`). Drawn with `Drawable.setTint` so they work on every
Canvas.

### Theme
`MapTheme(background, text, palette, mainTopicText)` with `light` and `dark` instances. Screen uses the
app theme; export always uses `light`.

## Risks / Trade-offs

- [Very wide maps in radial layout] → Acceptable; the editor zooms to fit.
- [Font differences between devices] → System font (Roboto) is used everywhere; minor differences only
  affect wrapping, not correctness.
