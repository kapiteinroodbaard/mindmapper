# Design

## Context

`MapLayouter` anchors a topic's box on its parent's side (`rect.left` fixed for right-side topics,
`rect.right` for left-side ones); the central topic is centred on the origin. Inside the box,
`TopicMetrics.content` centres the image horizontally over the content width, which is the widest of image,
text row and duration. So when the image is the widest element its inner edge stays put and the outer edge
moves by the full width change; when the text is wider, both edges move by half; for the central topic both
edges always move by half. `CornerHandle` today converts the drag delta along the corner's diagonal into a
width change, independent of where the edge actually ends up, so the handle can drift from the finger.

## Goals / Non-Goals

**Goals:**
- A dragged handle stays under the finger horizontally in every case above.
- Only handles that make sense for the anchoring are shown.

**Non-Goals:**
- Keeping the handle under the finger vertically (the box is vertically centred on its row and siblings
  reflow; horizontal tracking is what users judge size by).
- Changing how topics are anchored or laid out.

## Decisions

- **Which corners.** `ImageTools` gets the outward directions to show: `[1]` for `Side.RIGHT`, `[-1]` for
  `Side.LEFT`, `[-1, 1]` for the central topic (depth 0). `EditorScreen` derives them from the selected
  `TopicBox`. Top and bottom handles are both kept on the chosen side(s).
- **Width from feedback, not from the delta.** On each drag event the handle compares the finger's x (in
  root coordinates) with the image's current outer edge x on screen (from the latest `ImageTools.rect` and
  viewport) and applies `width += sx * (fingerX - edgeX) / scale`, clamped to 48–480. Because the layout
  reflows between events, the edge converges onto the finger whether it moves by the full or half the width
  change. Alternative considered: computing a factor 1 or ½ from whether the image is the widest element;
  rejected because the factor changes during a drag when the image overtakes the text.
- **Vertical movement** is ignored for the width.
- Undo stays as is: `previewImageWidth` during the drag, `commitImageWidth` once on release.

## Risks / Trade-offs

- [Convergence takes a few frames when the edge moves by half] → each event closes half the gap or more;
  at 60 events per second it settles within a few frames, which reads as following the finger.
- [Users who learned to drag inner corners lose them] → the feature shipped in 1.2.0 only.
