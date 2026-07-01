# Gobe — Controllers: Button Remap by Capture (sub-project C2)

> Date: 2026-07-01
> Scope: Full per-controller button remapping via capture ("press the button for A"), persisted,
> applied in the emulator. Extends the Swap A/B/X/Y presets (C first cut) with arbitrary bindings.

## 1. Purpose

Let the user reassign any face/shoulder/Select/Start button on a controller to any emulator button,
by capturing the physical press. Completes the original "change the buttons" request beyond the
quick A/B and X/Y swap presets.

## 2. Decisions captured during brainstorming

- **Per-controller custom map** `physicalKeyCode → targetKeyCode` (both Android keycodes), keyed by
  `InputDevice.getDescriptor()`.
- **Composition (clean, no double-transform):** in the emulator the mapping is
  `out = customRemap[code] ?: remapCode(code, swaps)` — a custom binding for a specific physical key
  wins; otherwise the existing Swap A/B/X/Y (sub-project C) applies; otherwise the code passes
  through.
- **Capture UI** in the controller detail: a "Remap" section, one row per emulator target button,
  each capturing the next physical press from that controller; a "Reset" clears the controller's map.
- **Buttons only** (face A/B/X/Y, shoulders L1/R1/L2/R2, Select, Start). No D-pad, no analog.

## 3. In scope

- Pure `ButtonRemap` (a `Map<Int,Int>` wrapper) + `applyMapping(code, remap, swaps)` combining the
  precedence rule; plus serialize/deserialize helpers. Unit-tested.
- Persistence: `ControllerPrefs.loadRemaps(context): Map<String, ButtonRemap>` and
  `saveRemap(context, descriptor, ButtonRemap)` — one string value per descriptor
  (`ctrl.remap.<descriptor>` = `"phys:target,phys:target,…"`).
- UI in `ControllerDetailScreen`: a "Remap buttons" section listing the 10 targets (A, B, X, Y, L1,
  R1, L2, R2, Select, Start) with each one's current physical binding (or "default"); tapping a row
  enters capture; a "Reset" button. A small "capturing…" affordance.
- `ControllersActivity`: a `capturingTarget` state; while set, the next gamepad keydown from the
  selected controller records `physicalKeyCode → targetKeyCode` (instead of driving the test panel),
  saves, and exits capture. Load remaps with the other prefs.
- `EmulatorActivity`: load remaps; apply `customRemap[code] ?: remapCode(code, swaps)` on the
  forwarded keycode. Pause combo stays on the raw code; motion untouched; read-at-launch.

## 4. Out of scope (deferred)

- D-pad / analog remapping.
- Per-game mappings.
- Conflict UI (two targets bound to the same physical key) — last binding wins; documented.
- Rumble (found infeasible on this hardware), deadzone, menu-hotkey config.

## 5. Architecture

- **Pure (`emulation/input/ButtonRemap.kt`, JVM-testable):**
  ```
  data class ButtonRemap(val byPhysical: Map<Int, Int> = emptyMap()) {
    fun bind(physical: Int, target: Int): ButtonRemap   // set physical->target (remove any other
                                                          // physical currently pointing at target)
    fun reset(): ButtonRemap
    fun physicalFor(target: Int): Int?                  // reverse lookup for the UI ("what physical
                                                          // key currently triggers this target")
  }
  fun applyMapping(code: Int, remap: ButtonRemap, swaps: ButtonSwaps): Int =
      remap.byPhysical[code] ?: remapCode(code, swaps)
  fun serializeRemap(r: ButtonRemap): String            // "phys:target,phys:target"
  fun parseRemap(s: String?): ButtonRemap
  ```
  `bind` enforces one physical per target (so the UI row shows a single binding) by removing any
  existing physical that maps to the same target before adding the new one.
- **Targets:** a fixed list of the 10 target keycodes (`KEYCODE_BUTTON_A/B/X/Y/L1/R1/L2/R2/SELECT/START`),
  with display labels reusing `PadButton` names.
- **Persistence (`ControllerPrefs`):** `ctrl.remap.<descriptor>` string; load all into
  `Map<String, ButtonRemap>` (parse), save one via `serializeRemap`.
- **UI (`ControllerDetailScreen`):** below the Swap toggles, a "Remap buttons" column. Each target
  row: `"<Target>: <physical binding or default>"` as a focusable Button → `onCaptureStart(target)`.
  A "Reset" Button → `onResetRemap`. When capturing, a banner "Press a button on the controller…".
  The row's physical label comes from `remap.physicalFor(targetKeycode)` mapped to a readable name
  (via `keyCodeToPadButton` when possible, else the raw keycode).
- **`ControllersActivity`:** `remaps: Map<String, ButtonRemap>` (loaded with assignments/swaps),
  `capturingTarget: Int?` (target keycode). In `dispatchKeyEvent`, when `capturingTarget != null`
  and the event is a DOWN from the selected device: `remaps[sel] = remaps[sel].bind(event.keyCode,
  capturingTarget)`, save, `capturingTarget = null`, consume. Otherwise the existing test-panel /
  Back logic. `onResetRemap` clears + saves.
- **`EmulatorActivity`:** add `remapsByDescriptor` loaded in `onCreate`; a helper
  `remapForInput(deviceId): ButtonRemap`; change the forward to
  `applyMapping(code, remapForInput(event.deviceId), swapsForInput(event.deviceId))`.

## 6. Testing

- **Unit (JVM):** `bind` sets and enforces one-physical-per-target; `applyMapping` gives the custom
  binding precedence over the swap and passes unmapped codes through; `serializeRemap`/`parseRemap`
  round-trip; `reset` clears.
- **On-device (controller awake):** in the detail, tap "A" under Remap → "press a button" → press
  physical X → the A row shows it's bound to X; launch a game and confirm pressing physical X now
  acts as A; Reset clears; the swap presets still work for unbound keys; the Select+Start menu still
  opens.

## 7. Risks

1. **Capture vs. navigation:** while capturing, D-pad presses would also be captured — mitigate by
   only capturing gamepad *face/shoulder/Select/Start* codes (ignore D-pad during capture so the
   user can still cancel/navigate), or provide a Back-to-cancel. Keep capture simple: capture the
   first mapped face/shoulder/Select/Start code; Back cancels capture.
2. **Composition confusion** with swaps — mitigated by the clear precedence rule (custom wins,
   else swap) and documented.
3. **Descriptor caveats** (as before) — no remap = passthrough, input always works.
4. **Read-at-launch** — remap changes mid-session need a game relaunch (like swaps).

## 8. Defaults (change on request)

- 10 face/shoulder/Select/Start targets; one physical per target; custom-wins-over-swap precedence;
  per-controller, global, read-at-launch; Back cancels capture; persisted as a per-descriptor string.
