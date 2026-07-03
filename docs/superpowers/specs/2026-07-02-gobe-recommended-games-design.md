# Gobe — Recommended / Essential Game Badge

> Date: 2026-07-02
> Scope: Tag each game as "recommended" (a curated best-of set per console, derived from public
> rating data at build time), show a badge on the tile + detail, and let the user filter to only
> recommended games and sort recommended-first. Works for any user's library out of the box.

## 1. Purpose

The user has a large library and wants to separate the worthwhile games from the junk. Give every
game an at-a-glance **"Recommended"** mark (no numeric score) sourced from public rating data, plus a
**filter** to show only recommended and a **sort** that surfaces them first. It must work for *any*
person's library automatically — the tagging is by game identity, not tied to one user's ROMs.

## 2. Decisions captured during brainstorming

- **Indicator = a badge only** ("Recommended"), no visible score. A game is recommended or not.
- **Curation is per console**, target the essential set of each system: **rating threshold, with a
  floor of the top ~100 and a soft cap of ~200** per console (quality decides; the floor/cap keep it
  from being too sparse or so large the badge loses meaning).
- **Usage = a "Recommended only" filter** on Home + **sort recommended-first** in the grid. No
  dedicated "Essentials" section for now.
- **Generic / prebundled**: the recommended flag ships *inside the app's metadata index* (same
  offline, name-normalized matching the app already uses for boxart/players/genre/year), so anyone's
  library gets tagged on scan with zero setup. Not a live API call on device.
- **Data source = IGDB** (`total_rating` 0–100 + `total_rating_count`), fetched at build time by the
  maintainer's `build.py`. End users never hit the API.

## 3. In scope

### 3.1 Data model
- `MetadataIndex` / `GameMeta`: add `recommended: Boolean = false`. The JSON only carries
  `"recommended": true` for recommended entries (omitted → false) to keep the assets small.
- `GameEntity` (Room): add `recommended: Boolean = false`, populated at scan time via `GameMatcher`
  like the other metadata fields. (No numeric rating stored in the app — only the boolean is needed
  for the badge/filter/sort; the score is used only at build time to derive the flag.)

### 3.2 Build pipeline (`tools/build-metadata-index/build.py`)
- New stage per system: query IGDB for that platform's games with `name`, `total_rating`,
  `total_rating_count`; normalize each IGDB name with the SAME `normalize()` already in the tool;
  keep the best score per normalized key.
- **Curation policy** (pure function, self-tested in the tool): from the list of
  `(key, rating, count)`, choose the recommended set:
  1. Candidates must have `count >= MIN_VOTES` (drop obscure games with ~1 rating).
  2. Primary: `rating >= RATING_THRESHOLD` (e.g. 75).
  3. **Floor**: if fewer than 100 pass, include the top-100 by rating (that still meet MIN_VOTES),
     down to the library size if a system has <100 rated games.
  4. **Cap**: if more than ~200 pass, keep the top ~200 by rating.
  The concrete constants (`RATING_THRESHOLD`, `MIN_VOTES`, floor=100, cap=200) are tunable; the tool
  **prints the resulting recommended count per console** so they can be adjusted.
- Merge `recommended=true` onto the existing per-system entries (by normalized key) before writing
  `assets/metadata/<tag>.json`. Games in the recommended set that have no boxart entry are still
  written (name + recommended), so the badge shows even without art.
- IGDB auth via env vars (`IGDB_CLIENT_ID`, `IGDB_SECRET`) — maintainer-side only; documented in the
  tool's header. If creds are absent, the stage is skipped and the index builds without the flag
  (graceful, so the build never hard-fails on a fresh clone).

