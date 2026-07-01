# Changelog

All notable changes to Gobe are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project aims to follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Each released `app-release.apk` is attached to its tag on the
[Releases](../../releases) page.

## [Unreleased]

_Nothing yet._

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

[Unreleased]: ../../compare/v0.1.0-beta...HEAD
[0.1.0-beta]: ../../releases/tag/v0.1.0-beta
