# map-export Specification

## Purpose
Defines how a map is exported to PDF and PNG files and handed to the user for saving or sharing.

## Requirements

### Requirement: PDF export
The app SHALL export the map to a PDF with a single page whose size fits the whole map plus a margin.

#### Scenario: Export to PDF
- **WHEN** the user exports a map to PDF
- **THEN** the PDF has exactly one page showing the complete map

### Requirement: PNG export
The app SHALL export the map to a PNG at twice the map's size in layout units, limited to 8192 pixels on
the longest side, on an opaque white background.

#### Scenario: Large map
- **WHEN** the map is 6000 units wide
- **THEN** the PNG is 8192 pixels wide with the height scaled proportionally

### Requirement: Export appearance
Exports SHALL always use the light appearance on a white background, regardless of the app theme.

#### Scenario: Export in dark mode
- **WHEN** the app is in dark mode and the user exports to PNG
- **THEN** the PNG has a white background with dark text

### Requirement: Save or share
After choosing an export format, the user SHALL be able to save the export to a location chosen with
the system file picker or share it through the Android share sheet. The default file name SHALL be the
map's name with the `.pdf` or `.png` extension.

#### Scenario: Share PNG
- **WHEN** the user chooses "Share" for a PNG export
- **THEN** the Android share sheet opens with the PNG attached
