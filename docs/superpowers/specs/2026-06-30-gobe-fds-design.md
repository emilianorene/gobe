# Gobe — NES / Famicom Disk System (FDS) Support Design

> Date: 2026-06-30
> Scope: Make the user's `.fds` (Famicom Disk System) games playable under `System.NES`, reusing
> the Fase 2 + Arcade emulation infrastructure.
> Builds on: Fase 0/1/2 + i18n + UI redesign + Arcade + Home grid + Gamepad-nav (all merged to `main`).

## 1. Purpose

Make the user's **194 `.fds` games** playable. The user's "NES" library is **100% Famicom Disk
System** (`.fds`); there are **0 cartridge `.nes` files**. FDS games require the FDS BIOS
(`disksys.rom`), which the user provides (the app does not distribute it). The Fase 2 + Arcade
stack already launches a system from the detail screen and exposes a user-accessible
`systemDirectory` for BIOS; this sub-project adds an NES/FDS-capable core, wires `System.NES`,
and handles the FDS-specific wrinkle (BIOS + possible multi-disk).

## 2. Content reality (confirmed on-device)

- `/storage/emulated/0/Download/ROMs/`: `Nintendo Famicom Disk System Champion Collection/` holds
  **194 `.fds`**; no `.nes`. They are Japanese / fan-translated titles.
- `System.NES` already lists `fds` in its extensions, so these are already scanned as NES and
  appear in the grid's NES section (currently with "próximamente" since no core is wired).
- `disksys.rom` is **not** yet in `/Download/ROMs/system`; without it, no FDS game boots.

## 3. Decisions captured during brainstorming

- **Core: spike-first.** Try the **already-bundled FBNeo** first (zero new binary if its FDS
  support works against the user's disks); if FDS loading fails, fall back to **FCEUmm**
  (armeabi-v7a — strong FDS support incl. disk control), then Nestopia. Pin whatever boots the
  user's real `.fds`.
- **BIOS: user-provided.** `disksys.rom` goes in `/storage/emulated/0/Download/ROMs/system`
  (already the emulator `systemDirectory`). Documented; not bundled/distributed.
- **"Jugar" auto-enables.** The detail gate is already `CoreManager.corePath(system) != null`, so
  wiring `System.NES → core` enables NES automatically (SNES/Arcade unchanged; N64 stays off).

## 4. In scope

- Spike the core against a real user `.fds` (with `disksys.rom` placed) on the ONN; pin the core
  + confirm the load-failure signal.
- If the pinned core is FCEUmm/Nestopia: bundle its `.so` (armeabi-v7a) in `jniLibs` (same as
  snes9x/fbneo). If FBNeo works: no new binary.
- `CoreManager`: map `System.NES → "lib<core>_libretro_android.so"`.
- Launch `.fds` games from the detail screen via the existing `EmulatorActivity` flow (render,
  audio, input, save states, Select+Start menu, Exit to Gobe).
- Document (RESULTS) the pinned core, that FDS needs `disksys.rom`, and where it goes.

## 5. Out of scope (deferred)

- **Disk-swap UI** for multi-disk FDS games (Disk A/B side changes). The spike checks whether the
  core auto-advances or exposes a control; a dedicated in-game disk-swap UI is a follow-up if the
  user's games need manual side changes. Single-disk / auto-swap titles work without it.
- Cartridge `.nes` support (user has none) — trivially covered by the same core if any appear.
- Box art for FDS: `assets/metadata/nes.json` uses No-Intro NES **cartridge** names, so most of
  the user's Japanese/translated FDS titles won't match and will show the branded `DefaultCover`
  (expected). Improving FDS box art is out of scope.
- N64; bundling/distributing any BIOS.

## 6. Architecture (reuses Fase 2 + Arcade)

- **Core packaging:** if a new core is needed, download it for `armeabi-v7a` from the libretro
  buildbot, rename to `lib<core>_libretro_android.so`, place in `app/src/main/jniLibs/armeabi-v7a/`,
  loaded by path from `applicationInfo.nativeLibraryDir` (already wired).
- **`CoreManager`:** add `System.NES -> "lib<core>_libretro_android.so"`. SNES (snes9x) and
  Arcade (fbneo) unchanged; N64 stays null.
- **Detail screen:** no change — `playable = CoreManager(...).corePath(system) != null` already
  turns NES on once the core exists.
- **`EmulatorActivity`:** the `.fds` path is passed as `gameFilePath`; the core loads the disk
  using `disksys.rom` found in `systemDirectory` (already `/Download/ROMs/system`). Save states /
  SRAM / pause menu / error safety-net (`getGLRetroErrors`) all reused unchanged.
- **BIOS discovery:** the core looks for `disksys.rom` in `systemDirectory`; nothing new to wire.

## 7. Error handling

- Missing `disksys.rom` or an unloadable disk → the core fails to load; surfaced via the existing
  non-crashing path (core's own message and/or the `getGLRetroErrors` safety-net toast + finish).
  The spike pins the concrete signal. RESULTS documents that the fix is "add `disksys.rom`".
- Missing core (shouldn't happen once wired) → existing CoreManager-null path.

## 8. Testing

- **Spike (on-device, first task):** place `disksys.rom` in `/Download/ROMs/system`; a real user
  `.fds` boots and is playable with the candidate core; iterate FBNeo→FCEUmm→Nestopia until one
  boots; pin the core + the load-failure signal; note whether multi-disk auto-advances.
- **Unit (JVM):** `CoreManager` returns the NES core path for `NES`; N64 still null.
- **On-device acceptance:** launch an FDS game from the grid → detail → Jugar; it renders with
  audio; gamepad controls; Select+Start opens the menu; Exit to Gobe returns; a missing-BIOS case
  shows a clear failure (not a crash).

## 9. Risks

1. **FDS BIOS** — nothing boots without `disksys.rom`; user-provided, documented. The spike must
   run with the BIOS in place, else it will wrongly conclude the core fails.
2. **Multi-disk FDS** — side A/B swaps may need manual control; deferred (may surface as "game
   stuck asking to flip disk"). Spike notes which user titles need it.
3. **Core FDS support variance** — FBNeo's FDS support may be incomplete; mitigated by the
   FCEUmm/Nestopia fallback in the spike.
4. **Box art** — sparse for FDS (Japanese/translated names); DefaultCover expected.
5. **Licensing** — cores are GPL-like (fine for personal use, consistent with the parent spec);
   no ROMs/BIOS bundled.

## 10. Defaults (change on request)

- Spike order: FBNeo (bundled) → FCEUmm → Nestopia; pin the first that boots the user's `.fds`.
  `disksys.rom` in `/Download/ROMs/system` (documented in RESULTS). "Jugar" enabled whenever a
  core exists for the system. Disk-swap UI deferred unless the spike shows the user's library
  needs it.
