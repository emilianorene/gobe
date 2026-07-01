# Gobe — Controllers: Detection + Test (sub-project A) — RESULTS

> Date: 2026-07-01
> Branch: `feat/controllers-detect`
> Plan: `docs/superpowers/plans/2026-07-01-gobe-controllers-detect.md`
> Spec: `docs/superpowers/specs/2026-07-01-gobe-controllers-detect-design.md`

## Outcome

A "Controllers" screen (Settings → Controllers) lists connected gamepads live and lets the user
test their inputs. Verified on-device with a real **Nintendo Switch Pro Controller**.

## What shipped

- **`PadButton` enum + `keyCodeToPadButton`** (pure, JVM-unit-tested): Android gamepad keycode → a
  logical button.
- **`ControllersScreen`** (Compose): connected-device list / empty state + a labeled button panel
  that highlights active buttons, live stick readouts, and a last-input line.
- **`ControllersActivity`**: enumerates gamepads via `InputManager`, hot-plugs via
  `InputDeviceListener`, and captures input by overriding `dispatchKeyEvent`/`onGenericMotionEvent`
  (D-pad as key OR HAT axis; triggers L2/R2 as keycodes OR axes; stick deadzone). Registered in the
  manifest (exported=false).
- **Settings** launches it. **Strings** in EN/ES.

## Fixes during acceptance

- **Device filter tightened to `GAMEPAD` AND `JOYSTICK`.** The initial `OR` filter wrongly matched
  the ONN remote (joystick-only) and virtual input devices (gamepad-only). Requiring both isolates
  real controllers — on-device the list then showed only the Pro Controller.
- **White text.** tv-material3 `Text` defaults to black outside a `Surface`; the Activity now
  provides `LocalContentColor = onBackground` (same as `MainActivity`) so all labels are legible.
  (Reported by the user during acceptance.)
- Enumeration rewritten as an explicit loop (an `IntArray` functional-chain inference error).

## Commits (feat/controllers-detect)

- PadButton enum + mapper (TDD) · screen strings (EN/ES) · ControllersScreen UI ·
  ControllersActivity + manifest · explicit-loop fix · Settings launch · white-text + filter fix.

## Acceptance (verified live on the ONN, Nintendo Switch Pro Controller connected)

- Settings shows **Controllers**; opening it shows the screen.
- **Detection:** the list shows **only** "Nintendo Switch Pro Controller" — the TV remote and
  virtual devices are correctly excluded.
- **Input capture:** the last-input line updates with `<device> · <button>` — real presses showed
  "Nintendo Switch Pro Controller · B"; an injected gamepad A showed "Virtual · A".
- **Sticks:** read `0.00` at rest (deadzone working).
- **Contrast:** all text white/legible after the fix.
- Highlight chips light on held buttons (state-driven; a real controller shows it live — the
  injected quick down+up releases before a screenshot can catch the highlight).
- **Back** returns to Settings.

## Notes / next

- `keyCodeToPadButton` covers the standard button set; a non-standard keycode still shows in the raw
  last-input line, so nothing is invisible.
- This is sub-project A. Next in the controller-config effort: **B) player/port assignment**
  (route input by deviceId to P1–P4), then **C) button remapping**, plus the suggested extras
  (A/B swap, rumble, deadzone setting, menu-hotkey config).
