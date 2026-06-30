# Gobe UI Redesign + Box Art + Players + Genre + In-Game Menu — Results

> Date: 2026-06-30
> Branch: `feat/ui-redesign`
> Plan: [2026-06-30-gobe-ui-redesign.md](2026-06-30-gobe-ui-redesign.md)
> Spec: [../specs/2026-06-30-gobe-ui-redesign-design.md](../specs/2026-06-30-gobe-ui-redesign-design.md)

## Outcome: ✅ The library is a searchable box-art grid with badges, genre/year, and a gamepad in-game menu.

Verified on the ONN Plus 4K (Android 14) with the user's real ~1600-game library.

## What shipped

- **Searchable grid Home (layout C):** search field (live Room `LIKE` filtering, verified
  with "ma" → Mario/Magic/Animaniacs/…), system filter chips (All/NES/SNES/N64/Arcade), a
  `LazyVerticalGrid` of box-art tiles, and the Continue-playing row. Settings reachable via UP.
- **Real box art** from libretro-thumbnails via Coil (on-demand, disk-cached), shown with
  `ContentScale.Fit` so covers are **not cut**. A **branded default cover** (Gobe logo
  watermark + title + system) for games without art.
- **Player badges** `👥N` (legible dark pill) on multiplayer games.
- **Detail screen:** full cover (Fit) + Players + **Genre** + **Year** + size + Play /
  Resume-from-save / Back.
- **Metadata pipeline:** `tools/build-metadata-index/build.py` merges libretro-thumbnails
  (box-art filenames) + the libretro `.rdb` (MessagePack: players, genre, releaseyear) into
  `assets/metadata/{snes,nes,n64,arcade}.json` (~1 MB). `NameNormalizer` handles leading and
  trailing (No-Intro) articles. Matched + persisted in Room on rescan (Room v1→v2→v3,
  additive migrations).
- **In-game menu:** **Select+Start** (or gamepad Home/Mode, or remote Back) opens the pause
  menu from a running game (face buttons don't send Android Back). **"Exit to Gobe"**
  auto-saves and returns to the **grid** (via a launch flag consumed on resume).

## Metadata coverage (SNES, user's main library)

- Box-art names: 3767 thumbnails → 2282 normalized keys. Players: 3701 RDB entries → 2156
  keys. Genre present on ~half. Coverage is partial by nature (messy names, regional dumps);
  unmatched games show the branded default cover and no badge — by design.

## Tests

- **Unit (JVM):** BoxartUrlBuilder, NameNormalizer (incl. articles), MetadataIndex, GameMatcher,
  + existing suite — green.
- **Instrumented:** Room migrations + `searchGames`/`updateMeta` (players/boxart/genre/year) —
  green on device.
- **On-device (screenshots):** grid with full covers + badges, default covers, live search,
  detail with genre/year, Fit fix for grid and detail.

## Deviations / fixes during build

1. **Box-art source = thumbnails repo, not RDB names** — the RDB "name" doesn't always match
   the thumbnail filename (e.g. "2020 - Super Baseball (SNK) (USA) (Alt 1)" vs
   "2020 Super Baseball (USA)"); using the thumbnails listing fixes box-art hit rate.
2. **NameNormalizer extended** (leading + trailing articles) to match No-Intro "Name, The".
3. **ContentScale.Crop → Fit** for grid tiles and the detail cover (user feedback: covers cut).
4. **Branded default cover** added (user request) instead of plain text fallback.
5. **Genre + year** added (user request) — Room v3, shown in detail; a genre *filter* is a
   planned follow-up.
6. **Metadata generator** uses a Python venv with `msgpack`; the RDB is a RARCHDB+MessagePack
   container (16-byte header, then maps).

## Pending hands-on check

- **Select+Start in-game menu** with a gamepad: the code is in and swallows the combo, but the
  combo + "Exit to Gobe → grid" should be confirmed live on the controller.

## Follow-ups (out of scope)

- Genre filter/browse on Home (data is ready).
- More consoles (NES `.fds`/BIOS, Arcade, N64).
- Controller management / remapping / multi-port.
- Detail cover for unmatched games could reuse the branded default placeholder.
