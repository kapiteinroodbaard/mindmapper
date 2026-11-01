# Design

## Context

All branches are drawn by `MapRenderer.ribbon()`: it samples a cubic Bézier into 40 points, offsets each
point along the curve normal by a tapered half width, and fills the resulting polygon. Main topics are a
rounded rectangle; subtopics have no box but an underline in the branch colour. The same renderer draws
the screen, PDF (`PdfDocument`), PNG and the thumbnail, and the spec requires them to match.

The colour of a main branch is already carried to its whole subtree as `TopicBox.colorSlot`. Box sizes are
measured by `TopicMetrics.measure`, which already enlarges the central topic per `CentralShape` so the
padded content fits inside the shape. `MindMapArchive` decodes `map.json` with `ignoreUnknownKeys`, but an
unknown enum value would still fail decoding.

## Goals / Non-Goals

**Goals:**
- One drawing pipeline for all styles, so tapering, trunk width and taper keep working in every style.
- Styles that render identically on every output, including PDF.
- Deterministic decorations: the same map always draws the same pixels.

**Non-Goals:**
- Animated styles, per-subtopic styles, a map-level default style, a separate box-shape choice.
- Changing the layout algorithm beyond box sizes per style.
- Replacing the central topic's shape chips with preview tiles (possible later).

## Decisions

### Model: optional `style` on `Topic`, enum `BranchStyle`

`Topic.style: BranchStyle?`, meaningful only for children of the root, like `side`. Null means standard,
and with `explicitNulls = false` it is omitted from the file. Enum values serialise as `standard`,
`blurred`, `plant`, `lightning`, `straight`. Decoding uses a lenient serializer that maps unknown strings
to null (standard), so a file from a newer app with more styles still opens.

*Alternative:* a map-level `Map<topicId, style>`. Rejected: it can go stale when topics are deleted or
moved, while a field on the topic moves with it.

`MapEdits.setBranchStyle(id, style)` sets it (standard stored as null). `MapEdits.move` already clears
`side` when a topic leaves the root; it clears `style` in the same place. `addSiblingAfter` clears `side`
for a new main branch; a new topic has no style anyway.

### Layout: carry the style in `TopicBox`

`TopicBox` gets `style: BranchStyle`, set from the main branch in `place()` and passed down exactly like
`colorSlot` (standard for the central topic). The renderer then needs no tree lookups to know how to draw
any box or branch.

### Rendering: centreline → width profile → paint → decorations

`ribbon()` is split into reusable steps:

```
 centreline(style)  ->  outline(points, widths)  ->  paint(style)  ->  decorate(style)
 Bezier / zigzag /      tapered polygon, as now      fill / layers     leaves, tendrils
 straight segment
```

- **Centreline** returns sampled points plus tangents. Standard and plant use today's Bézier. Straight uses
  a straight segment. Lightning takes the straight segment and offsets interior vertices perpendicular to
  it by alternating amounts (6–8 segments, amplitude scaled to the branch length and width), with the
  first and last vertex fixed so the line still meets both topics. Corners inside the box the bolt starts in
  stay on the straight line, so a main branch leaves the central topic cleanly.
- **Width profile** is today's code unchanged (`inside` rect, `taper` exponent, w0 → w1), so trunk
  settings apply to every style.
- **Paint**: blurred draws the same outline as 5 rings, from wide and very transparent to narrow and more
  opaque, and then the real outline, instead of using `BlurMaskFilter`. Mask filters are not reliably supported by the PDF
  canvas, and layered fills look identical on every output.
- **Decorate**: plant places, between 15% and 85% of the curve, alternately a leaf (a lens-shaped path
  rotated to the tangent) and a tendril (a short arc ending in a spiral), on alternating sides, sized from
  the half width at that point. Only main branches get tendrils; subtopic underlines get one leaf at their
  outer end.

### Deterministic irregularity

Lightning offsets and plant placement use a small seeded generator (e.g. a xorshift seeded from the
topic id's hash). Positions are relative to the branch, so moving a branch only moves its pattern; editing
another topic never changes it. `kotlin.random.Random(seed)` is also acceptable since it is deterministic
across runs.

### Main topic boxes and underlines per style

| Style | Box path | Size factor (from padded content w×h) | Underline |
|---|---|---|---|
| standard | rounded rect (r 10) | w × h | straight |
| blurred | rounded rect + 3 layered glow rings | w × h | straight, layered |
| plant | leaf: pointed tips, gently bulging middle | (w + 1.6h) × 1.25h | straight + leaf at outer end |
| lightning | parallelogram, skew 0.3h | (w + 0.6h) × h | zigzag |
| straight | rectangle with corners cut by h/4 | (w + h/4) × h | straight |

`TopicMetrics.measure` applies the factor at depth 1, mirroring the central shape sizing. A proportional
lens grew far too wide for long texts, so the leaf gets fixed-width tips instead. Main branches end where
the outline crosses the box's vertical centre: the box edge, or 0.15h inside it for the parallelogram. The drag ghost
(`drawTopicOnly`) uses the same box and underline helpers.

### Style picker

`TopicActions` shows the palette action when the topic is the root or depth 1; delete stays last for all
non-root topics. For a main topic the palette opens a new `BranchStyleDialog` with five tiles. Each tile is
a Compose `Canvas` that calls a renderer helper (`drawStyleSample(canvas, style, color, size)`) drawing a
short branch and a small box in that style, in the branch's colour from the current `MapTheme`. The
current style gets a border. Tiles carry only a test tag, no localised label. Picking a tile applies the
style through the view model as one undoable edit, like other style edits.

## Risks / Trade-offs

- [Plant decorations crowd neighbouring branches on dense maps] → keep them in the middle part, size them
  from the local width with a small cap, and tune on sample maps (explicit task).
- [Blurred looks muddy in dark mode or too faint in light mode] → layer alphas come from `MapTheme` per
  appearance; tune on sample maps in both modes.
- [Larger plant/lightning boxes shift the layout when switching style] → accepted; the factors are kept
  modest and the layout already handles varying sizes.
- [Lightning zigzag on very short branches looks like noise] → segment count scales with branch length,
  with a minimum length per segment; very short branches get fewer segments.
- [Rendering cost of extra paths] → a few paths per branch; negligible next to text layout. No caching
  needed now.

## Migration Plan

No migration: the field is optional, missing means standard, and `formatVersion` is unchanged. An older
app version opening a newer file ignores the unknown `style` key and shows standard branches.
