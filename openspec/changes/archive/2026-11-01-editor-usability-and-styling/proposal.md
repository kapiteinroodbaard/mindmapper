# Proposal

## Why

Using the editor on a tablet shows several rough edges. Topic actions sit in a bottom toolbar, away from
the topic they act on. Renaming a map and styling the central topic are hidden in menus. Maps with many
branches become lopsided after switching from the right layout to the radial layout. Long central-topic
text is drawn off-centre. Users also want more control over the look of the map: emphasised text and a
heavier trunk that thins out quickly.

## What Changes

- Topic actions move onto the selected topic on the map: "+" handles for adding a subtopic (on the outer
  side; on both sides of the central topic in the radial layout) and a topic after it (below), and a
  small action bar next to the topic for edit text, image, icon, delete and, on the central topic, style.
  These actions leave the bottom toolbar. The toolbar stays available for future actions and is hidden
  while it has no actions.
- A pencil next to the map name in the top bar renames the map. The map name is the file name without
  `.mindmap`. Renaming a saved map renames its file. For a never-saved map, the name becomes the suggested
  file name at the first save. Where the storage location does not allow renaming, the app shows a
  message pointing to "Save as".
- The central topic's style (shape, colour and trunk settings) is opened from a style action on the
  central topic. The "Central topic style" item is removed from the overflow menu.
- Radial balancing counts the spacing between main branches, so many small branches are weighed
  correctly against a few large ones. Switching the layout kind clears the sides chosen for main
  branches, so switching to the radial layout always starts from a balanced distribution.
- Each map gets a configurable trunk: the start width of the main branches at the central topic and how
  quickly they taper. Tapering starts at the edge of the central topic.
- A topic's text can be made bold, italic and underlined. Formatting applies to the whole text of a
  topic.
- Multi-line text in the central topic is drawn centred in its shape.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `map-editing`: topic actions are shown on the selected topic instead of in the toolbar; central topic
  style is opened from the central topic; text formatting; renaming the map.
- `map-layout`: balancing counts the spacing between branches; a layout switch clears chosen sides.
- `map-styling`: configurable trunk width and taper.
- `topic-content`: bold, italic and underline per topic; centred multi-line text in the central topic.
- `document-format`: `map.json` stores the trunk settings and each topic's text formatting.
- `document-storage`: renaming the document's file from the editor.

## Impact

- `core`: model fields (`Topic.bold/italic/underline`, `MindMap.trunkWidth/trunkTaper`), edit operations
  (`setTextFormat`, `setTrunk`, `setLayout` clearing sides, `addChild` with side), balancing in
  `MapLayouter`, `map.json` encoding.
- `app`: `TopicMetrics` (paints per format, centring), `MapRenderer` (trunk ribbon), new on-map action
  overlay in the editor, text and style dialogs, rename in the top bar and `DocumentRepository`, recent
  files after a rename, strings in English and Dutch.
- Files stay readable by older app versions: new fields are optional and unknown keys are ignored, so
  `formatVersion` stays 1.
