# Proposal

## Why

Presenters want more control over how a map looks and want to edit images where they are. Today
branch colours are always the fixed palette, text is always the theme colour, the background always
follows light/dark mode, numbering topics needs typed digits, and changing an image goes through a
separate dialog instead of working on the image itself.

## What Changes

- Ten new topic icons: a filled circle with a number, 0 to 9, for numbering topics.
- A topic's text can get a colour; without one it keeps today's automatic colour. The topic icon
  follows the text colour.
- A map can have a background colour and/or a background image. Without them the background follows
  light/dark mode as today; once set, the map keeps that background and an appearance that fits it,
  whatever the system theme. Exports use the map's background when one is set.
- A main branch can get a custom colour, used by it and everything under it. Branches without one keep
  the automatic colour.
- A second automatic branch colour scheme: shades of the central topic's colour, from light to dark in
  clockwise order. The map chooses between the palette (default) and shades.
- Images are edited on the map: when a topic with an image is selected, a delete button sits on the
  image and round handles on its corners resize it by dragging. **BREAKING** (UI only): the image
  dialog and "replace image" are removed; the image action is only offered on topics without an image.
- New optional fields in `map.json`; files stay readable by earlier versions and `formatVersion`
  stays 1.

## Capabilities

### New Capabilities
- `map-background`: the map's background colour and image, how they are shown on screen and in exports,
  and the appearance (text and palette contrast) that goes with them.

### Modified Capabilities
- `topic-content`: numbered icons; text colour per topic; image resized on the map within the same
  width range.
- `map-styling`: custom main branch colours, the shades scheme next to the palette, and light/dark
  appearance only while the map has no background of its own.
- `map-editing`: image tools on the map replace the image dialog; text colour in the text editor;
  background and branch colour scheme in the central topic's style dialog; branch colour in the branch
  style dialog.
- `branch-styles`: a custom branch colour is dropped with the style when a main branch becomes a
  subtopic.
- `map-export`: exports use the map's background when it has one.
- `document-format`: stores text colour, branch colour, colour scheme, background colour and
  background image, all optional.

## Impact

- `core`: `Topic` (text colour, branch colour), `MindMap` (colour scheme, background colour and image),
  edits, archive read/write and normalisation, colour resolution for branches.
- `app` rendering: `MapRenderer`, `MapTheme`, `MapCanvas`, `MapExporter`, `IconSet` plus ten new
  vector drawables (Material Symbols `counter_0`–`counter_9`, Apache 2.0).
- `app` editor: `EditorScreen`, `EditorViewModel`, `EditorDialogs` (text, style and branch style
  dialogs; image dialog removed), `TopicActions` (image tools), background image import.
- No new dependencies, permissions or network access.
