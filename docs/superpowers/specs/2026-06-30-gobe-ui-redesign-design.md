# Gobe — UI Redesign + Box Art + Player Counts + In-Game Menu Design

> Date: 2026-06-30
> Scope: Redesign the library UI (search + grid + filter chips), add real box art and
> player-count badges, and add a gamepad-accessible in-game menu with "Exit to Gobe".
> Builds on: Fase 0/1/2 + i18n (all merged to `main`).

## 1. Purpose

Make Gobe's library easy to browse with ~1600 games and visually rich: a searchable box-art
grid with per-system filter chips, player-count badges (2/3/4), and a game detail with cover
art. Also fix a real gap from Fase 2: with a gamepad there was **no way to open the in-game
menu** (face buttons don't send Android Back), so the player couldn't exit a game to pick
another. This adds a standard **Select+Start** gesture and an **"Exit to Gobe"** action that
returns to the grid without closing the app.

Single sub-project (the user chose "all together": UI + box art + players + in-game menu).
The implementation plan phases it; the spec covers the whole.

## 2. Decisions captured during brainstorming (with visual mockups)

- **Home layout = "C": grid + top filter chips + search.** A `LazyVerticalGrid` of box-art
  tiles, system chips (All / SNES / NES / Arcade / N64), a search field on top, and a fixed
  "Continue playing" row above the grid.
- **Detail:** large cover + name + system + player count + last-played/save-state + Play /
  Resume-from-save / Options / Back.
- **In-game menu gesture = Select+Start** (RetroArch-standard; also gamepad Home/Mode if
  present; Back on the TV remote). Menu: Resume / Save state / Load state / **Exit to Gobe**.
- **Box art = libretro-thumbnails**, loaded on-demand via Coil with disk cache; fallback to
  the text tile when there's no match.
- **Player counts = bundled offline dataset** derived from the libretro database, matched by
  normalized name; no per-game network. Partial coverage is acceptable (no badge when unknown).

## 3. In scope

- New `HomeScreen`: search field, system filter chips, `LazyVerticalGrid` of tiles, fixed
  "Continue playing" row; live filtering by query + selected system.
- `GameTile` with box art (Coil) + player badge + text fallback; existing focus visuals.
- New `DetailScreen` layout with cover art + player count.
- **Metadata index:** bundled per-system JSON (`assets/metadata/<system>.json`) mapping
  normalized name → `{ boxartName, players }`, generated offline by a committed tool.
- `GameMatcher` (normalize + match → players/boxartName) and `BoxartUrlBuilder` (system +
  name → libretro-thumbnails URL) — both pure/unit-tested.
- Room migration (db v1 → v2): add `players: Int?` and `boxartName: String?` to `Game`;
  populate during scan/rescan via the matcher.
- Coil (`coil-compose`) for async image loading + disk cache.
- In-game menu: detect Select+Start (and Home/Mode) in `EmulatorActivity`; "Exit to Gobe"
  auto-saves and returns to the library grid.
- All new strings localized (en default + es), per the i18n setup.

## 4. Out of scope (deferred)

- More consoles (NES `.fds`/BIOS, Arcade, N64) — next sub-project after the UI.
- Controller management / remapping / multi-port routing — the sub-project after that.
- Screenshot/title thumbnails (only box art for now), boxart scraping by CRC (we match by
  name), and per-game manual artwork override.
- Online metadata lookups.

## 5. Architecture

### 5.1 Library UI (`com.gobe.tv.ui.home` + `ui/search`)

- `HomeScreen`: top bar (logo, search field, ⚙ Settings); a row of system **filter chips**;
  a fixed **Continue-playing** row (when non-empty); a `LazyVerticalGrid` of `GameTile`s.
- `HomeViewModel`: holds `query: String` and `selectedSystem: System?`; exposes a filtered,
  grouped state. Filtering is done in Room (a `searchGames(query, system)` DAO query using
  `LIKE`) to scale to 1600 rows, observed as a Flow. "Continue playing" stays a separate
  query (existing `observeContinuePlaying`).
- **Search input:** a Compose text field that raises the system on-screen keyboard (Android TV
  leanback IME); results filter live as the query changes. D-pad: focus the search field from
  the top; chips and grid below.
- `GameTile`: `AsyncImage` (Coil) of the box-art URL with a text-tile fallback on error/empty;
  a player badge overlay (`👥N`) when `players >= 2`; current focus scale/border.

### 5.2 Metadata & box art (`com.gobe.tv.data.metadata`, `data/art`)

- `MetadataIndex`: loads `assets/metadata/<system>.json` (lazily, per system) into a map
  `normalizedName -> GameMeta(boxartName, players)`.
- `GameMatcher`: `normalize(name)` (lowercase, strip tags/punctuation/articles) + `match(game)`
  → `GameMeta?`. Pure, unit-tested.
- `BoxartUrlBuilder`: `url(system, boxartName)` →
  `https://thumbnails.libretro.com/<System Full Name>/Named_Boxarts/<URL-encoded name>.png`.
  Pure, unit-tested (system→folder map; encoding of spaces/`&`).
- **Persistence:** during `rescan()`, the repository matches each new game and stores
  `players` + `boxartName` in Room. Tiles build the box-art URL from `boxartName` (fallback:
  the cleaned displayName) and hand it to Coil; Coil handles download + disk/memory cache.
- **Index generation (build-time tool, committed):** `tools/build-metadata-index.*` reads the
  libretro database (RDB / `metadat` `.dat`) for the supported systems and emits the per-system
  JSONs. Output is committed as assets so the app needs no build-time network. The exact RDB
  parsing is finalized during implementation; **fallback** if RDB parsing is impractical: use a
  curated/openvgdb-derived players dataset for the supported systems (documented in RESULTS).

### 5.3 In-game menu (`com.gobe.tv.emulation`)

- `EmulatorActivity` tracks pressed gamepad buttons; **Select+Start** (KEYCODE_BUTTON_SELECT +
  KEYCODE_BUTTON_START held together) opens the pause overlay. Also open on KEYCODE_BUTTON_MODE
  (gamepad Home) and KEYCODE_BACK (remote). While the overlay is open, input is not forwarded
  to the core and the core is paused (existing behavior).
- The overlay (existing `PauseOverlay`) gains/keeps: Resume / Save state / Load state / **Exit
  to Gobe**. "Exit to Gobe" auto-saves (state + SRAM) and `finish()`es; the library then shows
  the grid (the detail's Back already returns to the grid; "Exit to Gobe" may navigate the
  library straight to Home for convenience).
- The combo must not also reach the core as input (swallow Select/Start while the combo fires).

### 5.4 Data model / Room

- `GameEntity` gains `players: Int?` and `boxartName: String?` (nullable). DB version 1 → 2
  with a `Migration(1, 2)` adding the two columns. Existing data preserved; values populated on
  the next rescan.

## 6. Error handling

- No box-art match / network failure → Coil error → text-tile fallback; never blocks the grid.
- No player metadata → no badge (silent).
- Missing/corrupt metadata asset → treat as empty index (no crash); log.
- Room migration must be additive and safe; instrumented-tested.

## 7. Testing

- **Unit (JVM):** `GameMatcher.normalize`/`match`; `BoxartUrlBuilder.url` (folder map +
  encoding); metadata JSON parsing; `HomeViewModel` filter logic (query + system).
- **Instrumented:** Room migration 1→2 (columns added, data preserved); `searchGames` DAO.
- **On-device:** box art loads and caches on the ONN; badges show correct counts on known
  games; search filters live; Select+Start opens the menu from a running game with a gamepad;
  "Exit to Gobe" returns to the grid to pick another game.

## 8. Risks

1. **Name matching coverage** — messy ROM names vs canonical libretro names; partial hits
   expected. Mitigated by normalization + text fallback; measured in RESULTS.
2. **Players dataset generation** — parsing libretro RDB is non-trivial; fallback dataset
   documented. This is the main effort/uncertainty.
3. **Box art needs internet on the ONN** (it has it); first load per game downloads, then cached.
4. **Room migration** correctness — additive, instrumented-tested.
5. **TV search keyboard** ergonomics — the system IME on TV is usable but not elegant; acceptable
   for v1, refine later if needed.
6. **Combo input** — Select+Start must be reliably detected without leaking to the core.

## 9. Defaults (change on request)

- Coil for images; box art only (no screenshots); `LazyVerticalGrid` ~6–7 columns on 1080p;
  player badge shown for `players >= 2`; metadata assets per system under `assets/metadata/`;
  Select+Start primary gesture (Home/Mode + remote Back also work).
