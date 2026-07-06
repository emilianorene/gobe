# Gobe — Richer Metadata (description + IGDB cover fallback)

> Date: 2026-07-05
> Scope: Add a game **description** (IGDB summary, recommended games only) shown on the detail screen,
> and an **IGDB cover** fallback (used when a game has no libretro-thumbnails art). Both baked into
> the bundled metadata index from IGDB at build time. The last v0.3 "Library & content" feature.

## 1. Purpose

Make the detail screen more informative and fill art gaps. Add a **description** for the games that
matter (the curated recommended set) and an **IGDB cover** image so games missing libretro art still
show a real cover instead of the branded placeholder. Both come from IGDB (already wired in `build.py`
for ratings) and ship prebundled — no runtime API on the device; images load from IGDB's public CDN
at runtime like the existing libretro art.

## 2. Decisions captured during brainstorming

- **Fields:** `description` (IGDB `summary`) and `igdbCover` (IGDB cover `image_id`). **No**
  developer/publisher.
- **Coverage:** `description` only for **recommended** games (keeps assets lean, ~+180 KB); `igdbCover`
  for **all IGDB-matched** games (the id is tiny).
- **Art fallback order (deterministic):** libretro (when `boxartName` set) → IGDB cover (when
  `igdbCover` set) → branded placeholder.
- **Runtime images:** the IGDB cover is loaded via Coil from the public CDN
  (`https://images.igdb.com/igdb/image/upload/t_cover_big/<image_id>.jpg`); nothing is bundled.
- **Detail description:** a paragraph under the metadata, capped (~8 lines, ellipsis).

## 3. In scope

### 3.1 Data model
- `GameMeta` / `GameEntity` / `Game`: add `description: String? = null` and `igdbCover: String? = null`.
- `MIGRATION_5_6` (DB version 5 → 6): `ALTER TABLE games ADD COLUMN description TEXT` and
  `ALTER TABLE games ADD COLUMN igdbCover TEXT`; register in `GobeApp.addMigrations(...)`.
- `MetadataIndex.parse`: read `description` and `igdbCover` (optString → null when blank/absent).

### 3.2 Build pipeline (`tools/build-metadata-index/build.py`)
- Extend the existing IGDB games query (which already runs per platform for ratings) to also request
  `summary` and `cover.image_id`:
  `fields name,total_rating,total_rating_count,summary,cover.image_id;` — **one call, no extra
  requests**. Collect per normalized key: `(rating, votes, summary, cover_image_id)`.
- When merging into the per-system `index`:
  - `igdbCover`: set on **every matched key** that has a `cover.image_id`.
  - `description`: set **only on keys in the recommended set** (already computed by
    `select_recommended`) that have a non-empty `summary`.
  - **Coverage caveat:** the existing query is gated by `total_rating != null & total_rating_count >= 5`,
    so `igdbCover` only exists for games IGDB has 5+ rating votes for — NOT every game IGDB knows. That
    systematically excludes obscure titles (which are the ones most likely to lack libretro art). This
    is an accepted YAGNI trade-off (broadening to a rating-less cover query would cost many extra
    requests + size and break the one-call constraint); missing covers just fall through to the
    placeholder. "All matched" above means "all games in our rated-games fetch."
- Requires the IGDB credentials again (`.igdb.env`) to regenerate the assets. (Note: if the secret was
  rotated after the recommended run, get a fresh one before the build step.)

### 3.3 Persistence / backfill (`LibraryRepository.rescan()`)
- `description`/`igdbCover` are index-derived (like players/boxart/genre/year/recommended), so they
  must reach **new AND already-scanned** games. Apply them through the rescan metadata refresh:
  - New games: extend the candidate pass's `updateMeta` (and its `MetaUpdate`) to also write
    `description` + `igdbCover`.
  - Existing games: extend the existing **full-library refresh pass** (currently refreshes
    `recommended`) to also refresh `description` + `igdbCover` from the same `GameMatcher.match(...)`
    result, writing only changed rows and keeping the empty-index guard (a failed/empty index must not
    wipe existing values). A broadened DAO update (e.g. `updateIndexExtras(id, recommended,
    description, igdbCover)`) replaces the single-field `updateRecommended` call in that pass.
  - Generalize the pure `recommendedBackfillUpdates` helper to return the three-field tuple
    `(id, recommended, description, igdbCover)` for rows that differ; on an empty index for a system,
    keep the row's existing `recommended`/`description`/`igdbCover` (no change), same guard as today.
- `toDomain()` maps `description` + `igdbCover`.

