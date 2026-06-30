# Gobe — Fase 2 (SNES) Emulation Design

> Date: 2026-06-30
> Scope: First sub-project of Fase 2 — SNES emulation end-to-end.
> Builds on: Fase 0+1 (TV shell + library browser, merged to `main`).
> Parent spec: [RETRO_TV_SPEC.md](../../../RETRO_TV_SPEC.md) §5 Fase 2, §6.1 (GPL).

## 1. Purpose

Make Gobe actually *play* games. This milestone integrates the native emulation stack
(LibretroDroid + a Libretro core) and delivers a full **SNES** play loop: from the game
detail screen → launch → OpenGL render + audio → gamepad input → pause overlay with save
states → exit back to the library. SNES is chosen first because `snes9x` needs **no BIOS**,
it is the user's largest library (~1386 games), and it runs comfortably on the ONN's arm64
hardware — so it validates the whole emulation pipeline without BIOS or performance fights.

NES (`.fds` needs a Famicom Disk System BIOS), Arcade, and N64 are explicitly deferred to
later sub-projects, as is multi-player (P1–P4) and button remapping.

## 2. Decisions captured during brainstorming

- **First target:** SNES end-to-end via the `snes9x` core only.
- **Input:** a USB / 2.4GHz gamepad as Player 1 (standard Android `InputDevice`).
  Multi-port routing and remapping deferred.
- **In-game scope:** pause overlay (Resume / Save state / Load state / Exit), auto-save on
  exit, and "resume from save" from the detail screen.
- **LibretroDroid integration:** JitPack dependency as the primary path, vendoring as a
  local NDK/CMake module as the documented fallback, gated by an early integration spike.
- **Cores:** `snes9x` `.so` precompiled from the libretro buildbot for the ONN's ABI,
  bundled in the APK.

## 3. In scope

- Integrate LibretroDroid (render via OpenGL ES, audio via Oboe, input).
- Bundle the `snes9x` core `.so` for the device ABI in the APK.
- `EmulatorActivity` hosting `GLRetroView`, launched from the detail screen for SNES games.
- Gamepad input (P1) forwarded to the core; default RetroPad mapping.
- Pause overlay: Resume / Save state / Load state / Exit to menu.
- Save states to internal storage; auto-save on exit; SRAM (battery saves) persisted.
- Detail screen: enable **JUGAR**; add **Reanudar desde save** when a state file exists.
- Update `Game.lastPlayed` on launch; surface the **"Continuar jugando"** row on Home.

## 4. Out of scope (deferred)

- NES (`.nes`/`.fds`) and the Famicom Disk System BIOS handling.
- Arcade (FBNeo/MAME) and romset/BIOS validation.
- N64 (Mupen64Plus-Next) and its performance work.
- Multi-player ports P1–P4, controller-to-port assignment, button remapping.
- Per-core dynamic options UI, video options (aspect/integer-scale/filter) UI, shaders.
- Boxart scraping.

## 5. Integration approach (primary + fallback)

- **Primary (A):** consume LibretroDroid as a JitPack dependency
  (`com.github.Swordfish90:LibretroDroid:<tag>`), which provides `GLRetroView` and the
  native libraries.
- **Fallback (B):** if the JitPack artifact does not resolve or lacks native libs for the
  device ABI, vendor LibretroDroid as a local Gradle module (git submodule) and build its
  native code with the Android **NDK + CMake** (installed via `sdkmanager`).
- **Spike first:** the implementation plan begins with an integration spike that proves
  `GLRetroView` loads `snes9x` and boots one SNES ROM on the ONN, choosing A or B based on
  the result, *before* building the surrounding UI. This de-risks the largest unknown. The
  spike must boot the ROM **from its real on-device path** (e.g. `/storage/emulated/0/
  Download/ROMs/Snes/<game>.sfc`), not a bundled test asset, to confirm the native core can
  open a raw `MANAGE_EXTERNAL_STORAGE` path via libc file I/O — this is the one place the
  ROM-access model could surprise us.

## 6. Core delivery

- **ABI:** `armeabi-v7a` (CONFIRMED on the ONN — it runs **32-bit Android**; there is no
  arm64 on this device). Verified via `adb shell getprop ro.product.cpu.abi`.
- **Source:** `snes9x_libretro_android.so` from the libretro buildbot **armeabi-v7a** build.
- **Packaging:** ship the core inside the APK under `jniLibs/<abi>/` renamed to the
  `lib*.so` form required for packaging, with `android:extractNativeLibs=true`, and resolve
  its path at runtime from `applicationInfo.nativeLibraryDir`. (This mirrors Lemuroid's
  approach and avoids copying the core out of assets at first run.)
- `CoreManager` maps a `System` to its bundled core file path; only SNES is wired now.

## 7. Architecture

### 7.1 New package `com.gobe.tv.emulation`

