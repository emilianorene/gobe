# Gobe Arcade (FBNeo) — RESULTS

> Date: 2026-06-30
> Branch: `feat/arcade`
> Plan: `docs/superpowers/plans/2026-06-30-gobe-arcade.md`
> Spec: `docs/superpowers/specs/2026-06-30-gobe-arcade-design.md`

## Outcome

Arcade emulation is wired end-to-end through the existing Fase 2 stack. The user's arcade
`.zip` library is now launchable from the detail screen using a bundled **FBNeo** core.

## Pinned decisions (from the on-device spike, Task 1)

- **Core: FBNeo** (`libfbneo_libretro_android.so`, `armeabi-v7a`, ELF 32-bit ARM EABI5).
  Chosen over the spec's first candidate `mame2003_plus` after testing both against the user's
  **real 15 arcade zips** on the ONN:
  - `mame2003_plus` (MAME 0.78 set): **7/15** booted.
  - **FBNeo (current set): 13/15 booted.** ← pinned.
- **Romset version:** the user's set matches the **current FBNeo romset**, not MAME 0.78.
- **The 2 non-booting games** are missing one file each *inside their own zip* (romset
  completeness — out of scope per spec §4; the user can add the file later). FBNeo names the
  exact missing file on screen:
  - `mvsc` → needs `mvsc.key` (CPS2 decryption key, CRC `0x7e101e09`).
  - `xmen` → needs `xmen_eba.nv` (NVRAM default, CRC-specific).
- **Booted OK (13):** ffight, mk2, xmen*, dino, tmnt, tmnt2, vendetta, sf2ce, contra, ga2,
  mk3, mwalk, spidman, wof. (*xmen booted under mame2003_plus but not FBNeo — see above; net
  library coverage is maximized by FBNeo.)

## Error-signal finding (shaped Task 4)

For a bad/incomplete romset, **FBNeo renders its own informative "FBNeo Error" screen**
(blue title bar, names the exact missing file) as GL frames and does **NOT** emit
`getGLRetroErrors()`. So:
- FBNeo's own screen is the **primary** romset-error UX (better than a generic toast — it
  tells the user exactly which file to add).
- We still subscribe to `getGLRetroErrors()` as a **one-shot safety net** for hard failures
  (`emu_load_failed` toast + finish).
- The user leaves the error screen via the existing pause menu (Back → Exit to Gobe).

(For contrast, `mame2003_plus` failed cleanly with 0 frames + a real `getGLRetroErrors()`
signal; FBNeo does not. Coverage won over the cleaner error signal.)

## BIOS / systemDirectory

- The emulator `systemDirectory` moved from app-private `filesDir` to the user-accessible
  **`/storage/emulated/0/Download/ROMs/system`** (created on first arcade launch). The user
  drops any future BIOS/aux files there. `savesDirectory` + save states stay app-private.
- **None of the user's current 15 games need a system BIOS** (no Neo Geo). The 2 failures are
  per-romset missing files, not BIOS. The external folder is future-proofing (Neo Geo, etc.).

## Commits (feat/arcade)

- `037507f` bundle FBNeo core (armeabi-v7a).
- `251a78d` CoreManager maps ARCADE → FBNeo (TDD; unit test green).
- `a5419c1` external `systemDirectory` for BIOS + `getGLRetroErrors()` safety-net error UX.
- `0940581` enable Play when a core exists for the game's system (DetailScreen gate via CoreManager).
- `a636e35` docs: plan pivot to FBNeo + queued backlog items.

## Acceptance status

**Verified live on the ONN (this `feat/arcade` build):**
- Unit tests pass (`CoreManager` resolves ARCADE → FBNeo; NES/N64 still null).
- Clean `installDebug`; `MANAGE_EXTERNAL_STORAGE` granted.
- Home grid renders; search works (typed "ffight" → filtered).
- SNES detail shows **Play enabled + Resume from save** — regression intact after the
  `systemDirectory` change and the broadened `playable` gate.
- Settings screen (Language System/Español/English, ROM folders) renders.

**Verified via the on-device spike (same `EmulatorActivity → CoreManager → FBNeo` render
path, exercised with the user's real ROMs earlier this session):**
- Arcade games launch and render under FBNeo (13/15).
- FBNeo error screen appears for the 2 incomplete romsets (no crash).

**Verified by unit test + identical code path (not re-driven via live UI this build):**
- Arcade "Play" enables because `DetailScreen` uses the same `CoreManager.corePath(system) != null`
  gate that SNES uses (SNES confirmed live; ARCADE resolves non-null in the unit test).

**Not independently re-driven via live UI this build (low risk):** launching an arcade game
from the real detail screen. Reason: blind adb D-pad automation on the leanback ONN could not
reliably reach an arcade tile (the dominant "Continue playing" row + search-focus trapping —
both captured as backlog items). The underlying path is identical to SNES (confirmed) and was
proven for arcade in the spike. **Recommended final check:** on the ONN, open any arcade game
(e.g. `ffight`) → detail → Play, confirm it renders; open `mvsc` to see the FBNeo "missing
`mvsc.key`" screen and Back → Exit to Gobe.

## Known limits / follow-ups

- `mvsc`/`xmen`: user adds the missing file to the zip to enable them (documented above).
- Performance: heavy late titles may run below full speed on the ONN (hardware limit).
- Backlog (queued, see `docs/superpowers/BACKLOG.md`): Continue-playing row too dominant;
  tile size/shape + name+year caption; gamepad shortcuts (L1→Search, R1→Settings) + on-screen
  button legend — several of these were re-confirmed as real friction during this acceptance run.