### 3.4 Art resolution
- New pure helper `coverUrl(system, boxartName, igdbCover, builder)` (or `coverUrl(game)`), tested:
  1. `boxartName != null` → `builder.url(system, boxartName)` (libretro).
  2. else `igdbCover != null` → `igdbCoverUrl(igdbCover)` =
     `https://images.igdb.com/igdb/image/upload/t_cover_big/<id>.jpg`.
  3. else → `null` (Coil shows the branded placeholder).
  (This drops the old `boxartName ?: displayName` displayName-guess in favor of the IGDB fallback; the
  rare displayName-only hit is negligible and IGDB is a better fallback.)
- `GameTile` and `DetailScreen` (`CoverArt`) call `coverUrl(game)` instead of
  `boxartUrlBuilder.url(game.system, game.boxartName ?: game.displayName)`.
- Add an `igdbCoverUrl(imageId)` builder (in `BoxartUrlBuilder` or a small `IgdbImage` helper).

### 3.5 Detail UI
- When `game.description != null`, render it as a paragraph under the metadata block (before or after
  the size line), `style = bodyMedium`, `maxLines = 8`, `overflow = Ellipsis`, with a top spacer.

## 4. Out of scope (deferred)

- Developer / publisher fields.
- Descriptions for non-recommended games.
- Bundling cover images in the APK (loaded at runtime).
- IGDB genres/franchise; per-game art override; a scrollable/expandable full description.

## 5. Architecture / units

- **Pure/testable:** `coverUrl` resolution; `MetadataIndex.parse` of the two new fields; `igdbCoverUrl`.
- **Build tool:** IGDB query + per-key `summary`/`cover` collection + the merge rules (§3.2).
- **Data:** entity/domain fields + `MIGRATION_5_6`; DAO `updateMeta` (+2 fields) + a broadened
  refresh update; `LibraryRepository` rescan passes + `toDomain`.
- **UI:** `GameTile` + `DetailScreen` cover via `coverUrl`; the description paragraph.

## 6. Data flow

1. **Build time:** `build.py` → IGDB (`summary`, `cover.image_id`) → baked as `description`
   (recommended only) + `igdbCover` (all matched) in `assets/metadata/<tag>.json`.
2. **Scan:** `rescan()` applies both to new + existing games via the meta candidate pass and the
   full-library refresh pass.
3. **Browse:** tiles/detail resolve the cover URL (libretro → IGDB → placeholder); the detail shows
   the description when present.

## 7. Error handling / edge cases

- Missing IGDB creds at build → the whole recommended/description/cover stage is skipped (existing
  graceful path); assets build without the fields, UI simply shows no descriptions and no IGDB covers.
- A game with no libretro art and no IGDB cover → branded placeholder (unchanged).
- Empty/failed index at scan → the refresh pass keeps existing `description`/`igdbCover` (empty-index
  guard), never wiping them on a transient failure.
- IGDB cover URL 404 at runtime → Coil falls through to the placeholder (same as a libretro miss).
- Old DB → `MIGRATION_5_6` adds the two nullable columns.

## 8. Testing

- **Unit (JVM):** `coverUrl` returns the libretro URL when `boxartName` set, the IGDB URL when only
  `igdbCover` set, and `null` when neither; `igdbCoverUrl` formats the CDN URL; `MetadataIndex.parse`
  reads `description`/`igdbCover` (present → value, absent → null).
- **Instrumented (DAO):** `updateMeta`/the broadened refresh write `description` + `igdbCover`; a
  re-scan backfills them onto a pre-existing row (guards the index-field backfill boundary).
- **On-device:** a recommended game shows its description; a game lacking libretro art but present in
  IGDB shows the IGDB cover (tile + detail); a game with neither shows the placeholder.

## 9. Risks

1. **IGDB↔ROM name matching** — same partial-coverage caveat as ratings (spec carried over); descriptions
   /covers appear only where the normalized name matches. Acceptable; missing values are harmless.
2. **Asset size** — descriptions add ~180 KB (recommended-only cap keeps it bounded); covers add only
   the small id strings. Verified against the current ~830 KB after the build.
3. **Backfill correctness** — extending the refresh pass to three fields must keep the empty-index
   guard and write-on-change semantics so it doesn't churn or wipe data. Covered by the DAO/backfill
   test.

## 10. Defaults (change on request)

- Description: recommended-only, detail paragraph, maxLines 8, ellipsis.
- Cover fallback order: libretro → IGDB (`t_cover_big`) → placeholder.
- Two nullable columns; index-derived; applied via the rescan refresh pass.
