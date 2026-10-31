# Proposal

## Why

Maps need to leave the app as images or documents: for handouts, sharing with others, or printing. This
change adds PDF and PNG export (change 6 of 6).

## What Changes

- Export the current map to PDF (one page sized to the map) or PNG (high resolution).
- Exports always show the whole map in the light appearance on a white background.
- After exporting, the file can be saved to a chosen location or shared via the Android share sheet.

## Capabilities

### New Capabilities
- `map-export`: exporting maps to PDF and PNG and handing the result to the user.

### Modified Capabilities
- None.

## Impact

- `app`: PDF rendering with `PdfDocument`, bitmap rendering, `FileProvider` for sharing, export menu.
