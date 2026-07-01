# Gobe — In-Game Controls Help — RESULTS

> Date: 2026-07-01
> Branch: `feat/ingame-controls`
> Plan: `docs/superpowers/plans/2026-07-01-gobe-ingame-controls.md`
> Spec: `docs/superpowers/specs/2026-07-01-gobe-ingame-controls-design.md`

## Outcome

The emulator now teaches its controls: a brief launch hint plus a pause-overlay legend. All
acceptance criteria passed on the ONN.

## What changed

- **`ControlsHint`** (new, `emulation/ui/ControlsHint.kt`): a bottom-centered pill shown at game
  launch — "Menu: Select + Start · or Back".
- **`EmulatorActivity`**: a `showControlsHint` state (true at launch), rendered in the overlay
  `ComposeView`; a `LaunchedEffect { delay(5000); showControlsHint = false }` auto-hides it, and
  `pause()` sets it false so it is **gone for good once the menu is opened**.
- **`PauseOverlay`**: a subtle legend line at the bottom — "Ⓐ Select · Ⓑ/Back Close" — reflecting the
  real bindings (A confirms, B/Back closes = resume).
- **i18n**: `controls_hint_menu`, `pause_legend_select`, `pause_legend_close` in EN + ES.

## Commits (feat/ingame-controls)

- `feat(emu): strings for in-game controls hint + pause legend (EN/ES)`.
- `feat(emu): auto-hiding launch controls hint (gone after menu opens)`.
- `feat(emu): control legend line in the pause overlay`.

## Acceptance (verified live on the ONN, 2020 Super Baseball / SNES)

- On launch, the hint pill "Menu: Select + Start · or Back" appeared bottom-center.
- ~7 s later (past the 5 s timeout) the hint had **disappeared on its own** while the game kept
  running.
- Opening the menu (Back) showed the pause overlay with the legend line **"Ⓐ Select · Ⓑ/Back Close"**;
  Resume/Save/Load/Exit all present and navigable.
- Game renders and the menu works; A selects, B/Back closes.
- Bonus: the L1→Search Home shortcut (shipped earlier) was used to reach the game — still works.

## Notes / follow-ups

- Screenshots of the running game show the GL surface as black in `screencap` (SurfaceView isn't
  captured) but the Compose overlays (hint/menu) are captured — the game itself renders fine on the
  TV (confirmed by the pause-overlay screenshot showing the live scoreboard behind it).
- Spanish strings use the same `stringResource` mechanism proven app-wide; not separately re-driven
  live (device locale was English/System).
- Parked, unrelated: **NES/FDS** support is spec+plan-approved on `feat/fds`, waiting on the user's
  `disksys.rom` BIOS.
