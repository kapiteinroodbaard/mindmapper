# Tasks

## 1. Layout engine (core)

- [ ] 1.1 Implement the tidy-tree placement per side with depth-dependent gaps; verify with unit tests that children are centred on the parent and no boxes overlap for a generated map of 200 topics
- [ ] 1.2 Implement radial balancing by subtree height with chosen sides and clockwise order, and the right layout; verify the four-equal-branches, one-large-branch and chosen-side scenarios with unit tests
- [ ] 1.3 Implement colour slot assignment with the wrap-around rule; verify with unit tests for n = 1..25 that neighbouring slots differ (cyclic in radial)

## 2. Rendering (app)

- [ ] 2.1 Add `MapTheme` light/dark with the palette and text measurement (`StaticLayout`) for topics including icon and image; verify text wraps at the maximum width with a Robolectric test
- [ ] 2.2 Implement the Canvas renderer: central shapes, main topic boxes, subtopic underlines, tapered Bézier branches, images and icons; verify by rendering a sample map to a bitmap in a Robolectric test (non-blank, expected size)
- [ ] 2.3 Add the bundled icon set (≥ 50 Material Symbols as vector drawables) with a name → drawable registry; verify every registered name resolves to a drawable
