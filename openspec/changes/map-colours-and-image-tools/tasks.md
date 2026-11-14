# Tasks

## 1. Model and format (core)

- [x] 1.1 Add `Topic.textColor`, `Topic.branchColor`, `MindMap.branchColors` (palette/shades, unknown read as palette), `MindMap.background` and `MindMap.backgroundImage`, include the background image in `imagePaths()`, store them in `map.json` and keep `formatVersion` 1; verify with `MindMapArchiveTest` round trips, absent keys when unset, an unknown scheme read as palette, and a file without the fields opening with defaults
- [x] 1.2 Add edits `setTextColor`, `setBranchColor` (main branches only), `setBranchColorScheme`, `setBackground`, `setBackgroundImage`; clear `branchColor` in `move` when a main branch leaves the root and in archive normalisation off non-main topics; verify with `MapEditsTest`
- [x] 1.3 Expose each main branch's clockwise position from `MapLayouter` and add the shades computation (light to dark, central hue); verify with unit tests that four branches get four distinct shades ordered by lightness with the central hue, and one branch gets the middle shade

## 2. Rendering (app)

- [x] 2.1 Resolve branch colours (custom, palette, shades) in one place and use it for branches, main boxes, underlines, the drag ghost and the style preview tiles; verify with a render test that a custom-coloured branch and its subtopics use the custom colour and that shades differ per branch
- [x] 2.2 Draw topic text and icon in `textColor` when set; verify with a render test that a red subtopic's text pixels are red
- [x] 2.3 Add `MapTheme.forMap` (system theme without background, luminance-based Light/Dark with a background colour or image) and the cached average image luminance; verify with unit tests for dark colour in light mode, light colour in dark mode and a bright image in dark mode
- [x] 2.4 Draw the background colour and cover-scaled image in `MapCanvas` (screen space) and in exports/thumbnails; keep white + light for maps without a background; verify with a `MapExporterTest` that a PNG of a map with a dark background colour has that colour in its corners and a map without one is white
- [x] 2.5 Add the `counter_0`–`counter_9` icons to `IconSet`, listed first; verify with `IconSet` test that all ten resolve to drawables

## 3. Editor UI (app)

- [x] 3.1 Text colour in `TextDialog` (Automatic + swatches) applied together with text and format as one undoable edit; verify with an `EditorViewModelTest` for set, back to automatic and undo
- [x] 3.2 Branch colour in `BranchStyleDialog` (Automatic + swatches, applied at once, tiles redraw in the colour); verify with a UI test that picking a swatch sets the branch colour
- [x] 3.3 Colour scheme and background section in `StyleDialog`, with background image picking through the existing image importer; verify with `EditorViewModelTest` that setting and removing a background image is undoable and the image is saved in the container
- [x] 3.4 Image tools in `TopicActions`: delete button and four corner handles on the selected topic's image, live resize with one undo step per drag (`previewImageWidth`/`commitImageWidth`), image action only without an image; remove `ImageDialog` and its strings; verify with `EditorViewModelTest` for preview + commit + single undo, and a UI test that the delete button removes the image
- [x] 3.5 Add English and Dutch strings for all new labels; verify with `LocalizationTest`

## 4. Verification and docs

- [x] 4.1 Run all unit tests and check on the emulator: numbered icon, text colour, background colour and image (editor and presentation, light and dark mode), custom branch colour, shades scheme, image delete and resize
- [x] 4.2 Update the README feature list where it describes styling and images
