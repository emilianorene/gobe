# Gobe — Home Grid Redesign — RESULTS

> Date: 2026-06-30
> Branch: `feat/home-grid-redesign`
> Plan: `docs/superpowers/plans/2026-06-30-gobe-home-grid-redesign.md`
> Spec: `docs/superpowers/specs/2026-06-30-gobe-home-grid-redesign-design.md`

## Outcome

The Home grid was reworked per the three queued complaints. All acceptance criteria passed
on the ONN.

## What changed

- **Square, captioned tiles (`GameTile`)**: from a fixed 140×190 portrait Card (cover fills
  the card) to a `Column` = a **square cover** (`aspectRatio(1f)`, `ContentScale.Fit`, so nothing
  is cropped — portrait art letterboxes on the sides, wide art top/bottom) + a **caption**
  `Name (Year)` below (e.g. "Dragon Ball Z - Super Butouden 2 (1993)"; name-only when the year
  is unknown). Tile width 132dp. Players badge and branded `DefaultCover` kept, adapted to the
  square.
- **Single scroll (`HomeScreen`)**: the pinned "Continue playing" `LazyRow` + separate grid were
  merged into **one** `LazyVerticalGrid(GridCells.Adaptive(132.dp))`. "Continue playing" is now a
  full-span header item (`item(span = { GridItemSpan(maxLineSpan) })`) that **scrolls with the
  grid**, so it no longer pins/cuts the catalog.
- **Pure `tileCaption(name, year)` helper** (TDD, JVM unit-tested): `"$name ($year)"` when
  `year > 0`, else `name`.

## Commits (feat/home-grid-redesign)

- `97a02d4` tileCaption helper (TDD; 3 unit tests green).
- `da6cd80` square GameTile with Name (Year) caption.
- `1fb19e9` single-scroll grid with Continue playing as a full-span header.

## Acceptance (verified live on the ONN)

- Unit tests pass (`TileCaptionTest` 3/3).
- Tiles are square with a `Name (Year)` caption; covers not cropped; players badge intact;
  `DefaultCover` (e.g. `ffight`) adapts to the square.
- Catalog now shows **6 columns per row** (vs the old ~140dp layout) — more games per screen.
- **"Continue playing" scrolls away**: from the top it's visible; pressing DOWN scrolls it
  off-screen and reveals the full alphabetical catalog. No longer pinned/cutting.
- **Focus (the one real risk) works**: D-pad moves from the Continue-playing header row down
  into the first grid row (confirmed: focus landed on "Air Fortress (1989)").
- Tiles open the detail screen (the `onClick` now lives on the inner cover Card) — confirmed by
  opening a game.
- Long titles wrap to ≤2 lines (e.g. "Aki to Tsukasa no Fushigi no Kabe").

## Notes / follow-ups

- Nested `LazyRow` inside the full-span grid header behaves correctly for D-pad focus on the
  ONN — the spec's fallback (compact pinned row) was not needed.
- Remaining queued backlog item: gamepad navigation shortcuts (L1→Search, R1→Settings) + an
  on-screen button legend (`docs/superpowers/BACKLOG.md`).
