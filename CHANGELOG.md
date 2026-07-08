# Changelog

All notable changes to Gobe are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project aims to follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Each released `app-release.apk` is attached to its tag on the
[Releases](../../releases) page.

## [Unreleased]

_Nothing yet._

## [0.6.0-beta] — 2026-07-08

**Home redesign** — a cinematic, console-first home screen.

### Changed

- **Console carousel Home** — the home screen is now a full-screen carousel showing one console at a
  time as the hero: its controller art floats on the dark stage lit by that console's accent glow, and
  moving left/right re-tints the whole background to the focused console's color. Replaces the flat grid
  of tiles, making the console choice the main event.
- **Continue-playing is now per-console** — a slim strip at the bottom that appears only when the
  focused console has games you've recently played, instead of one global row.
- **Recommended / Favorites moved into the library** — no longer top-level Home sections; inside a
  console's library, chips filter to ★ Recommended or ♥ Favorites, and the genre chips adapt to the
  active filter.

### Added

- Real controller artwork for **NES, SNES and N64** (public-domain photos, background-removed to float
  on the dark UI). Arcade keeps a placeholder controller icon until a suitable image is sourced.

## [0.5.0-beta] — 2026-07-07

**Nintendo 64** — the fourth console comes online.

### Added

- **N64 emulation** — Nintendo 64 games are now playable, powered by the Mupen64Plus-Next core
  (GLideN64, OpenGL ES 3.2). Drop `.z64`/`.n64`/`.v64` files in your ROMs folder and they show up in
  the N64 library with cover art and metadata like every other console; the **Play** button is now
  live. First-party titles run well on the reference hardware (ONN 4K); more demanding games may show
  glitches or slowdowns — a limit of the hardware, not the app.

## [0.4.0-beta] — 2026-07-07

**Navigation** — a faster, denser way to browse and read about your games.

### Changed

- **Master–detail game browsing** — the console/library and search screens are redesigned as a
  two-pane layout: a vertical **poster rail** (cover + name + year·genre + ★/♥) on the left and a
  **live detail panel** on the right that updates as you move the highlight — so you see a game's
  cover, description, and metadata without leaving the list. **Ⓐ launches the game directly**;
  **Right** enters the panel for Play / Resume / Favorite; **L1/R1** jump a page in the rail.
  Replaces the old cover-tile grid, showing more games per screen and cutting presses-to-info to zero.

### Internal

- Shared `GameCover` and `GameDetailPanel` composables (the panel is reused by the full-screen
  detail view); pure, unit-tested `LibraryKeyMap` (L1/R1 → page) and `rowSubtitle` helpers.

## [0.3.0-beta] — 2026-07-06

**Library & content** — make a large collection pleasant to browse.

### Added

- **Console-first Home** — a two-level layout: pick a collection or console, then browse its games.
  Level-1 sections are **Recommended**, **Favorites**, and one per console; level-2 is the game
  library with a genre filter and sort. Back navigation returns to the sections.
- **Console tiles with controller art** — each Home section is now a **Hero card**: the console's
  controller (NES pad, SNES pad, N64 trident, arcade stick) on a per-console color glow, with
  Recommended (★) and Favorites (♥) in the same style.
- **Recommended games** — a ★ badge and a Recommended collection highlight highly-rated titles
  (derived from IGDB ratings, baked into the bundled metadata; no on-device network needed).
- **Favorites** — mark any game with ♥ from its detail screen; a Favorites collection gathers them.
- **Sort options** — order a library by **Recommended**, **Title**, or **Year**.
- **Richer metadata** — game **descriptions** (for recommended titles) on the detail screen, and an
  **IGDB cover** fallback so games without libretro box art still show real cover art instead of the
  placeholder. Both baked into the metadata index from IGDB.

## [0.2.0-beta] — 2026-07-02

Controller & input polish, and a big compatibility fix so Gobe installs on far more devices.

### Added

- **Unassign a controller** — clear a gamepad's player slot directly from the controllers screen.
- **Analog stick deadzone** setting to stop drift on worn sticks.
- **Configurable menu hotkey** — choose **Select + Start**, **L1 + R1**, or **L3 + R3** to open
  the in-game menu.
- **Dynamic on-screen controls hint** reflecting the chosen hotkey.
- **Scrollable Settings screen** so all options are reachable on any TV resolution.
- **FDS multi-disk swap** UI (see known limitation below).

### Changed

- **Lowered `minSdk` from 30 (Android 11) to 24 (Android 7.0)** so Gobe installs on the vast
  majority of Android TV / Google TV / Fire TV devices. `minSdk 30` caused a **"problem parsing
  the package"** error on any device running Android 10 or older.

### Docs

- README: much more detailed **step-by-step install** (unknown sources → get the APK → install)
  and **game setup** (folder layout → copy files → FDS BIOS → scan) instructions for Android TV.
- README: added a **Requirements** section with a compatible-device table, plus troubleshooting
  for the package-parse error and the Google Play Protect sideload warning.

### Known limitations

- **FDS disk-swap button doesn't appear** yet — the multi-disk swap UI shipped but the button to
  flip/insert the next disk is not showing (under investigation). Single-disk `.fds` games work.
- Carried over from 0.1.0-beta: USB controllers not detected on the ONN (use Bluetooth), no
  rumble on that hardware.

## [0.1.0-beta] — 2026-07-01

First public beta. A working Android TV emulator frontend tested on an ONN Google TV 4K Plus.

### Added

- **Emulation** via LibretroDroid 0.14.0 with bundled cores:
  - SNES (snes9x), Arcade (FBNeo), NES / Famicom Disk System (FCEUmm).
  - FDS `.fds` support (requires the `disksys.rom` BIOS in `Download/ROMs/system/`).
- **Save states + SRAM**, with a pause menu (Resume / Save / Load / Exit).
- **Library**:
  - Automatic ROM scanning across configurable folders.
  - Box-art grid with cover art, game title and year.
  - Search, plus **filter by system and genre**.
  - **Continue playing** row for recently played games.
  - Per-game metadata: players, genre, year.
- **Controllers**:
  - Detection of connected gamepads with a live button/stick test panel.
  - Assign each controller to a **player (P1–P4)**.
  - Quick **Swap A/B** and **Swap X/Y** presets.
  - Full **button remapping by capture** (press the physical button to bind it), per controller.
  - All controller settings persist per device.
- **Navigation & UX**:
  - Gamepad shortcuts — **L1** opens search, **R1** opens settings — with an on-screen button legend.
  - Storage-permission onboarding screen.
- **Localization**: English and Spanish.
- **Release signing**: APKs are signed with Gobe's stable release key so future versions update in place.

### Known limitations

- Tested on a single device (ONN Google TV 4K Plus, 32-bit `armeabi-v7a`).
- USB controllers are not detected on the ONN (its USB-C port is power-only) — use Bluetooth.
- No rumble on this hardware.
- No disk-swap UI yet for multi-disk FDS games.

[Unreleased]: ../../compare/v0.2.0-beta...HEAD
[0.2.0-beta]: ../../compare/v0.1.0-beta...v0.2.0-beta
[0.1.0-beta]: ../../releases/tag/v0.1.0-beta
