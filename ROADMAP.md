# Gobe Roadmap

A living plan for where Gobe is headed. Dates are directional, not commitments — this is a personal
project. Items graduate to the [CHANGELOG](CHANGELOG.md) as they ship.

## ✅ v0.2.0-beta — _current_

Controller & input polish, plus much wider device support.

- **Unassign a controller** (clear its player slot) directly from the controllers screen.
- **Analog stick deadzone** setting to stop drift on worn sticks.
- **Configurable menu hotkey** (Select + Start / L1 + R1 / L3 + R3).
- **Wider install support**: `minSdk` lowered 30 → 24, so Gobe installs on Android 7.0+ devices
  (Fire TV, Mi Box, most TV boxes) instead of only Android 11+.
- ⚠️ **FDS multi-disk swap** UI shipped but has a **known bug** — the disk-swap button doesn't
  appear yet (under investigation). Single-disk `.fds` games are unaffected.

## ✅ v0.1.0-beta

The first public, playable build. Emulation (SNES / Arcade / NES-FDS), a box-art library with
search + genre/system filters, save states, and full controller configuration (player assignment,
A/B–X/Y swaps, capture remapping). Signed releases with in-place updates.

## 🚀 v0.3 — Library & content

Make a large collection pleasant to browse.

- **Favorites / recently added** rows on the home screen.
- **Richer metadata**: descriptions, developer/publisher, and higher-quality art matching.
- **Manual art override** for games the scanner mismatches.
- **Sort options** (title / year / last played / most played).
- **Per-system landing** views.

## 🔮 v1.0 — Stable & broader

Graduate out of beta.

- **More cores** (e.g. Genesis, Game Boy / GBA, PlayStation) — subject to `armeabi-v7a` availability
  and performance on the target hardware.
- **Per-game settings** (aspect ratio, shaders/filters, per-game control maps).
- **Cloud/local backup** of saves and settings.
- **Wider device testing** beyond the ONN, and a 64-bit (`arm64-v8a`) build where supported.
- Stability, performance and accessibility passes.

## Backlog / ideas

Unscheduled, tracked in [docs/superpowers/BACKLOG.md](docs/superpowers/BACKLOG.md):

- Netplay, achievements, rewind, screenshot capture, themes.
- USB host support is **won't-fix** on the ONN (hardware limitation — its USB-C is power-only).
- Rumble is not feasible on the current hardware.

---

Have a suggestion? Open an issue on the [Issues](../../issues) page.
