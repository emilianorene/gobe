# Gobe — In-Game Controls Help (launch hint + pause legend)

> Date: 2026-07-01
> Scope: Make the in-game menu discoverable via a brief launch hint and a pause-overlay control legend.
> Builds on: everything merged to `main` (incl. the Home button legend from gamepad-nav).

## 1. Purpose

Inside the emulator, the pause menu opens with **Select+Start** (a gamepad combo) or **Back** — but
nothing tells the user that. This adds two small, non-intrusive control hints:

1. A **launch hint**: when a game starts, a subtle pill briefly shows how to open the menu, then
   fades — teaching the non-obvious combo without covering gameplay.
2. A **pause-overlay legend**: a small line in the pause menu showing its controls (A selects,
   B/Back closes), reflecting the app's real bindings.

## 2. Decisions captured during brainstorming

- **Both** helps, kept minimal (launch hint = the key discovery; pause legend = completeness).
- **Real bindings**: A = select/confirm, B/Back = close the menu (= resume). The menu opens via
  Select+Start or Back.
- **Home only for the emulator screen**; no change to controls or the emulator flow.

## 3. In scope

- `EmulatorActivity`: a Compose `ControlsHint` overlay shown from game start for a few seconds, then
  auto-hidden; also hidden as soon as the menu is opened (the user has discovered it). Drawn in the
  existing ComposeView overlay, below/independent of the `PauseOverlay`.
- `PauseOverlay`: a subtle control-legend line at the bottom (`Ⓐ <select> · Ⓑ/Back <close>`).
- i18n: hint + legend strings in `values/` (EN) and `values-es/` (ES).

## 4. Out of scope (deferred)

- Changing button bindings or the pause/menu flow.
- A full controls/settings screen, remapping, or per-system control diagrams.
- Legends on other screens (Home already has one; Detail/Settings not needed).

## 5. Architecture

- **Launch hint state (`EmulatorActivity`)**: a `showControlsHint` Compose state, initially `true`.
  - Auto-hide after a delay (~5 s) via a `LaunchedEffect { delay(...); showControlsHint = false }`
    inside the composable (do NOT use `Date.now()`; a Compose `delay` is fine).
  - Also set `false` when the menu opens (in `pause()` / when `paused` becomes true), so it vanishes
    once discovered.
  - Rendered only while `showControlsHint && !paused`.
- **`ControlsHint` composable** (new, e.g. in `emulation/ui/`): a bottom-centered pill
  (`Color(0xCC000000)`, rounded), one line: the localized "menu: Select+Start (or Back)" text,
  small type, white/High-alpha. Uses `Modifier.align(Alignment.BottomCenter)` in the overlay `Box`.
- **`PauseOverlay` legend**: append a subtle `Text` line at the bottom of the existing `Column`
  (or aligned bottom), `labelSmall`, `Color.White` ~60% alpha:
  `Ⓐ <legend_select> · Ⓑ/Back <legend_close>`. Reuse/mirror the Home legend style.
- The overlay host in `EmulatorActivity` currently shows `PauseOverlay` only when `paused`; the
  `ControlsHint` is added as a sibling in the same `setContent { }` so both can render independently.

## 6. Testing

- **On-device (ONN)**: launch a game → the hint pill appears at the bottom and **fades after a few
  seconds**; open the menu (Select+Start / Back) → the hint is gone and the pause overlay shows the
  **`Ⓐ … · Ⓑ/Back …` legend**; A selects a button, B/Back closes (resumes); Exit to Gobe still works.
  Switch to Spanish → hint + legend are translated.
- No meaningful pure-logic surface → no new unit test (the existing emulator tests are unaffected).

## 7. Risks

1. **Hint overlapping gameplay** — mitigated: bottom-centered, small, low-opacity, auto-hides ~5 s.
2. **Timing/lifecycle** — the `LaunchedEffect` delay is cancelled if the composable leaves
   composition (e.g. Activity finishes); acceptable. If the hint must survive config changes it can
   be hoisted to a `rememberSaveable`, but YAGNI for a one-shot hint.
3. **Glyph legibility** (Ⓐ/Ⓑ) — same as the Home legend (already verified fine on the ONN); text is
   tunable.

## 8. Defaults (change on request)

- Hint text (localized): "Menu: Select+Start (or Back)"; ~5 s visible; bottom-center pill. Pause
  legend: `Ⓐ Select · Ⓑ/Back Close`, bottom of the overlay, ~60% alpha. All tunable on-device.
