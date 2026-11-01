# Tasks

## 1. Model, format and layout (core)

- [x] 1.1 Add `Topic.bold/italic/underline` and `MindMap.trunkWidth/trunkTaper` with limits, store them in `map.json` (optional, clamped on read, `formatVersion` 1); verify with `MindMapArchiveTest` round-trip, old-file and out-of-range tests
- [x] 1.2 Add `setTextFormat`, `setTrunk` (clamped), `addChild` with side, and `setLayout` clearing chosen sides on a kind change; verify with `MapEditsTest`
- [x] 1.3 Count the gap between main branches in `balanceSides`; verify with `MapLayouterTest` (many small branches within a quarter, switch back to radial balances, existing scenarios still pass) and `./gradlew :core:test`

## 2. Rendering (app)

- [x] 2.1 Make `TopicMetrics` paints and layouts depend on the effective format (bold default per level, italic, underline) and let the renderer use the layout's paint; verify with a Robolectric `RenderTest` that bold/italic/underline change the measured width or paint flags
- [x] 2.2 Centre wrapped central-topic text by ink extent; verify with a `RenderTest` that a long central text has equal left and right margins within 1 unit
- [x] 2.3 Draw main branches with the map's trunk width, full width up to the central topic's edge and `(1 - p)^taper` thinning after it; verify with a `RenderTest` that exports render without errors for min and max values, and on the emulator by comparing taper 1 and 4

## 3. Editor actions on the map (app)

- [x] 3.1 Add the on-map action overlay (subtopic handle(s) on the outer side, both sides on the radial central topic, topic-after handle, action bar with edit text, image, icon, style on the central topic, delete), placed from the viewport, clamped to the canvas, hidden when out of view or while dragging; verify with a Robolectric UI test that selecting the central topic shows "Add subtopic" and "Central topic style" and no "Delete"
- [x] 3.2 Remove moved actions from `TopicToolbar`, render it only when it has actions, and remove the style item from the overflow menu; verify with the UI test that no toolbar is shown and the menu has no style item
- [x] 3.3 Add the trunk width and taper sliders to the style dialog (English and Dutch strings); verify with `./gradlew lint` (no missing translations) and on the emulator
- [x] 3.4 Add bold, italic and underline toggles with preview to the text dialog, applied together with the text as one undoable edit; verify with an `EditorViewModelTest` that formatting is stored, defaults are stored as null, and one undo reverts text and formatting

## 4. Rename the map (app)

- [x] 4.1 Add a pencil next to the title and a rename dialog that rejects empty names and invalid file-name characters (English and Dutch strings); verify with a Robolectric UI test
- [x] 4.2 Implement rename in `DocumentRepository` and `EditorViewModel` (new URI and permission, recent files updated, never-saved map keeps the name for "Save as", message when renaming is not possible); verify with `EditorViewModelTest` for a `file:` URI, a never-saved map and a read-only document

## 5. Integration

- [x] 5.1 Update README features for formatting, trunk and on-map actions; run `./gradlew check` and verify it passes
- [x] 5.2 On the emulator, verify the full flow: add branches with the handles, switch right → radial on a map with many branches, set trunk and formatting, rename a saved map, and export a PNG that matches the screen
