# Gobe — Controllers: Button Swap Presets (sub-project C, first cut) — RESULTS

> Date: 2026-07-01
> Branch: `feat/controllers-swap`
> Plan: `docs/superpowers/plans/2026-07-01-gobe-controllers-swap.md`
> Spec: `docs/superpowers/specs/2026-07-01-gobe-controllers-swap-design.md`

## Outcome

The controller detail now has **Swap A/B** and **Swap X/Y** toggles per controller; the emulator
applies them as a keycode substitution on that controller's input. Persisted per controller.

## What shipped

- **`ButtonSwaps` + `remapCode`** (pure, JVM-unit-tested): A↔B when `swapAB`, X↔Y when `swapXY`,
  else the keycode passes through.
- **`ControllerPrefs.loadSwaps`/`saveSwaps`**: per-descriptor boolean swap keys
  (`ctrl.swapab.<desc>` / `ctrl.swapxy.<desc>`) in `gobe.settings`.
- **`EmulatorActivity`**: loads swaps in `onCreate`; forwards `remapCode(code, swapsForInput(deviceId))`
  to the core (both `sendKeyEvent` sites). The Select+Start pause combo stays on the raw code;
  motion/sticks are untouched. Read-at-launch (mid-session changes need a relaunch).
- **`ControllerDetailScreen`**: two toggle Buttons (Swap A/B, Swap X/Y) below the player selector,
  showing on/off; `ControllersActivity` persists on toggle.

## Commits (feat/controllers-swap)

- ButtonSwaps + remapCode (TDD) · persist swaps · apply swap in emulator · detail toggles + strings ·
  (docs) backlog USB-controller item.

## Acceptance

**Verified live on the ONN:**
- `ButtonSwapsTest` (4) + all existing unit tests green.
- Clean build + install.
- The swap-apply in `EmulatorActivity` is a **no-op when no swaps are configured** (`swapsForInput`
  returns `ButtonSwaps()` for an unknown descriptor → `remapCode` returns the code unchanged),
  matching the input path already confirmed working in sub-project B (game launches + input works).

**Not re-driven live (Nintendo Switch Pro Controller was asleep — Bluetooth idle, can't be woken via
adb):** the detail's Swap toggles and the in-game A/B swap effect. These are backed by the
unit-tested `remapCode`, the thin per-key persistence, and the one-line emulator apply.

**User self-test (~30 s, controller awake):** press a button on the Pro Controller to reconnect →
Settings → Controllers → select it → toggle **Swap A/B** (shows "● Swap A/B", persists across
reopen) → launch a game and confirm A and B act swapped (confirm/cancel roles flip) → toggle off and
relaunch restores → the Select+Start menu still opens regardless.

## Notes / next

- Only A/B and X/Y face-button swaps here; **full arbitrary capture-remap** ("press the button for
  A") is the deferred next cut.
- **Queued (backlog):** USB controller compatibility — several USB pads aren't detected through the
  ONN's USB-C multiport adapter. Two hypotheses to diagnose with a USB pad connected: (1) our
  `isGamepad` filter requires `GAMEPAD && JOYSTICK` and may hide `GAMEPAD`-only USB pads — likely fix
  is `GAMEPAD && !isVirtual`; (2) USB-OTG/hub power (if Android's `dumpsys input` doesn't see the pad
  at all). See `docs/superpowers/BACKLOG.md`.
- Other extras still open: rumble, analog deadzone setting, configurable menu hotkey.
