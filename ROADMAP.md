# Gobe Roadmap

A living plan for where Gobe is headed. Dates are directional, not commitments — this is a personal
project. Items graduate to the [CHANGELOG](CHANGELOG.md) as they ship.

## ✅ v0.5.0-beta — _current_ — Nintendo 64

The fourth console goes live.

- **N64 emulation** — Nintendo 64 games are now playable via the Mupen64Plus-Next core (GLideN64,
  OpenGL ES 3.2). ROMs (`.z64`/`.n64`/`.v64`) appear in their own console library with art and
  metadata; the detail-panel **Play** button, previously "coming soon", now launches the game.
  First-party titles run well on the reference ONN 4K; heavier games may glitch/slow (hardware limit).

## ✅ v0.4.0-beta — Navigation

A faster, denser way to browse a big library on a TV.

- **Master–detail game browsing** — the console/library and search screens become a two-pane layout:
  a vertical **poster rail** (cover + name + year·genre + ★/♥) plus a **live detail panel** that
  updates as you move the highlight. Ⓐ launches directly, Right opens the panel's actions, L1/R1
  page-jump. Replaces the cover-tile grid — more games per screen, zero presses to see a game's info.

_First slice of the broader Android TV UI/UX redesign; Home, in-game pause and further polish are
follow-ups (see `docs/superpowers/BACKLOG.md`)._

## ✅ v0.3.0-beta — Library & content

Make a large collection pleasant to browse.

- **Console-first Home** — a two-level layout (collection/console → game library); level-1 sections
  are Recommended, Favorites, and one per console. Absorbs the old "per-system landing" idea.
- **Console tiles with controller art** — Home sections are Hero cards showing each console's
  controller on a per-console color glow (★ Recommended / ♥ Favorites in the same style).
- **Recommended games** — ★ badge + Recommended collection from IGDB ratings (baked in, no network).
- **Favorites** — ♥ toggle on the detail screen + a Favorites collection.
- **Sort options** — Recommended / Title / Year.
- **Richer metadata** — descriptions (recommended titles) + an IGDB cover fallback for games missing
  libretro box art.

Still open from the original v0.3 wishlist: **manual art override** for mismatched games, and
last-played / most-played sort — moved to a later release / the backlog.

## ✅ v0.2.0-beta

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

## 🚀 Next — Library polish

Follow-ups from the v0.3 wishlist not yet shipped.

- **Manual art override** for games the scanner mismatches.
- **More sort options** (last played / most played).
- **Recently added** row on the home screen.
- **Developer/publisher** in metadata.

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
