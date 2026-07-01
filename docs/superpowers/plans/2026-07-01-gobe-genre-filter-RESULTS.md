# Gobe — Browse by Genre — RESULTS

> Date: 2026-07-01
> Branch: `feat/genre-filter`
> Plan: `docs/superpowers/plans/2026-07-01-gobe-genre-filter.md`
> Spec: `docs/superpowers/specs/2026-07-01-gobe-genre-filter-design.md`

## Outcome

Home now has a genre filter — a second scrollable chip row below the system chips — that composes
(AND) with the search box and the system filter. All acceptance criteria passed on the ONN.

## What changed

- **DAO (`GameDao`)**: `distinctGenres(): Flow<List<String>>`
  (`SELECT DISTINCT genre … WHERE genre IS NOT NULL AND genre != '' ORDER BY genre COLLATE NOCASE`);
  `searchGames` gained a `genre: String?` predicate (`AND (:genre IS NULL OR genre = :genre)`).
- **Repository**: `genres()` passthrough; `searchGames(query, system, genre: String? = null)` — the
  default keeps the existing call site compiling.
- **`HomeViewModel`**: `selectedGenre` StateFlow + `setGenre()`; `genres` StateFlow from the repo;
  `filtered` now a 3-way `combine(_query, _selectedSystem, _selectedGenre)`.
- **`HomeScreen`**: a `LazyRow` of genre `FilterChip`s (All + one per genre) below the system chips,
  shown only when genres exist; re-tapping the selected genre (or "All") clears it.

## Commits (feat/genre-filter)

- `feat(genre): DAO distinctGenres + genre filter in searchGames` (instrumented tests).
- `feat(genre): HomeViewModel selectedGenre + genres list`.
- `feat(genre): genre chip row on Home (scrollable, composes with system + search)`.

## Acceptance

**Instrumented (Room, on-device):** `GameDaoTest` — 7 tests green (5 existing + 2 new):
`distinctGenresSortedNonEmpty` (nulls/blanks excluded, sorted) and `searchFiltersByGenreAndSystem`
(genre-only, genre+system, and genre=null unchanged).

**Live on the ONN:**
- The genre chip row appears with real, clean genres from the user's library
  (Action, Adventure, Beat'em Up, Board, Card, Casual Game, Compilation, Educational, …).
- Selecting **Adventure** filtered the grid to only Adventure titles (3x3 Eyes, Barbie Super Model,
  Barbie Vacation Adventure, Bonkers, Clock Tower, Emit Vol 1, Famicom Tantei, …); the chip shows
  the selected dot.
- Focus traverses cleanly between the grid and the scrollable genre row (from non-first tiles).
- No regression with no genre selected.

## Notes / follow-ups

- The raw RDB genres looked clean on the user's library — no normalization needed for now (Spec §4).
- **Pre-existing focus quirk (not introduced here):** the very first grid tile routes UP straight to
  the Settings button when there is no Continue-playing row, so from that one tile UP skips the
  filter rows. Reaching the genre/system rows works from any other tile. Revisiting the top-bar/
  filter-row focus routing is a possible future polish, out of scope for this feature.
- Parked, unrelated: **NES/FDS** support is spec+plan-approved on `feat/fds`, waiting on the user's
  `disksys.rom` BIOS.
