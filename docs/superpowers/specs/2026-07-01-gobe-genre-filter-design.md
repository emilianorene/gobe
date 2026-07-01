# Gobe — Browse by Genre on Home

> Date: 2026-07-01
> Scope: Add a genre filter to the Home screen, as a second chip row parallel to the system filter.
> Builds on: everything merged to `main` (Arcade, Home grid redesign, Gamepad-nav).
> Origin: queued idea — genre data is already in Room + the metadata index but unused in the UI.

## 1. Purpose

Let the user narrow the library by **genre** (Action, Platform, Fighting, RPG, …) from Home. The
genre is already stored per game (`genre: String?` in Room, populated from the libretro RDB during
the metadata pass) but nothing in the UI uses it. This adds a genre filter that composes with the
existing search box and system filter.

## 2. Decisions captured during brainstorming

- **Presentation: a second horizontal, scrollable chip row** below the system chips — same look as
  the system `FilterChip`s. "All" + one chip per genre present in the library (sorted).
- **Filtering is AND** across the three dimensions: search query AND system AND genre.
- **No separate browse screen / dropdown** (YAGNI); genres come straight from the DB.

## 3. In scope

- `GameDao.distinctGenres()`: `Flow<List<String>>` of the distinct non-empty genres present, sorted.
- Extend `GameDao.searchGames` (and `LibraryRepository.searchGames`) to also filter by an optional
  `genre` (exact match) when one is selected.
- `HomeViewModel`: add `_selectedGenre: String?` + `setGenre(...)`, expose the genre list, and fold
  genre into the combined `filtered` flow.
- `HomeScreen`: render a second `LazyRow` of genre `FilterChip`s below the system chips (only when
  the genre list is non-empty); selecting a chip sets the genre (tapping the selected one / "All"
  clears it).

## 4. Out of scope (deferred)

- A dedicated genre-browse screen or grouping the grid by genre.
- Genre normalization/merging (RDB genres are used as-is; e.g. "Action" vs "Action, Platform" stay
  distinct). If the raw genres look messy on-device, cleanup is a follow-up.
- Multi-select genres (single selected genre at a time, like the system filter).
- Changing the metadata pipeline, box art, detail, or emulation.

## 5. Architecture

- **DAO (`GameDao`)**:
  - `@Query("SELECT DISTINCT genre FROM games WHERE genre IS NOT NULL AND genre != '' ORDER BY genre")
    fun distinctGenres(): Flow<List<String>>`.
  - `searchGames` gains a `genre: String?` param; the SQL adds `AND (:genre IS NULL OR genre = :genre)`
    to the existing name/system predicate.
- **Repository (`LibraryRepository`)**:
  - `fun genres(): Flow<List<String>> = gameDao.distinctGenres()`.
  - `searchGames(query, system, genre)` forwards the new param.
- **ViewModel (`HomeViewModel`)**:
  - `_selectedGenre: StateFlow<String?>` + `setGenre(g: String?)`.
  - `genres: StateFlow<List<String>>` from `repo.genres()`.
  - `filtered` combines `_query`, `_selectedSystem`, `_selectedGenre` → `repo.searchGames(q, s, g)`.
    (Compose the three via `combine`; if `combine`'s 3-arg tuple is awkward, wrap the three in a
    small data holder before `flatMapLatest`.)
- **UI (`HomeScreen`)**: below the existing system chip `Row`, add a genre chip row — a `LazyRow`
  (scrollable, genres can be many) of `FilterChip`s: an "All" chip (`selectedGenre == null`) plus one
  per genre, reusing the existing `FilterChip` composable. Rendered only when `genres` is non-empty.
  Keep the row compact (one line) so it doesn't reintroduce vertical clutter.

## 6. Testing

- **Instrumented (Room, on-device — matches the existing DAO test pattern)**:
  - `distinctGenres` returns the sorted distinct non-empty genres for a seeded set (nulls/blanks
    excluded).
  - `searchGames(q="", system=null, genre="Action")` returns only Action games; combining
    `system=SNES, genre="Action"` returns only SNES+Action; `genre=null` returns everything (current
    behavior unchanged).
- **On-device**: the genre row appears with real genres; selecting a genre filters the grid;
  it composes with the system filter and the search box; "All"/re-tap clears it.

## 7. Risks

1. **Messy raw genres** (compound/verbose RDB strings) → the chip row may show odd labels. Mitigated
   by using them as-is now; normalization is a documented follow-up (§4).
2. **Long genre list** → handled by a scrollable `LazyRow`.
3. **Room migration** — none needed; `genre` already exists as a column. Adding a query + a param is
   backward-compatible.
4. **Filter interaction** — genre must AND with search + system; covered by the instrumented test.

## 8. Defaults (change on request)

- Genre chips: scrollable `LazyRow` under the system chips, "All" default, single-select, exact
  match, raw RDB genre strings. Hidden when there are no genres. All tunable on-device.