### 3.3 Matching / tagging (unchanged mechanism)
- `GameMatcher.match` already returns a `GameMeta` for a normalized display name; the scanner copies
  `meta.recommended` into the `GameEntity`. Any library, any user — a game is tagged iff its cleaned
  name matches a recommended entry in the bundled index. Homebrew/hacks not in the dataset stay
  untagged (correct — they aren't "essential").

### 3.4 UI
- **Badge on the tile**: a small "★ Recommended" mark in a corner of the cover (only when
  `recommended`). Reuse the existing tile overlay style (like the players "· N" chip). New string
  `game_recommended` ("Recommended" / "Recomendado").
- **Badge on the detail screen**: a "★ Recommended" line near the system/genre/year metadata.
- **Filter chip**: a "Recommended" toggle chip in the Home filter row (alongside system/genre). When
  on, the grid shows only recommended games. Combines with the existing system + genre filters.
- **Sort recommended-first**: the grid query orders `recommended DESC` before the current ordering,
  so the good stuff floats up even with the filter off.

### 3.5 Persistence / query
- Extend the games DAO: add `recommended` to the entity + a `recommendedOnly` argument to the
  library query (`searchGames(query, system, genre, recommendedOnly)`), and `ORDER BY recommended
  DESC, <existing order>`. Room schema version bump + migration adding the `recommended` column
  (default 0); since the games table is a rebuildable scan cache, a destructive fallback that
  re-scans is acceptable if simpler.

## 4. Out of scope (deferred)

- A visible numeric score / stars, or sorting by score within the recommended set.
- A dedicated "Essentials" browse section or per-console top rows.
- Manual per-game override of the recommended flag.
- Multiple rating sources / blending (IGDB only for now).
- Popularity/"most players" as a separate signal beyond IGDB's rating count.

## 5. Architecture

- **Pure/testable (JVM):** JSON parsing of the new field (`MetadataIndex.parse`), `GameMatcher`
  tagging, and the DAO query behavior. No new pure module strictly needed on the Kotlin side; the
  curation *policy* lives in `build.py`.
- **Build tool (Python):** a pure `select_recommended(entries) -> set[key]` function implementing
  §3.2 with an inline assert-based self-test; an IGDB fetch function; wiring to merge the flag into
  the existing output.
- **App wiring:** `GameEntity` + DAO + scanner copy + `HomeViewModel` filter state + `HomeScreen`
  chip + `GameTile` badge + `DetailScreen` line + strings (EN/ES).

## 6. Data flow

1. **Build time (maintainer):** `build.py` → IGDB ratings → curation policy → `recommended:true`
   baked into `assets/metadata/<tag>.json`, committed.
2. **Scan time (any device):** scanner cleans each ROM name → `GameMatcher` → `GameMeta.recommended`
   → stored on `GameEntity`.
3. **Browse:** Home reads the filter chip + system/genre; the DAO returns games filtered by
   `recommendedOnly` and ordered recommended-first; tiles/detail render the badge.

## 7. Error handling / edge cases

- No IGDB creds at build → stage skipped, index still builds (no flags); the app simply shows no
  badges. Never hard-fails.
- A recommended game the user doesn't own → simply absent (index is a superset; only owned+matched
  games are tagged).
- Name mismatch (IGDB title ≠ ROM cleaned name) → that game just isn't tagged; no crash. The tool
  logs unmatched high-rated titles so coverage can be improved.
- Old DB without the column → migration adds it defaulting to false; a re-scan repopulates from the
  index.
- Filter on with zero recommended games owned → the grid shows the existing "no games" empty state.

## 8. Testing

- **Unit (JVM):** `MetadataIndex.parse` reads `recommended` (true when present, false when absent);
  `GameMatcher` propagates `recommended`; the DAO returns only recommended when `recommendedOnly` is
  set and orders recommended-first (Room instrumented test or a query-logic test consistent with the
  existing DAO tests).
- **Build tool (Python):** `select_recommended` self-test — threshold selection, MIN_VOTES drop,
  floor-to-100 when few pass, cap-to-200 when many pass, and a system with <100 rated games.
- **On-device:** badge appears on known classics (e.g. Super Metroid, Zelda) and not on shovelware;
  the "Recommended" chip filters the grid; recommended sort-first holds with the chip off.

## 9. Risks

1. **IGDB ↔ ROM name matching** (main risk): IGDB titles differ from No-Intro/redump/ROM names
   (subtitles, regional names, punctuation). The shared `normalize()` helps but won't catch
   everything; coverage will be partial. Mitigation: log unmatched high-rated titles; accept partial
   coverage for v1 (a missing badge is harmless); optionally add a small manual alias list later.
2. **IGDB platform scoping** for Arcade/MAME is fuzzy (MAME set names vs IGDB arcade titles) — arcade
   coverage may be weaker than console; documented, tunable.
3. **Threshold tuning** — the right `RATING_THRESHOLD`/`MIN_VOTES` need a look at the printed
   per-console counts; first run is a calibration pass, not final.
4. **Asset size** — adding recommended entries (incl. those without boxart) grows the JSON modestly;
   acceptable (still small text assets).

## 10. Defaults (change on request)

- Badge label "Recommended" / "Recomendado" with a ★; corner of the tile + a detail line.
- Policy: `RATING_THRESHOLD≈75`, `MIN_VOTES` small (e.g. 5), floor 100, cap ~200 per console — tuned
  after the first build prints real counts.
- IGDB as the sole source; boolean flag only (no score in-app); filter + sort-first; per-console.