```
com.gobe.tv
├─ emulation
│  ├─ EmulatorActivity.kt     (hosts GLRetroView via AndroidView + Compose pause overlay;
│  │                            immersive fullscreen, landscape, GL lifecycle)
│  ├─ EmulatorViewModel.kt    (pause state, save/load orchestration, lastPlayed update)
│  ├─ CoreManager.kt          (System -> bundled core .so path)
│  ├─ SaveStateStore.kt       (state/SRAM file paths + read/write; pure, unit-testable)
│  └─ ui/PauseOverlay.kt      (Compose overlay; D-pad navigable)
├─ data
│  └─ LibraryRepository       (+ updateLastPlayed(id); "continue playing" query)
└─ ui/detail
   └─ DetailScreen            (JUGAR enabled; "Reanudar desde save" when state exists)
```

### 7.2 Launch flow

1. Detail screen **JUGAR** (or **Reanudar desde save**) starts `EmulatorActivity` with an
   Intent extra payload: `gameId`, `romPath`, `system`, and a `loadState` flag.
2. `EmulatorActivity` resolves the core path via `CoreManager`, builds `GLRetroViewData`
   (core path, ROM path, `system/` + `saves/` dirs under `filesDir`), and adds the
   `GLRetroView`.
3. On first frame, the repository updates `lastPlayed`. If `loadState`, the saved state is
   applied **on the concrete LibretroDroid "core ready"/first-frame signal** (e.g. the
   `GLRetroView` state event), not on a timer — applying it before the core is ready would
   be racy and silently fail.

### 7.3 Input (P1)

- The gamepad delivers standard Android `KeyEvent`/`MotionEvent`. `EmulatorActivity`
  forwards `dispatchKeyEvent`/`onGenericMotionEvent` to `GLRetroView`, which maps them to
  RetroPad port 0 using LibretroDroid's default mapping. No custom remapping this milestone.
- The pause overlay is opened by the **Back** button (and/or a gamepad menu/start-style
  combo if trivial); while open, the overlay captures D-pad navigation and emulation is
  paused.

### 7.4 Pause + save states

- Opening the overlay pauses emulation (`GLRetroView` paused / audio muted).
- **Guardar estado:** `serializeState()` → `filesDir/states/<gameId>.state` via
  **temp-file-then-rename** (write `<gameId>.state.tmp`, then atomic `rename`), so an
  auto-save fired during a tight `onPause`/`onDestroy` window can never leave a truncated
  state file.
- **Cargar estado:** read that file → `unserializeState(bytes)`.
- **Salir:** auto-save state, persist SRAM, finish the Activity → back to detail/Home.
- **Resume from save:** the detail screen offers it when `states/<gameId>.state` exists.

### 7.5 Data / Home

- `Game.lastPlayed` is set on launch (epoch millis). No schema change.
- Home shows a **"Continuar jugando"** row first when any game has `lastPlayed != null`,
  ordered by `lastPlayed` desc (cap e.g. 12 items). Reuses the existing tile/focus code.
- Detail "Save state: sí/no" derives from the presence of the state file.

## 8. Error handling

- Missing/oversized core or ROM unreadable → show an error screen in `EmulatorActivity`
  and return to detail rather than crashing.
- `GLRetroView` load failure / core error → surfaced as a message; Activity finishes safely.
- Save/load state IO errors → non-blocking message in the overlay; emulation continues.
- Lifecycle: `onPause` pauses the core and auto-saves; `onDestroy` releases `GLRetroView`.

## 9. Testing strategy

- **Unit (JVM, test-first where logic exists):** `SaveStateStore` path building + state-exists
  checks; `CoreManager` system→core resolution; repository `updateLastPlayed` and the
  "continue playing" ordering/cap.
- **On-device acceptance checklist (cannot be unit-tested):**
  - A real SNES ROM boots and renders with audio.
  - Gamepad controls the game (P1, default mapping).
  - Back opens the pause overlay; emulation pauses.
  - Save state, then Load state restores it.
  - Exit auto-saves and returns to detail; "Reanudar desde save" then resumes.
  - SRAM persists across a full quit/relaunch (battery-save games).
  - "Continuar jugando" row appears and launches the last game.

## 10. Risks

1. **LibretroDroid integration** (JitPack vs vendor) — the dominant risk; mitigated by the
   early spike that picks A or B before UI work.
2. **NDK/CMake build** if the fallback (B) is needed — extra toolchain setup + longer builds.
3. **Core `.so` packaging + path resolution** (jniLibs naming, `extractNativeLibs`).
4. **GL/Activity lifecycle** — pause/resume/destroy and auto-save timing.
5. **Licensing:** LibretroDroid and `snes9x` are GPL-3.0 — fine for personal use; if ever
   distributed, sources must be offered (parent spec §6.1).

## 11. Defaults (change on request)

- New `EmulatorActivity` (separate from the single-activity browser), package
  `com.gobe.tv.emulation`, arm64-v8a core, default RetroPad mapping, video left at
  LibretroDroid defaults (aspect/scaling UI deferred), states under `filesDir/states/`.
