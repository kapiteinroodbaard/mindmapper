# Proposal

## Why

The value of the app is a mind map that looks good without manual work: evenly placed topics, tapered
coloured branches in the XMind style, and a choice between a map around the central topic and a map to
its right. This change defines how a document is laid out and drawn (change 3 of 6).

## What Changes

- Automatic layout in two kinds: **radial** (main branches on both sides of the central topic, clockwise
  from the top right, balanced by subtree height) and **right** (all branches to the right, stacked).
- A main branch may be pinned to a side; the remaining branches are balanced around it.
- Branches drawn as filled shapes that are thickest at the central topic and get thinner per level.
- Automatic branch colours by position, from a palette ordered so neighbouring branches contrast;
  children inherit their main branch's colour.
- Central topic shapes (circle, ellipse, rectangle, rounded rectangle, diamond, hexagon, capsule) with
  a selectable colour.
- Topics show text, an optional image with adjustable width, and an optional icon from a bundled set.
- Light and dark map appearance following the app theme; one renderer used for screen, PDF and PNG.

## Capabilities

### New Capabilities
- `map-layout`: where topics are placed for each layout kind.
- `map-styling`: shapes, branch colours, branch thickness and light/dark appearance.
- `topic-content`: what a topic can show (text, image, icon) and how it is sized.

### Modified Capabilities
- None.

## Impact

- `core`: layout engine and colour assignment (pure Kotlin, unit-tested).
- `app`: Canvas renderer, text measurement, bundled icon set (Material Symbols, Apache 2.0) as vector
  drawables.
