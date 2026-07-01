# Gobe — NES / Famicom Disk System (FDS) — RESULTS

> Date: 2026-07-01
> Branch: `feat/fds`
> Plan: `docs/superpowers/plans/2026-06-30-gobe-fds.md`
> Spec: `docs/superpowers/specs/2026-06-30-gobe-fds-design.md`

## Outcome

The user's 194 `.fds` (Famicom Disk System) games are now playable under `System.NES`, reusing the
Fase 2 + Arcade stack. Confirmed on-device: **Donkey Kong Jr. (FDS) boots and renders.**

## Pinned decisions (from the on-device spike, Task 1)

- **Core: FCEUmm** (`libfceumm_libretro_android.so`, `armeabi-v7a`, ELF 32-bit ARM EABI5, committed).
  Went straight to FCEUmm rather than trying FBNeo first (the plan's zero-binary probe) because the
  user's library is 100% FDS and FBNeo's FDS support is weak — FCEUmm is the standard FDS core.
- **BIOS: `disksys.rom`** (8192 bytes, CRC32 `5e607dcf` — verified) placed by the user and pushed to
  **`/storage/emulated/0/Download/ROMs/system/`** (the emulator `systemDirectory` from the Arcade
  work). FCEUmm found it automatically — core log:
  `FCEU_MakeFName: /storage/emulated/0/Download/ROMs/system/disksys.rom`.
- **"Jugar" auto-enabled**: the detail gate is already `CoreManager.corePath(system) != null`, so
  mapping `System.NES → FCEUmm` turned NES on with no detail/EmulatorActivity change.

## Commits (feat/fds)

- `feat(fds): bundle FCEUmm core (armeabi-v7a) — pinned by on-device spike`.
- `feat(fds): CoreManager maps NES to bundled FCEUmm core` (TDD; `nesResolvesToBundledCore` green).

## Acceptance

**On-device spike (real `EmulatorActivity → CoreManager → FCEUmm → disksys.rom` path):**
- Launched an FDS disk (`Donkey Kong Jr. (JP).fds`) → core log shows FDS header parsed
  (`Manufacturer: Nintendo · # of Sides: 1 · ROM MD5 …`) and the BIOS loaded from `systemDirectory`;
  no load errors.
- The **game renders** — a pause-menu screenshot shows live Donkey Kong Jr. gameplay behind the
  overlay, and the pause menu / in-game controls hint / save-state buttons all work with FDS.

**Unit (JVM):** `CoreManagerTest` green — NES resolves to FCEUmm; N64 still null; SNES/Arcade
unchanged.

**Not re-driven via live UI on the clean build (low risk):** opening an FDS title from the real
detail screen. Reason: blind adb D-pad navigation on the leanback ONN could not reliably reach a
specific FDS tile. The underlying path is identical to what the spike proved (the only difference vs.
the spike build is the reverted temporary manifest `exported` flag, which only affected
adb-launchability), and the "Jugar" gate is unit-verified non-null for NES.

## Known limits / follow-ups

- **Multi-disk FDS**: single-disk titles (e.g. DK Jr.) work. Games with a Disk A/B side swap may need
  a disk-control action our UI doesn't expose yet — a **disk-swap UI** is the natural follow-up
  (Spec §5) if the user hits a "please turn the disk" prompt. Not exhaustively sampled.
- **Box art**: `nes.json` is No-Intro cartridge names, so most of the user's Japanese/translated FDS
  titles won't match and show the branded `DefaultCover` (expected).
- **BIOS reminder**: FDS needs `disksys.rom` in `/Download/ROMs/system/` — now present.
