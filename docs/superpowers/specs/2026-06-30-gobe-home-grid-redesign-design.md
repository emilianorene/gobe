# Gobe — Home Grid Redesign (square tiles + single scroll + captions)

> Date: 2026-06-30
> Scope: Rework the Home screen grid so games are easier to browse on the ONN.
> Builds on: Fase 0/1/2 + i18n + UI redesign + Arcade (all merged to `main`).
> Origin: queued user feedback (`docs/superpowers/BACKLOG.md`).

## 1. Purpose

Three concrete complaints about the current Home grid:

1. **"Continue playing" dominates.** It is pinned above the grid (a separate, fixed row), so
   it permanently eats ~190dp+ and the main grid below is cut off / you can't see the other
   games.
2. **Tiles are too big and portrait-only.** Fixed 140×190 boxes waste space and suit only
   portrait box art; the library is mixed (SNES/NES portrait, arcade wide/square).
3. **No title on the tile.** You can't tell which game a cover is without opening it.

Goal: more games visible per screen, nothing pinned/cut, and each tile labeled.

## 2. Decisions captured during brainstorming

- **Tile shape: square** (~130dp art area). Best compromise for the mixed library — with
  `ContentScale.Fit`, portrait covers letterbox on the sides, wide covers on top/bottom,
  nothing is cropped. More tiles fit per row than the old portrait box.
- **Continue-playing: scrolls with the grid.** It stops being pinned; it becomes a
  full-width header inside the single scroll, so scrolling down moves it off-screen and
  reveals the whole catalog.
- **Caption: `Name (Year)`** below each cover — e.g. `Super Mario World (1991)`. If the year
  is unknown, show the name alone.

## 3. In scope

- `GameTile`: square cover area + a caption line (`displayName` + ` (year)` when `year != null`),
  truncated to at most 2 lines with an ellipsis. Keep the players badge and the branded
  `DefaultCover`, adapted to the square.
- `HomeScreen`: unify "Continue playing" and the game grid into **one** `LazyVerticalGrid`.
  "Continue playing" becomes a full-span header item (its title + a `LazyRow` of tiles);
  the game tiles follow as normal grid items. Everything scrolls together.
- Preserve the existing focus behavior: the topmost focusable content row routes UP to the
  top-bar Settings button; the first tile requests initial focus.
- i18n: the caption is composed from data (name + year), so no new user-facing strings are
  required. (A `%1$s (%2$d)` format resource may be used for clarity but is optional.)

## 4. Out of scope (deferred / separate backlog items)

- Gamepad navigation shortcuts (L1→Search, R1→Settings) and the on-screen button legend —
  separate queued backlog item, its own spec.
- Genre filter/browse on Home.
- Changing the detail screen, box-art pipeline, or metadata.

## 5. Architecture

- **`GameTile` (rework):** replace the single `Card(140×190)` whose art fills the card with a
  `Column` of fixed width (~130dp):
  - Top: a square (`Modifier.aspectRatio(1f)` or fixed ~130×130) `Card`/`Box` with the cover
    (`SubcomposeAsyncImage`, `ContentScale.Fit`) over the surface; `DefaultCover(game)` on
    error/no-URL; players badge overlaid top-end (unchanged logic).
  - Bottom: a `Text` caption = `game.displayName` + (if `game.year != null`) ` (${game.year})`,
    `maxLines = 2`, `overflow = Ellipsis`, small typography, centered or start-aligned.
  - The caption text should be derived by a small pure helper so it is unit-testable
    (e.g. `fun tileCaption(name: String, year: Int?): String`).
- **`HomeScreen` (rework):** collapse the current `Column { continueColumn; LazyVerticalGrid }`
  into a single `LazyVerticalGrid(columns = GridCells.Adaptive(~130dp))`:
  - When `continuePlaying` is non-empty, emit a leading full-span item
    (`item(span = { GridItemSpan(maxLineSpan) }) { ... }`) containing the "Continue playing"
    title and a `LazyRow` of `GameTile`s.
  - Then emit the game tiles with `items(state.games)`.
  - Focus: apply `Modifier.focusProperties { up = settingsFocus }` to whichever row is
    topmost (the continue header when present, else the first grid content), matching today's
    behavior. `requestInitialFocus` goes to the first continue tile when present, else the
    first grid tile.
- **Nested scroll note:** a horizontal `LazyRow` inside a full-span grid item is the standard
  Compose pattern; the item is width-bounded by the grid, so the `LazyRow` measures fine. TV
  D-pad focus moving between the header row and the first grid row must be verified on-device.

## 6. Testing

- **Unit (JVM):** `tileCaption` returns `"Super Mario World (1991)"` when year present and
  `"Super Mario World"` when year is null.
- **On-device (ONN):** Continue playing no longer pins/cuts the grid (scrolls away when you go
  down); more games per screen; each tile shows the cover + `Name (Year)`; covers are not
  cropped (portrait letterbox sides, wide letterbox top/bottom); D-pad focus navigates from
  the continue row into the grid and back, and UP reaches Settings; SNES and arcade games
  still open from a tile.

## 7. Risks

1. **Nested `LazyRow` focus inside the grid header** on TV — mitigated by on-device focus
   verification; fallback is to keep two scrollers but shrink/compact the continue row.
2. **Caption height** eating vertical space — mitigated by small typography + max 2 lines and
   the smaller square art (net footprint still smaller than today's 140×190).
3. **Adaptive column count** with the new width — pick a min width (~130dp) that yields a
   sensible column count at 1080p; tune on-device.

## 8. Defaults (change on request)

- Square art ~130dp; caption max 2 lines, small type, centered under the cover; Adaptive grid
  min column ~130dp. Continue-playing rendered as a full-span header row that scrolls with the
  grid. All tunable on-device.
