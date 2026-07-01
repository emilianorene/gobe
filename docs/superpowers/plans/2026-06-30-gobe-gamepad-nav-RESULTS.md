# Gobe — Home Gamepad Navigation — RESULTS

> Date: 2026-06-30
> Branch: `feat/gamepad-nav`
> Plan: `docs/superpowers/plans/2026-06-30-gobe-gamepad-nav.md`
> Spec: `docs/superpowers/specs/2026-06-30-gobe-gamepad-nav-design.md`

## Outcome

Home now has gamepad shortcuts and an on-screen control legend. All acceptance criteria passed
on the ONN.

## What changed

- **Shortcuts (`HomeScreen`)**: `Modifier.onPreviewKeyEvent` on the root Column maps **L1 →
  focus Search** (and shows the IME) and **R1 → open Settings**, via the pure
  `keyToHomeAction(keyCode)` helper. Both KeyDown (act) and KeyUp (no-op) are consumed for L1/R1
  so they don't leak into focus navigation (per plan-review note).
- **Search focus**: `SearchField` gained a `FocusRequester` param, applied to its `BasicTextField`,
  so L1 can focus it directly.
- **Legend bar**: a subtle `HomeControlLegend` line pinned at the bottom of Home (the content
  `when` is wrapped in a `Box(Modifier.weight(1f))` so the legend stays fixed):
  `Ⓐ Select  ·  Ⓑ Back  ·  L1 Search  ·  R1 Settings` — labels match the app's real bindings
  (A = select, B = back).
- **Pure helper**: `HomeKeyMap.kt` (Compose-free) + `HomeKeyMapTest.kt` (JVM, 3 tests).
- **i18n**: `legend_select/back/search/settings` in `values/` (EN) + `values-es/` (ES).

## Commits (feat/gamepad-nav)

- `keyToHomeAction` helper (TDD; 3 unit tests green).
- legend label strings (EN + ES).
- L1→Search / R1→Settings shortcuts + button legend.

## Acceptance (verified live on the ONN)

- Unit tests pass (`HomeKeyMapTest` 3/3).
- Legend bar is visible and legible at the bottom of Home; enclosed-letter glyphs Ⓐ/Ⓑ render
  fine on the ONN typeface.
- **L1** (injected `input keyevent 102`) → the search field focuses and the on-screen keyboard
  appears. `keyboard?.show()` was needed and worked (focus alone did not always raise the IME).
- **R1** (injected `input keyevent 103`) → the Settings screen opens.
- `onPreviewKeyEvent` on the root was sufficient — the `MainActivity.dispatchKeyEvent` fallback
  from the plan/spec was **not** needed.
- Normal D-pad + A(select)/B(back) navigation unchanged.

## Notes / follow-ups

- Spanish legend labels are wired via the same `stringResource` mechanism proven app-wide for
  i18n; not separately re-driven live.
- Deferred (spec §4): in-game control legend; shortcuts/legend on Detail & Settings screens.
- This completes the queued gamepad-nav backlog item (`docs/superpowers/BACKLOG.md`).
