# Gobe — Controllers: Button Remap by Capture (sub-project C2) — RESULTS

> Date: 2026-07-01
> Branch: `feat/controllers-remap`
> Plan: `docs/superpowers/plans/2026-07-01-gobe-controllers-remap.md`
> Spec: `docs/superpowers/specs/2026-07-01-gobe-controllers-remap-design.md`

## Outcome

Each controller's detail now has a **Remap buttons** section: capture any physical button for each
emulator target (A, B, X, Y, L1, R1, L2, R2, Select, Start), persisted per controller, applied in
the emulator with precedence over the Swap presets.

## What shipped

- **`ButtonRemap` + `applyMapping` + `serializeRemap`/`parseRemap`** (pure, JVM-unit-tested):
  physical→target map with one-physical-per-target `bind`, `reset`, `physicalFor` (reverse lookup);
  `applyMapping(code, remap, swaps) = remap[code] ?: remapCode(code, swaps)` (custom wins, else swap,
  else passthrough).
- **`ControllerPrefs.loadRemaps`/`saveRemap`**: per-descriptor string `ctrl.remap.<desc>`.
- **`EmulatorActivity`**: loads remaps in `onCreate`; forwards
  `applyMapping(code, remapForInput(deviceId), swapsForInput(deviceId))` at both send sites.
- **`ControllerDetailScreen`**: a scrollable "Remap buttons" section (10 target rows showing the
  current physical binding or "default", a capture banner, a Reset button).
- **`ControllersActivity`**: a `capturingTarget` capture state machine in `dispatchKeyEvent` — Back
  cancels (UP, consumed, doesn't leave/bind); an eligible face/shoulder/Select/Start DOWN from the
  selected controller binds+saves+exits; D-pad/unknown pass through; leaving the detail cancels.

## Commits (feat/controllers-remap)

- ButtonRemap + applyMapping (TDD) · persist remaps · apply in emulator · remap UI + capture state
  machine + strings (EN/ES).

## Acceptance

**Verified live on the ONN:**
- `ButtonRemapTest` (7) + all existing unit tests green.
- Clean build + install.
- The emulator apply is a **no-op when nothing is configured** (`applyMapping` → `remapCode` →
  passthrough with no remap/swap), matching the input path already confirmed working in B/C.

**Not re-driven live (Nintendo Switch Pro Controller asleep — Bluetooth idle; can't wake via adb):**
the Remap UI + capture flow (and, still, the B player-assignment and C swap flows). Backed by the
unit-tested `ButtonRemap`/`applyMapping`, the thin per-descriptor persistence, and the authoritative
capture state machine (Back-cancel / eligible-bind / ineligible-passthrough) reviewed against the
spec.

**Combined user self-test (~1 min, one controller awake) — covers B + C + C2:**
1. Wake the controller; Settings → Controllers → it appears → select it.
2. **B:** assign **P2** → "● P2"; Back → the row shows "P2"; reopen → persists.
3. **C:** toggle **Swap A/B** → "● Swap A/B".
4. **C2:** under Remap, tap **A** → "Press a button…" → press a physical button → the A row shows it;
   tap **B** → press **Back** → capture cancels, B stays "default" (nothing bound); **Reset** clears.
5. Launch a game and confirm the mapping/swap take effect and the Select+Start menu still opens.

## Notes / next

- **Pause combo on raw code (by design):** the Select+Start pause combo reads the RAW keycodes before
  remap, so a physical button remapped *to* Select/Start won't open the menu, and physical
  Select/Start still will. Intentional (the menu stays reachable regardless of remap).
- Remaps are read at emulator launch; mid-session changes need a game relaunch (like swaps).
- Deferred: D-pad/analog remap, per-game mappings. Extras still open: deadzone, menu-hotkey config.
  Rumble was found infeasible on this hardware.
