# Tasks

## 1. Model, format and layout (core)

- [x] 1.1 Add `BranchStyle` (standard, blurred, plant, lightning, straight) and optional `Topic.style` with a lenient serializer that reads unknown values as null; verify with `MindMapArchiveTest` round-trip (style kept, standard omitted from `map.json`), old-file (all standard) and unknown-value tests, with `formatVersion` unchanged
- [x] 1.2 Add `setBranchStyle` (standard stored as null, only on main branches) and clear `style` in `move` when a main branch leaves the root while keeping it on reorder or side change; verify with `MapEditsTest`
- [x] 1.3 Add `TopicBox.style`, set from the main branch and passed to its whole subtree like `colorSlot`; verify with `MapLayouterTest` that sub-subtopics carry their main branch's style and moving a subtopic to another branch changes it

## 2. Rendering (app)

- [x] 2.1 Split `ribbon()` into centreline, tapered outline and paint steps without changing the standard look; verify with existing `RenderTest`s and a test that standard output matches the previous path bounds
- [x] 2.2 Size main topic boxes per style in `TopicMetrics.measure` and draw the per-style box paths (rounded, glow, lens, parallelogram, cut corners), also in the drag ghost; verify with a `RenderTest` that each style's padded content rect fits inside its box path
- [x] 2.3 Implement the straight and lightning centrelines (seeded zigzag with fixed end points, segment count scaled to length) and zigzag underline; verify with a `RenderTest` that lightning paths are identical across two renders and after editing another topic, and that line widths still taper per level
- [x] 2.4 Implement the blurred paint as layered transparent fills with per-appearance alphas from `MapTheme`, for branches, underlines and the box glow, keeping text sharp; verify with a `RenderTest` that PDF and PNG export of a blurred map succeed
- [x] 2.5 Implement plant decorations (alternating leaves and tendrils between 15% and 85% of main branches, sized from the local width, seeded per topic; one leaf at the end of subtopic underlines); verify with a `RenderTest` that decorations stay outside the central and main topic rects and are deterministic
- [x] 2.6 Add `drawStyleSample` for preview tiles; verify with a `RenderTest` that each style draws onto a small bitmap without errors

## 3. Choosing a style (app)

- [x] 3.1 Show the style action on main topics before delete in `TopicActions`; verify with a Robolectric UI test that a main topic shows style and delete (delete last) and a subtopic shows no style action
- [x] 3.2 Add `BranchStyleDialog` with five tiles drawn by `drawStyleSample` in the branch colour, current style marked, tiles with test tags and no text labels; wire it in `EditorScreen` for main topics while the central topic keeps its own dialog; verify with a UI test that tapping the lightning tile marks it as current
- [x] 3.3 Add `EditorViewModel.setBranchStyle` as one undoable edit; verify with an `EditorViewModelTest` that set and undo restore the previous style

## 4. Tuning

- [x] 4.1 Build sample maps (few and many main branches, radial and right layout, every style, thin and thick trunk) and tune plant and blurred (sizes, spacing, alphas) on the emulator in light and dark mode and in a PDF export; verify by screenshots in which decorations do not reach neighbouring branches and blurred stays visible in both modes

## 5. Integration

- [x] 5.1 Update the README features with branch styles; run `./gradlew check` and verify it passes
- [x] 5.2 On the emulator, verify the full flow: set each style on a branch, undo, move a styled main branch under another topic (style dropped), save and reopen, and export a PDF and PNG that match the screen
