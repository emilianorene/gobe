# Gobe — Console-first Home + Favorites + Sort

> Date: 2026-07-05
> Scope: Reorganize the Home around **consoles/collections** (a two-level navigation), add user
> **favorites** (mark + heart badge + a Favorites section), and a **sort selector**
> (Recommended-first / Title / Year). Absorbs the roadmap's "per-system landing" + "favorites" +
> "sort options" v0.3 items into one Home redesign.

## 1. Purpose

The library is large and the flat Home with a growing row of filter chips (system, genre,
Recommended) doesn't scale. Reorganize around a **console-first** structure: Home lists **sections**
(special collections + one per console); entering a section shows that library with genre + sort.
Add **favorites** (user-set) as a first-class section + a heart badge + a detail toggle. This is
tidier, has fewer chips, and scales — adding a console (a `System` enum value) or a collection adds
one tile, not one more chip.

## 2. Decisions captured during brainstorming

- **Console-first, drill-in** (not rows-per-console): Home (level 1) = sections; a section opens a
  library (level 2).
- **Recommended and Favorites are sections, not chips** — same tile pattern as consoles.
- **Sort selector** with three modes: **Recommended-first (default)**, **Title (A–Z)**, **Year**
  (newest first, unknown years last). No "last played" / "most played" (YAGNI; the latter would need
  a new play counter).
- **Favorites are user data** (a persisted per-game flag), toggled from the detail screen, shown as a
  **♥ badge** on the tile. The recommended backfill must not touch it.
- **Global search** lives on level-1 Home; it opens the library in a search-results mode.
- Genre + sort live **inside** each section's library (level 2), scoped to that section.

## 3. Navigation model

Current nav is a single `var route: Route` with a `when(route)` in `GobeNavHost` (no back stack).
Add a **minimal back stack** (a `List<Route>` in `GobeNavHost`) so Home → Library → Detail → Back
returns to Library (preserving its section), and Back again returns Home. Push on navigate, pop on
Back; the emulator-return path still resets to Home.

- `Route`: add `data class Library(val section: LibrarySection)`.
- New `LibrarySection` (sealed, in `ui/` or `domain/`):
  - `data class Console(val system: System)`
  - `data object Recommended`
  - `data object Favorites`
  - `data class SearchAll(val query: String)`
- Flow: Home → `Library(section)` → `Detail(gameId)` → Back → Library → Back → Home.

## 4. In scope

### 4.1 Data model
- `GameEntity`/`Game`: add `favorite: Boolean = false` (user data). `MIGRATION_4_5`:
  `ALTER TABLE games ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0`; bump `version = 4 → 5`; register
  in `GobeApp.addMigrations(...)`. (Recommended used `MIGRATION_3_4`; this follows it.)
- `LibraryRepository.rescan()` backfill: unchanged — it only writes `recommended`, so `favorite`
  (user data) is never cleared by a rescan. (Add a test asserting a favorited game stays favorited
  after rescan.)

### 4.2 DAO / query
- `SortMode` (pure enum, `emulation`/`ui`-agnostic, e.g. `domain` or `ui/home`): `RECOMMENDED` (0),
  `TITLE` (1), `YEAR` (2).
- Extend `searchGames` to
  `searchGames(q, system, genre, recommendedOnly, favoritesOnly, sortMode)`:
  - add `AND (:favoritesOnly = 0 OR favorite = 1)` to the existing filter chain;
  - replace the `ORDER BY` with a `sortMode`-driven `CASE` (one query, DRY):
    ```sql
    ORDER BY
      CASE WHEN :sortMode = 0 THEN recommended ELSE 0 END DESC,
      CASE WHEN :sortMode = 2 THEN year END DESC,
      displayName COLLATE NOCASE ASC
    ```
    Mode 0 = recommended-first then title; mode 1 = title only; mode 2 = year desc (SQLite sorts
    NULLs last in DESC → unknown years last) then title. `displayName` is always the final tiebreaker.
- Add `updateFavorite(id, favorite)` (`UPDATE games SET favorite = :favorite WHERE id = :id`).
- `toDomain()` maps `favorite`. `LibraryRepository.searchGames(...)` gains `favoritesOnly` +
  `sortMode` args; `getGame`/detail returns `favorite`.
- Genres per section: derive the genre list from the **section's** games (distinct genres among the
  current section results) rather than the global list, so a console's genre chips only show genres
  present there. (Compute in the Library ViewModel from the queried list, or a scoped DAO query.)

### 4.3 Level 1 — Home (reworked `HomeScreen`)
- Global **search** box (top) → on submit, navigate to `Library(SearchAll(query))`.
- **Continue playing** row (kept; from `observeContinuePlaying`), tiles → Detail.
- **Sections** grid: tiles for **Recomendados**, **Favoritos**, then **one per `System.entries`**
  (NES, SNES, N64, Arcade). Each tile shows the section label (a count is optional/nice-to-have).
  Selecting a tile → `Library(section)`. Adding a `System` value adds a tile automatically.
- Removes from Home: the system chip row, the genre chip row, and the flat main grid (they move to
  the Library screen).
- `HomeViewModel` shrinks to: scanning state + `observeContinuePlaying`; (optional) per-section
  counts.

