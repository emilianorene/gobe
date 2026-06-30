# Gobe Fase 0 + Fase 1 — Acceptance Results

> Date: 2026-06-30
> Branch: `feat/fase0-1` (local, no remote)
> Plan: [2026-06-30-gobe-fase0-1.md](2026-06-30-gobe-fase0-1.md)
> Spec: [../specs/2026-06-30-gobe-fase0-1-design.md](../specs/2026-06-30-gobe-fase0-1-design.md)

## Outcome: ✅ Fase 0 + Fase 1 complete and verified on the real ONN Plus 4K.

The TV shell + library browser builds, deploys, and runs on the device, scanning the
user's real ROM library (NES `.fds`, SNES `.sfc/.smc`, Arcade `.zip`) into a D-pad
navigable grid grouped by system. No emulation (deferred to Fase 2 by design).

## Environment

- **Toolchain (this Mac):** Homebrew OpenJDK 17 (formula, no sudo), Android cmdline-tools,
  platform-tools, platforms;android-34 + android-35, build-tools 34/35.0.0, Gradle 8.9
  wrapper. Env persisted in `~/.zshrc`.
- **Device:** ONN Plus 4K (`onn__4K_Plus_Streaming`, codename coffey), **Android 14
  (SDK 34)**, paired + connected over Wi-Fi adb (`192.168.1.219`).

## Automated tests

- **JVM unit tests** (`:app:testDebugUnitTest`): **green**. SystemDetector (incl. `.fds`
  and folder-based `.zip`), NameCleaner, RomScanner, LibraryDiff.
- **Instrumented DAO test** (`:app:connectedDebugAndroidTest`): **2 tests passed on the
  ONN** (Room insert/query + duplicate-path ignore).

## On-device acceptance checklist

- [x] App builds (`assembleDebug`) and installs (`adb install -r`) on the ONN.
- [x] Permission onboarding shows on first run; "Conceder acceso" focused.
- [x] After granting `MANAGE_EXTERNAL_STORAGE`, app routes to Home.
- [x] In-app D-pad folder browser used (by the user) to add `Download/ROMs`.
- [x] Library scans the real folder and shows rows: **NES** (`.fds`), **SNES**
      (`.sfc/.smc`), **Arcade** (`.zip`).
- [x] Display names cleaned (region/tags stripped).
- [x] Initial focus lands on the first tile; D-pad moves focus with visible highlight.
- [x] OK opens game detail (name, system, size; "Jugar" disabled; "Volver" focused).
- [x] Back returns Home from detail/folders; (Back from Home exits — by design).
- [x] Text legible on the dark theme (after contrast fix).

Screenshots captured during verification: onboarding, empty state, Home (SNES+Arcade,
then NES+SNES after `.fds` support), folder management, game detail.

## Deviations from the original plan (all justified, committed)

1. **AGP 8.5.2 → 8.6.0, compileSdk 34 → 35** — required by `androidx.tv` 1.0.0 AAR
   metadata. `targetSdk` stays 34, `minSdk` 30.
2. **Compose lists:** `TvLazyColumn/TvLazyRow` do not exist in `tv-foundation` 1.0.0
   (removed in stable); switched to standard `LazyColumn/LazyRow` with focusable `Card`s.
3. **JDK install:** used `brew install openjdk@17` (formula, no sudo) instead of the
   Temurin cask (which needs an interactive sudo password).
4. **RomScanner symlink check:** `Files.isSymbolicLink` instead of canonical/absolute
   comparison (avoids skipping files under symlinked temp ancestors on macOS).
5. **`androidx.test:runner` dependency added** — the instrumented runner class wasn't on
   the test classpath transitively; required for `connectedDebugAndroidTest`.

## Fixes made from review + on-device findings

- **HomeViewModel loading state:** drive `loading` from a real scanning flag so a fresh
  launch shows "Escaneando…" instead of the empty state during the first scan.
- **Initial D-pad focus:** request focus from the tile that owns the FocusRequester (once
  composed), avoiding a no-op that left the remote with no focused element.
- **Contrast:** provide `LocalContentColor` = `onBackground` so tv-material3 `Text` is
  legible (it defaulted to black outside a Surface).
- **Detection extended for the real library:** `.fds` → NES; `.zip` classified by parent
  folder (SNES/N64/NES/Arcade), else ARCADE. Default ROM path set to
  `/storage/emulated/0/Download/ROMs`.

## Known limitations / follow-ups (out of Fase 1 scope)

- A leftover empty default folder `/storage/emulated/0/Roms` may exist from an earlier run;
  removable in-app via "Quitar".
- Rescan runs fully on each folder mutation and Home entry — fine for now; may want
  incremental/async indexing for very large libraries on the ONN.
- USB/removable volumes (`/storage/<uuid>`) still deferred; the real library here was on
  internal storage (`Download/roms`).
- Emulation, boxart scraping, per-core/per-game settings, controller-port management,
  arcade romset validation → later fases.
