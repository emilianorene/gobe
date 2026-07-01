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
  fun serializeRemap(r: ButtonRemap): String            // "phys:target,phys:target" (decimal ints)
  fun parseRemap(s: String?): ButtonRemap               // null/empty -> empty; skip malformed
  ```
  `bind` enforces one physical per target (so the UI row shows a single binding) by removing any
  existing physical that maps to the same target before adding the new one.
  - **Unbound physicals keep their default.** Binding physical X→A does NOT change physical A: with
    no entry for A, `applyMapping` falls through to `remapCode`/passthrough, so pressing A still does
    A. Result: both X and A can act as A (documented, like the conflict case) — the user rebinds A to
    something else if they want it moved.
  - **`parseRemap`** returns an empty map for null/empty input, ignores malformed entries (not
    `int:int`), and keeps the last value on duplicate physical keys. Keycodes serialize as decimal.
- **Targets:** a fixed list of the 10 target keycodes (`KEYCODE_BUTTON_A/B/X/Y/L1/R1/L2/R2/SELECT/START`),
  with display labels reusing `PadButton` names.
- **Persistence (`ControllerPrefs`):** `ctrl.remap.<descriptor>` string; load all into
  `Map<String, ButtonRemap>` (parse), save one via `serializeRemap`.
- **UI (`ControllerDetailScreen`):** below the Swap toggles, a "Remap buttons" column. Each target
  row: `"<Target>: <physical binding or default>"` as a focusable Button → `onCaptureStart(target)`.
  A "Reset" Button → `onResetRemap`. When capturing, a banner "Press a button on the controller…".
  The row's physical label comes from `remap.physicalFor(targetKeycode)` mapped to a readable name
  (via `keyCodeToPadButton` when possible, else the raw keycode).
- **`ControllersActivity` capture state machine (authoritative):** `remaps: Map<String, ButtonRemap>`
  (loaded with assignments/swaps), `capturingTarget: Int?` (the target keycode being bound; null =
  not capturing). In `dispatchKeyEvent`, capture takes priority and follows these exact rules:
  1. **Back while capturing:** if `capturingTarget != null` and the event is `KEYCODE_BACK`
     (ACTION_UP), **cancel**: set `capturingTarget = null` and consume (return true). It must NOT
     fall through to the normal Back-leaves-detail/finish behavior and must NOT bind anything.
  2. **Eligible physical press while capturing:** if `capturingTarget != null`, the event is a DOWN
     from the selected device, and `keyCodeToPadButton(event.keyCode)` yields a **non-D-pad** button
     (i.e. a face/shoulder/Select/Start button — the eligible capture set), then bind:
     `remaps[sel] = remaps[sel].bind(event.keyCode, capturingTarget!!)`, save via `ControllerPrefs`,
     set `capturingTarget = null`, consume.
  3. **Ineligible press while capturing:** any other key while capturing (D-pad, unknown/unmapped
     codes) is **ignored for binding** — it does not bind and does not exit capture (D-pad may still
     move focus / the banner stays until an eligible button or Back). This prevents binding the
     D-pad or stray codes.
  4. **Not capturing:** the existing test-panel / Back / list logic (unchanged).
  `onResetRemap` clears the selected controller's map + saves.
- **`EmulatorActivity`:** add `remapsByDescriptor` loaded in `onCreate`; a helper
  `remapForInput(deviceId): ButtonRemap`; change the forward to
  `applyMapping(code, remapForInput(event.deviceId), swapsForInput(event.deviceId))`.

## 6. Testing

- **Unit (JVM):** `bind` sets and enforces one-physical-per-target; `applyMapping` gives the custom
  binding precedence over the swap and passes unmapped codes through; an unbound physical keeps its
  default even when another physical maps to the same target; `serializeRemap`/`parseRemap`
  round-trip including the **empty map** (→ `""` → empty) and **malformed/null** input (→ empty);
  `reset` clears.
- **On-device (controller awake):** in the detail, tap "A" under Remap → "press a button" → press
  physical X → the A row shows it's bound to X; launch a game and confirm pressing physical X now
  acts as A; **Cancel path:** tap "A" → press **Back** → capture exits and A keeps its prior binding
  (nothing bound); Reset clears; the swap presets still work for unbound keys; the Select+Start menu
  still opens.

## 7. Risks

1. **Capture vs. navigation (resolved in §5):** capture binds ONLY an eligible physical press (a
   `keyCodeToPadButton` non-D-pad button); D-pad and unknown codes are ignored during capture (they
   don't bind and don't exit); **Back cancels** capture (clears `capturingTarget`, consumes, does not
   leave the screen or bind). This is the single authoritative rule — see §5.
2. **Composition confusion** with swaps — mitigated by the clear precedence rule (custom wins,
   else swap) and documented.
3. **Descriptor caveats** (as before) — no remap = passthrough, input always works.
4. **Read-at-launch** — remap changes mid-session need a game relaunch (like swaps).

## 8. Defaults (change on request)

- 10 face/shoulder/Select/Start targets; one physical per target; custom-wins-over-swap precedence;
  per-controller, global, read-at-launch; Back cancels capture; persisted as a per-descriptor string.
