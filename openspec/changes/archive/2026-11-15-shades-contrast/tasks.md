# Tasks

## 1. Shades computation (core)

- [x] 1.1 Add relative luminance and contrast ratio helpers to `core` and the per-appearance lightness ranges (light 0.65→0.25, dark 0.88→0.45) to `BranchShades.shade`; verify with `BranchShadesTest` that shades on a dark background are lighter than on a light one and still ordered light to dark
- [x] 1.2 Add the 1.5:1 contrast floor with stepping away from the background; verify with `BranchShadesTest` that four shades of a blue central colour all reach 1.5:1 against white, against `#17171B` and against navy `#1D3557`, and that a mid-orange background still yields shades that reach the floor or the 0.05/0.95 limit

## 2. Rendering (app)

- [x] 2.1 Pass the appearance and the effective background luminance (background image average, else `theme.background`) from `MapRenderer.schemeColor`; verify with `ColourRenderTest` that a shades map gets lighter branch colours with `MapTheme.Dark` than with `MapTheme.Light`, and that a navy background colour yields branch colours with at least 1.5:1 contrast to navy

## 3. Verification

- [x] 3.1 Run all unit tests and check on the emulator a shades map on white, in dark mode and on a navy background colour, confirming every branch is clearly visible
