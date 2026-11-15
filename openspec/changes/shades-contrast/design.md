# Design

## Context

`BranchShades.shade(central, position, count)` spreads HSL lightness evenly from 0.78 to 0.28 in the central
hue (saturation at least 0.35). `MapRenderer.schemeColor` calls it without knowing the background.
`MapTheme.forMap` already decides the map's appearance (Light/Dark) from the system theme or from the map's
own background colour or image luminance, and `theme.background` is the colour actually drawn (an image is
drawn over it). `relativeLuminance` exists in `MapTheme.kt`; `MapRenderer.backgroundLuminance` caches the
average luminance of the background image.

## Goals / Non-Goals

**Goals:**
- Every shade visibly differs from what is behind it, in light and dark appearance and on own backgrounds.
- Keep the scheme's character: one hue, light to dark clockwise, evenly spaced where possible.

**Non-Goals:**
- Changing the palette scheme or custom branch colours (they stay as the user or palette chose).
- Contrast of text inside main boxes (already handled by the contrasting text colour).

## Decisions

- **Range per appearance.** Light appearance: lightness 0.65 → 0.25. Dark appearance: 0.88 → 0.45. Chosen by
  inspection against white, the dark theme background and navy; the light range starts darker than today
  (0.78) because 0.78 is faint on white.
- **Contrast floor.** The effective background luminance is the background image's average luminance when
  the map has an image, else the luminance of `theme.background`. After computing a shade, if its WCAG
  contrast ratio with that luminance is below 1.5, its lightness is stepped (0.02 at a time) away from the
  background (darker on a light background, lighter on a dark one) until it reaches 1.5 or hits 0.05/0.95.
  Alternative considered: a fixed outline around branches; rejected as it changes every style's look.
- **API.** `BranchShades.shade(central, position, count, darkBackground: Boolean, backgroundLuminance: Float)`
  stays pure (core has no Android); `contrastRatio(l1, l2)` and a luminance helper move to/are duplicated in
  core so the check is testable there. `MapRenderer.schemeColor` passes `theme == Dark`-ness (by
  `theme.text` luminance, as `forMap` chose it) and the effective luminance.
- **Order.** Nudging only happens at the end of the range nearest the background, so the light-to-dark order
  is kept; if two neighbouring shades end up equal after nudging (very few branches near the limit), that is
  accepted.

## Risks / Trade-offs

- [Mid-luminance backgrounds (e.g. orange) leave little room] → nudging pushes the near end away; shades get
  closer together but stay visible.
- [Existing maps using shades look slightly different after the update] → only the lightness range changes;
  hue and order are kept. Accepted, since the scheme shipped in 1.2.0 only.
