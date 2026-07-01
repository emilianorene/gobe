# Gobe — Controllers: Button Swap Presets (sub-project C, first cut)

> Date: 2026-07-01
> Scope: Per-controller **Swap A/B** and **Swap X/Y** toggles that remap those buttons in the
> emulator. Persisted per controller. The simple, high-value first cut of button remapping.
> Full arbitrary capture-remap is deferred to a later sub-project.

## 1. Purpose

Controllers like the Nintendo Switch Pro Controller report A/B and X/Y in the Nintendo layout
(opposite to the Xbox layout many games/cores assume), so face buttons feel swapped. Give the user
a one-tap **Swap A/B** and **Swap X/Y** per controller that the emulator applies — fixing the common
case without a full per-button capture UI.

## 2. Decisions captured during brainstorming

- **Simple presets first**: `swapAB` + `swapXY` booleans per controller (not full capture-remap).
- **Keycode substitution** in `EmulatorActivity`: swap the Android keycode before forwarding to the
  core. The pause combo (Select+Start) stays on the RAW keycodes so the menu always works.
- **Persisted per controller** (by `InputDevice.getDescriptor()`), reusing the B persistence pattern.
- **Global** (not per-game). Only face buttons A/B, X/Y — analog/sticks untouched.

## 3. In scope

- Pure `ButtonSwaps(swapAB, swapXY)` + `remapCode(keyCode, swaps): Int` (unit-tested).
- Persistence: extend `ControllerPrefs` with per-descriptor swap flags (load a
  `Map<String, ButtonSwaps>`; save one controller's swaps).
- UI: in the existing controller **detail** screen (below the player selector), two toggle buttons
  **Swap A/B** and **Swap X/Y** showing on/off state; writing persists immediately.
- `EmulatorActivity`: load per-controller swaps; apply `remapCode(code, swapsFor(deviceId))` to the
  forwarded keycode (alongside the existing port routing). Pause-combo detection unchanged (raw codes).

## 4. Out of scope (deferred)

- Full arbitrary per-button remap via capture ("press the button for A") — a later sub-project.
- Remapping D-pad / L1/R1/L2/R2/Select/Start / analog — only A/B and X/Y here.
- Per-game mappings; rumble/deadzone/menu-hotkey settings.

## 5. Architecture

- **Pure (`emulation/input/ButtonSwaps.kt`, JVM-testable):**
  ```
  data class ButtonSwaps(val swapAB: Boolean = false, val swapXY: Boolean = false)
  fun remapCode(keyCode: Int, s: ButtonSwaps): Int  // A<->B if swapAB; X<->Y if swapXY; else unchanged
  ```
- **Persistence (`ControllerPrefs`):** add `ctrl.swapab.<descriptor>` / `ctrl.swapxy.<descriptor>`
  boolean keys in `gobe.settings`. `loadSwaps(context): Map<String, ButtonSwaps>` and
  `saveSwaps(context, descriptor, ButtonSwaps)`.
- **UI (`ControllerDetailScreen`):** add a "Buttons" row with two toggle Buttons (Swap A/B, Swap X/Y)
  reflecting the controller's current `ButtonSwaps`; `onToggleSwapAB` / `onToggleSwapXY` callbacks.
  `ControllersActivity` owns the swaps state (loaded with assignments), writes via `ControllerPrefs`.
- **Emulator (`EmulatorActivity`):** load `swapsByDescriptor` in `onCreate`; a helper
  `swapsForInput(deviceId): ButtonSwaps`; in `dispatchKeyEvent` compute
  `val outCode = remapCode(code, swapsForInput(event.deviceId))` and pass `outCode` to
  `sendKeyEvent(...)`. The Select/Start pause detection continues to use the raw `code`.

## 6. Testing

- **Unit (JVM):** `remapCode` swaps A↔B only when `swapAB`, X↔Y only when `swapXY`, leaves other
  codes (and A/B when only XY is on) unchanged; both-on swaps all four.
- **On-device (ONN, Pro Controller):** in the controller detail, toggle **Swap A/B** on; launch a
  game and confirm A and B are swapped (e.g. the button that used to confirm now cancels); toggle off
  restores; the swap persists across reopen; the Select+Start menu still opens regardless of swap.

## 7. Risks

1. **Core A/B convention** — some cores already treat the physical layout a certain way; the swap is
   a raw keycode substitution, so the effect is predictable (A key becomes B key). Documented; the
   user toggles to taste.
2. **Interaction with pause combo** — mitigated by detecting Select+Start on raw codes before remap.
3. **Descriptor caveats** (as in B) — default is no-swap, so input always works.

## 8. Defaults (change on request)

- Both swaps default **off**; per-controller; global; only A/B and X/Y; keycode substitution applied
  in the emulator. All tunable.