### 4.4 Level 2 — `LibraryScreen` + `LibraryViewModel` (new)
- Input: a `LibrarySection`. Base filter derived from it — Console → `system = X`; Recommended →
  `recommendedOnly = 1`; Favorites → `favoritesOnly = 1`; SearchAll(q) → `q` across all systems.
- UI: section **title**; **genre** chips (scoped, incl. "All"); a **sort** control
  (cycling button "↕ <mode>": Recommended → Title → Year); the **grid** of that section's games with
  **★** (recommended) + **♥** (favorite) badges; tiles → Detail; Back → Home.
- `LibraryViewModel` holds `selectedGenre` + `sortMode` (session state, like today's filters — not
  persisted) and calls `repo.searchGames(query, system, genre, recommendedOnly, favoritesOnly, sortMode)`
  with the section's fixed base filter. The 6-input `combine` uses the vararg `combine(...) { arr -> }`
  (or nested combines), since the typed `combine` overloads stop at 5.

### 4.5 Favorites UI
- **Detail screen**: a **"♥ Favorito"** toggle button (filled ♥ when favorite, ♡ when not) near
  Play/Resume/Back that calls `repo.updateFavorite(id, !favorite)` and reflects the new state.
- **Tile** (`GameTile`): a **♥** badge for favorites, placed **bottom-start** of the cover (the ★ is
  top-start, the `👥N` players chip is top-end — bottom-start avoids all overlap).

### 4.6 Strings (EN/ES)
- Section labels reuse existing where possible (`filter_recommended` → "Recommended", system display
  names). New: `section_favorites` ("Favorites"/"Favoritos"), `detail_favorite` ("Favorite"/"Favorito"),
  `sort_label` ("Sort"/"Orden"), `sort_recommended`/`sort_title`/`sort_year`, `home_consoles`
  ("Consoles"/"Consolas") (optional header).

## 5. Architecture / units

- **Pure/testable:** `SortMode` enum; the DAO query behavior (instrumented). No heavy new pure module.
- **Nav:** `GobeNavHost` back stack + `Route.Library` + `LibrarySection`.
- **Screens:** `HomeScreen` (sections), `LibraryScreen` (+`LibraryViewModel`), `DetailScreen`
  (favorite toggle), `GameTile` (♥ badge). `HomeViewModel` slims down.
- **Data:** `GameEntity`/`Game.favorite`, `MIGRATION_4_5`, DAO `searchGames`(+2 args)/`updateFavorite`,
  `LibraryRepository` threading.

## 6. Data flow

1. Home renders sections from `System.entries` + Recommended/Favorites; search box → `Library(SearchAll)`.
2. Entering a section builds the base filter; `LibraryViewModel.combine(genre, sort, …)` →
   `repo.searchGames(...)` → grid.
3. Detail's favorite toggle → `updateFavorite` → Room; the ♥ badge + Favorites section reflect it
   (reactive Flow).
4. A rescan refreshes `recommended` only; `favorite` persists.

## 7. Error handling / edge cases

- Empty section (e.g. no favorites yet) → the existing "no games" empty state, scoped to the section
  ("No favorites yet").
- Old DB → `MIGRATION_4_5` adds `favorite` default 0.
- Back-stack: Back on level-1 Home does nothing special (or exits per current behavior); the emulator
  `returnToHomeOnResume` still forces Home and clears the stack.
- Sort `YEAR` with all-null years → falls back to title order (CASE yields NULL for all → no effect).
- SearchAll with empty query → treat as all games (or guard to require ≥1 char before navigating).

## 8. Testing

- **Unit/instrumented (DAO):** `favoritesOnly` filters; each `sortMode` orders correctly (mode 0
  recommended-first, mode 1 title, mode 2 year-desc with a null-year row sorted last); `updateFavorite`
  persists; **a favorited game stays favorited after `rescan()`** (guards the user-data-vs-backfill
  boundary).
- **Unit (JVM):** `LibrarySection` → base-filter mapping (a small pure mapper is worth extracting and
  testing: section → (system?, recommendedOnly, favoritesOnly, query)).
- **On-device:** Home shows console + Recommended + Favorites sections; entering SNES shows only SNES
  with ★/♥; the sort control cycles and reorders; marking a favorite in Detail shows the ♥ and adds it
  to the Favorites section; Back returns Library→Home.

## 9. Risks

1. **Home rewrite scope** — this restructures Home + adds a navigation level; larger than a chip. Kept
   cohesive by reusing the tile/grid components and the existing `searchGames` (just more args).
2. **Back navigation** — moving from single-`route` to a back stack must not break the emulator-return
   (`returnToHomeOnResume`) or permission flows; covered explicitly in §3/§7.
3. **6-arg `combine`** — needs the vararg form; typed overloads stop at 5 (noted in §4.4).
4. **Scoped genres** — deriving per-section genres adds a little logic; acceptable, keeps chips honest.

## 10. Defaults (change on request)

- Sort default **Recommended-first**; Year = newest-first, unknowns last.
- Sections order: Recommended, Favorites, then `System.entries` order.
- Favorite ♥ bottom-start on the tile; toggle on the detail screen; session-only sort/genre state
  (not persisted); global search on level-1 Home.
