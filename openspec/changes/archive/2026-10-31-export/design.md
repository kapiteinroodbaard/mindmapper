# Design

## Context

See proposal.md. The renderer from change 3 draws to any `Canvas`, so export is a matter of choosing the
target canvas and scale.

## Goals / Non-Goals

**Goals:** identical drawing on screen and in exports. **Non-Goals:** multi-page PDF, SVG, transparency.

## Decisions

- PDF: `android.graphics.pdf.PdfDocument`; 1 layout unit = 1 pt, margin 36 pt. PDF pages are limited to
  14400 pt; larger maps are scaled down uniformly to fit.
- PNG: `Bitmap.createBitmap` at scale 2, reduced so the longest side ≤ 8192 px, background filled white,
  compressed losslessly.
- Rendering runs on a background dispatcher; the export is written to `cacheDir/exports/` first.
- Save: `ACTION_CREATE_DOCUMENT` with `application/pdf` or `image/png` and copy from cache. Share:
  `FileProvider` URI with `ACTION_SEND` and `FLAG_GRANT_READ_URI_PERMISSION`.
- Images in exports use the full embedded image, not a screen-sized cache.

## Risks / Trade-offs

- [Memory for an 8192×8192 ARGB bitmap is 256 MB] → Use the cap only on the longest side; most maps are
  wide, not square. If allocation fails, retry at half the scale and inform the user.
