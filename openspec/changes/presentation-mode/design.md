# Design

## Context

See proposal.md. Reuses `MapLayout`, the renderer and the viewport transform from the editor.

## Goals / Non-Goals

**Goals:** smooth, predictable focus animation; no way to modify the map.

**Non-Goals:** step-by-step reveal, presenter remote, second screen, speaker notes.

## Decisions

- Separate navigation destination sharing the editor's `ViewModel` state (the map is passed, not
  reloaded), so presenting never touches the file.
- Viewport as `(scale, offset)`; focusing computes the bounds of the tapped topic's subtree from
  `MapLayout` and animates scale and offset with `Animatable` (≈ 450 ms, ease in/out), the same "fit
  bounds" maths as the editor's fit-to-screen.
- Immersive mode with `WindowInsetsControllerCompat.hide(systemBars())` and
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`; restored when leaving.
- Screen stays on during presentation (`FLAG_KEEP_SCREEN_ON`).

## Risks / Trade-offs

- [Accidental exit by back gesture] → First back only returns to the overview.
