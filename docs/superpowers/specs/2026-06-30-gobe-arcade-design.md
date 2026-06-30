# Gobe — Arcade (MAME/FBNeo) Support Design

> Date: 2026-06-30
> Scope: Add arcade emulation (a MAME/FBNeo Libretro core) so the user's `.zip` arcade games
> are playable, reusing the Fase 2 emulation infrastructure.
> Builds on: Fase 0/1/2 + i18n + UI redesign (all merged to `main`).

## 1. Purpose

Make the user's ~15 arcade `.zip` games (contra, dino, ffight, ga2, mk2, mk3, …) playable.
The Fase 2 stack (LibretroDroid `EmulatorActivity`, `CoreManager`, jniLibs core packaging,
save states, Select+Start in-game menu) already supports launching a system from the detail
screen; this sub-project adds an arcade core, wires `ARCADE`, and handles the arcade-specific
wrinkle: **romset/core-version compatibility** (and BIOS for some games).

## 2. Decisions captured during brainstorming

- **Core family:** a **MAME** core (the user believes their romsets are MAME). Primary
  candidate **`mame2003_plus`** (the most common Android MAME core, romset ~MAME 0.78);
  FBNeo and other MAME versions (mame2010 ≈ 0.139) are fallbacks chosen by the spike.
- **Spike first:** the plan begins by loading one of the user's **real** arcade `.zip` on the
  ONN with the candidate core; iterate the core/version until a game boots, then pin it.
- **BIOS:** user-provided (personal use). The core's `systemDirectory` is where BIOS zips
  (e.g. `neogeo.zip`) go; the app does not bundle/distribute BIOS.

## 3. In scope

- Bundle the chosen arcade core `.so` (armeabi-v7a) in `jniLibs` (same packaging as snes9x).
- `CoreManager`: map `System.ARCADE` → the arcade core path (so detail "Jugar" enables).
- Enable launching `ARCADE` games from the detail screen (same `EmulatorActivity` flow:
  render/audio/input, save states, Select+Start menu, Exit to Gobe).
- Set the emulator `systemDirectory` so the core finds BIOS/parent files the user places there.
- Clear, non-crashing error UX when a romset doesn't match the core (wrong version) or a BIOS
  is missing — show a message and return to the library.
- Document (in RESULTS) the resolved core + the romset version the user's games need.

## 4. Out of scope (deferred)

- Romset auto-validation/conversion (the core decides; we surface failures, not fix sets).
- Bundling/distributing any BIOS.
- Multiple simultaneous arcade cores / per-game core selection (one arcade core this round;
  revisit only if the spike shows the library spans incompatible sets).
- NES (`.fds`/BIOS) and N64 — separate sub-projects.
- Arcade-specific input remapping / 4-player routing (Fase 3 controls work).

## 5. Architecture (reuses Fase 2)

- **Core packaging:** download the arcade core for `armeabi-v7a` from the libretro buildbot,
  rename to `lib<core>_libretro_android.so`, place in `app/src/main/jniLibs/armeabi-v7a/`,
  load by path from `applicationInfo.nativeLibraryDir` (already wired via
  `extractNativeLibs`/`useLegacyPackaging`).
- **`CoreManager`:** add `System.ARCADE -> "lib<core>_libretro_android.so"`. SNES stays
  `snes9x`. (NES/N64 remain null.)
- **Detail screen:** `playable` currently is `system == SNES`; broaden it to "core available
  for this system" (i.e. `CoreManager(...).corePath(system) != null`), so ARCADE shows an
  enabled "Jugar" and SNES is unchanged. Other systems still show "próximamente".
- **`EmulatorActivity`:** unchanged flow. The arcade `.zip` path is passed as `gameFilePath`;
  the core loads the romset. `systemDirectory` points at a `system/` dir under `filesDir`
  (the user copies BIOS zips there). Save states / SRAM / the pause menu work as for SNES.
- **Error handling:** if `GLRetroView`/the core fails to load the game (bad romset / missing
  BIOS), surface a clear message (Toast/overlay) and finish the Activity rather than hanging
  or crashing. (Confirm the failure signal during the spike — likely an error event or no
  first-frame within a timeout.)

## 6. Box art / metadata

- `assets/metadata/arcade.json` already exists (from the MAME thumbnails repo), so many
  arcade tiles already show box art via the existing matcher. **No player counts** for arcade
  (the MAME RDB has no `users`), so no badges — expected. Genre/year may be sparse for arcade.

## 7. Error handling

- Wrong romset version or missing BIOS → clear message + return to grid (no crash).
- Missing core (shouldn't happen once bundled) → handled by the existing CoreManager-null path.
- Performance: CPS3/late-MAME titles may run slowly on the ONN — a hardware limit, documented,
  not "fixed".

## 8. Testing

- **Spike (on-device, first task):** a real user arcade `.zip` boots and is playable with the
  candidate core; iterate core/version until it works; pin the choice + romset version.
- **Unit (JVM):** `CoreManager` returns the arcade core path for `ARCADE`.
- **On-device acceptance:** launch an arcade game from the grid → detail → Jugar; it renders
  with audio; gamepad controls; Select+Start opens the menu; Exit to Gobe returns to the grid;
  a clearly-failing romset shows an error instead of crashing.

## 9. Risks

1. **Romset/core-version mismatch** — the dominant risk; mitigated by the spike trying
   `mame2003_plus`, then other MAME versions / FBNeo, against the user's actual `.zip`.
2. **BIOS** for some games (Neo Geo `neogeo.zip`, etc.) — user-provided; documented.
3. **Performance** on the ONN for heavy titles — hardware limit.
4. **Licensing:** the arcade core (FBNeo/MAME) is GPL-like; fine for personal use (consistent
   with §6.1 of the parent spec); no ROMs/BIOS bundled.

## 10. Defaults (change on request)

- Core: `mame2003_plus` (armeabi-v7a) as the spike's first candidate; switch if the user's
  romsets don't match. `systemDirectory` = `filesDir/system`. "Jugar" enabled whenever a core
  exists for the game's system.
