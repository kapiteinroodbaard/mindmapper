# Spec Delta

## MODIFIED Requirements

### Requirement: Export appearance
Exports of a map without a background of its own SHALL always use the light appearance on a white
background, regardless of the app theme. Exports of a map with its own background SHALL use that
background, covering the whole page or image, and the appearance that fits it.

#### Scenario: Export in dark mode
- **WHEN** the app is in dark mode and the user exports to PNG
- **THEN** the PNG has a white background with dark text

#### Scenario: Export with a background colour
- **WHEN** a map has a dark green background colour and the user exports it to PDF
- **THEN** the PDF page is dark green with light subtopic text
