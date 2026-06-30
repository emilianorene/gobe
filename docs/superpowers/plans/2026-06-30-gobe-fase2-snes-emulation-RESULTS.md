# Gobe Fase 2 (SNES) — Acceptance Results

> Date: 2026-06-30
> Branch: `feat/fase2-emulacion`
> Plan: [2026-06-30-gobe-fase2-snes-emulation.md](2026-06-30-gobe-fase2-snes-emulation.md)
> Spec: [../specs/2026-06-30-gobe-fase2-snes-emulation-design.md](../specs/2026-06-30-gobe-fase2-snes-emulation-design.md)
> LibretroDroid API notes: [fase2-libretrodroid-api-notes.md](fase2-libretrodroid-api-notes.md)

## Outcome: ✅ SNES emulation works end-to-end on the real ONN Plus 4K.

From the library, a SNES game launches into a full-screen OpenGL render with audio and
gamepad input, a pause overlay with save states, and a "Continuar jugando" row that brings
you back. Verified on-device with the user's real library (e.g. 2020 Super Baseball, Final
Fight).

## Integration decision

- **Approach A (JitPack) chosen:** `com.github.Swordfish90:LibretroDroid:0.14.0`. The
  vendored NDK/CMake fallback was NOT needed. JitPack provides `GLRetroView` + native libs.
- The exact 0.14.0 API was pinned by the spike (see the API-notes doc) and used by all
  emulator code.

## Critical discovery: the ONN is 32-bit

- `ro.product.cpu.abi = armeabi-v7a` (abilist `armeabi-v7a,armeabi`) — **no arm64** on this
  device. The first spike shipped an arm64 core and it failed to load (`nativeLibraryDir`
  was `.../lib/arm`). Fixed by shipping the **armeabi-v7a** snes9x core. The spec/plan/notes
  were corrected to armeabi-v7a.

## Controller findings (important for the user)

- **Wired** controllers did not enumerate on the ONN: a wired Xbox needs the `xpad` kernel
  driver (absent on this TV box) and the Switch Pro over USB uses proprietary HID.
- **Bluetooth pairing works:** the **Nintendo Switch Pro Controller paired over Bluetooth**
  enumerates as `GAMEPAD | JOYSTICK` and controls the game. Recommendation for broad
  compatibility: pair gamepads over **Bluetooth** (or use a 2.4GHz USB dongle), not wired.
- The app forwards input generically (standard Android `KeyEvent`/`MotionEvent` → RetroPad
  port 0), so any gamepad Android enumerates works. A dedicated controller-management /
  remap / broad-compat screen remains a future sub-project (Fase 3 "Controles").

## Automated tests

- **JVM unit** (`:app:testDebugUnitTest`): green. CoreManager, SaveStateStore, EmulatorArgs
  (+ existing detection/scanner/diff).
- **Instrumented** (`:app:connectedDebugAndroidTest`): 4 tests pass on the ONN (Room DAO +
  the new continue-playing ordering/exclusion).

## On-device acceptance checklist

- [x] A real SNES ROM boots and renders with audio (snes9x, Mali-G310, GL ES 3.2).
- [x] Gamepad (Switch Pro over Bluetooth, P1) controls the game.
- [x] Back opens the pause overlay; the core pauses; D-pad navigates it.
- [x] Guardar estado writes `filesDir/states/<gameId>.state` (verified ~823 KB on device).
- [x] Salir auto-saves and returns to the detail/Home.
- [x] Detail shows "Save state: sí" + "Reanudar desde save" after a save (refreshes on resume).
- [x] `lastPlayed` is persisted and the **"Continuar jugando"** row appears first on Home.
- [x] SRAM wiring present (`saves/<id>.srm`, loaded via `GLRetroViewData.saveRAMState`);
      2020 Super Baseball has 0 Kbit SRAM so no file is written — expected.
- [x] Non-SNES games keep "Jugar (próximamente)" (only SNES is wired this milestone).

## Deviations / fixes during implementation

1. **ABI armeabi-v7a** (not arm64) — see above.
2. **EmulatorActivity is `exported=false`** (correct), so it can't be launched via `adb am
   start`; it is launched from the detail screen in-app. The spike used a temporary exported
   launcher entry, since removed.
3. **No separate EmulatorViewModel** — pause/save/load state is managed directly in the
   Activity with Compose state. Simpler; the plan's ViewModel was advisory.
4. **Detail `hasState` refresh on ON_RESUME** — fixed staleness so "Reanudar desde save"
   appears immediately after an in-game save without leaving the detail.

## Known limitations / follow-ups

- Only **SNES** (snes9x) is wired. NES (`.fds` needs the FDS BIOS), Arcade, N64 are next.
- Single save slot per game (auto-save shares the slot with manual save). Multi-slot later.
- Input is P1 only with the default RetroPad mapping; **controller management, remapping,
  multi-port (P1–P4), and broad-compat detection screen** are the next sub-project (Fase 3).
- `connectedDebugAndroidTest` uninstalls/reinstalls the app, wiping `filesDir` (save states)
  and resetting the app uid — a test-harness artifact, not an app bug.
