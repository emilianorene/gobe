# Gobe — Controllers: List, Select & Player Assignment (sub-project B) — RESULTS

> Date: 2026-07-01
> Branch: `feat/controllers-players`
> Plan: `docs/superpowers/plans/2026-07-01-gobe-controllers-players.md`
> Spec: `docs/superpowers/specs/2026-07-01-gobe-controllers-players-design.md`

## Outcome

The Controllers screen is now a **list → select → detail** flow: each detected controller is a
selectable row (showing its assigned player), and its detail lets you assign it to **P1–P4** (saved)
and test it. The emulator **routes each controller's input to its assigned port** (unassigned → P1).

## What shipped

- **`ControllerAssignments` + `portForDevice`** (pure, JVM-unit-tested): a descriptor→port map with
  unique-port `assign`, `clear`, `portFor`; `portForDevice` defaults to **P1 (port 0)** for
  unknown/null.
- **`ControllerPrefs`**: persists assignments per-key in `gobe.settings` SharedPreferences (keyed by
  `InputDevice.getDescriptor()`).
- **`EmulatorActivity` routing**: the hardcoded port `0` in `sendKeyEvent`/`sendMotionEvent` is now
  `portForInput(event.deviceId)` (deviceId → descriptor → assigned port, default P1).
- **UI split** (`ControllersScreen.kt`): `ControllersListScreen` (rows + empty state),
  `ControllerDetailScreen` (P1–P4 selector + test panel), extracted `ControllerTestPanel`.
- **`ControllersActivity`**: list/detail navigation (`selected` descriptor), assign→save→refresh,
  Back returns detail→list→finish, and the test panel is **filtered to the selected controller**
  (both key and motion events).

## Commits (feat/controllers-players)

- ControllerAssignments + portForDevice (TDD) · ControllerPrefs · emulator port routing ·
  list→detail UI + per-device test + strings (EN/ES).

## Acceptance

**Verified live on the ONN:**
- `ControllerAssignmentsTest` (5) + all existing unit tests green.
- **Routing regression:** after switching the emulator from hardcoded port 0 to `portForInput`, an
  SNES game (2020 Super Baseball) still launches, renders, and responds (pause menu opens) — with no
  assignments every device resolves to P1, identical to before.
- Controllers **list** renders with the correct **empty state** ("No controllers detected…") and
  legible white text. The empty state was accurate: the Nintendo Switch Pro Controller (verified
  connected earlier) had gone to **sleep** (Bluetooth idle) by this point, and the
  `GAMEPAD && JOYSTICK` filter correctly excluded the remaining virtual devices.

**Not re-driven live (controller asleep, can't be woken via adb):** the list→detail→assign→persist
flow and per-controller test in the detail. These are backed by:
- the unit-tested assignment logic (unique-port, default-P1),
- the thin `ControllerPrefs` serialization,
- sub-project A having already proven the detection + test panel work with this Pro Controller live.

**User self-test (≈20 s, controller awake):** press any button on the Pro Controller to reconnect →
Settings → Controllers → the controller appears in the list → select it → assign **P2** (shows
"● P2") → Back shows "P2" on the row → reopen to confirm it persists. With two controllers, each
drives its assigned port in a 2-player game.

## Notes / next

- Descriptor caveats (empty/duplicate for identical pads) are a documented limitation; the P1
  default keeps input working regardless.
- Next: **C) button remapping**, plus the suggested extras (A/B swap, rumble, deadzone, menu-hotkey).
