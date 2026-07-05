# Gobe — Richer Metadata (description + IGDB cover) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a game `description` (IGDB summary, recommended games only) on the detail screen and an `igdbCover` fallback (used when a game has no libretro art), both baked into the metadata index from IGDB.

**Architecture:** Two new index-derived fields (`description`, `igdbCover`) flow `GameMeta → GameEntity/Game` and are applied at scan time (new games via the `updateMeta` candidate pass; existing games via a generalized full-library refresh pass, keeping the empty-index guard). A pure `coverUrl` resolver picks libretro → IGDB → placeholder. `build.py` fetches `summary`+`cover.image_id` in the existing IGDB call.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Room (real migrations), Coil, JUnit4, Python 3 (build tool), IGDB.

**Spec:** `docs/superpowers/specs/2026-07-05-gobe-rich-metadata-design.md`

**Build/test env:**
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- Unit: `./gradlew :app:testDebugUnitTest`. Compile: `./gradlew :app:assembleDebug` + `:app:compileDebugAndroidTestKotlin`.
- Gradle is slow (~3–10 min); run in background and wait.
- On-device: **ask the user before installing.**

---

## File Structure

**Modify:**
- `data/metadata/MetadataIndex.kt` — `GameMeta.description`/`igdbCover` + parse.
- `data/db/GameEntity.kt`, `domain/Game.kt` — the two fields.
- `data/db/GobeDatabase.kt` (v5→6 + `MIGRATION_5_6`), `GobeApp.kt` (register).
- `data/db/GameDao.kt` — `updateMeta` (+2 args) + `updateIndexExtras`.
- `data/LibraryRepository.kt` — `MetaUpdate` (+2), candidate pass, refresh pass, `toDomain`.
- `data/RecommendedBackfill.kt` — generalize to `IndexExtras` + `indexExtrasBackfillUpdates`.
- `data/art/GameTile.kt`? no — `ui/home/GameTile.kt`, `ui/detail/DetailScreen.kt` — cover via `coverUrl` + description.
- `androidTest/.../GameDaoTest.kt` — `updateMeta` call gains 2 args + new test.
- `tools/build-metadata-index/build.py` — IGDB `summary`+`cover` + merge.

**Create:**
- `data/art/CoverUrl.kt` — `igdbCoverUrl` + `coverUrl`.
- Test `test/.../data/art/CoverUrlTest.kt`.
- (Rewrite) `test/.../data/RecommendedBackfillTest.kt` → tests `indexExtrasBackfillUpdates`.

---

## Task 1: `GameMeta` description + igdbCover (parse)

**Files:** `data/metadata/MetadataIndex.kt`, test `test/.../data/metadata/MetadataIndexTest.kt`.

- [ ] **Step 1: Extend the test** — replace `MetadataIndexTest`'s json + add a case:
```kotlin
    private val json = """{"supermarioworld":{"boxart":"Super Mario World","players":2,"recommended":true,"description":"A platformer.","igdbCover":"co123"},
                          "contra":{"boxart":"Contra","players":2,"igdbCover":"co456"}}"""
    @Test fun readsRichMetadata() {
        val idx = MetadataIndex.parse(json)
        assertEquals("A platformer.", idx["supermarioworld"]?.description)
        assertEquals("co123", idx["supermarioworld"]?.igdbCover)
        assertNull(idx["contra"]?.description)          // non-recommended: no description
        assertEquals("co456", idx["contra"]?.igdbCover)
    }
```
(Keep the existing `looksUpNormalized`/`readsRecommendedFlag` tests; add `import org.junit.Assert.assertNull` if missing.)
- [ ] **Step 2: Run to verify fail** — `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.data.metadata.MetadataIndexTest"` → FAIL.
- [ ] **Step 3: Implement** — `GameMeta` add fields; `parse` read them:
```kotlin
data class GameMeta(
    val boxart: String?,
    val players: Int?,
    val genre: String? = null,
    val year: Int? = null,
    val recommended: Boolean = false,
    val description: String? = null,
    val igdbCover: String? = null,
)
```
in `parse`, after `recommended = ...`:
```kotlin
                    recommended = o.optBoolean("recommended", false),
                    description = o.optString("description").ifBlank { null },
                    igdbCover = o.optString("igdbCover").ifBlank { null },
```
- [ ] **Step 4: Run to verify pass** — same command → PASS.
- [ ] **Step 5: Commit** `git add data/metadata/MetadataIndex.kt test/.../MetadataIndexTest.kt && git commit -m "feat(metadata): description + igdbCover fields + parse"`

---

## Task 2: Data model — columns + migration

**Files:** `data/db/GameEntity.kt`, `domain/Game.kt`, `data/db/GobeDatabase.kt`, `GobeApp.kt`.

- [ ] **Step 1:** `GameEntity.kt` — add after `favorite`:
```kotlin
    val description: String? = null,
    val igdbCover: String? = null,
```
- [ ] **Step 2:** `domain/Game.kt` — add after `favorite`:
```kotlin
    val description: String? = null,
    val igdbCover: String? = null,
```
- [ ] **Step 3:** `GobeDatabase.kt` — `version = 5` → `6`; add:
```kotlin
        val MIGRATION_5_6 = androidx.room.migration.Migration(5, 6) { db ->
            db.execSQL("ALTER TABLE games ADD COLUMN description TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN igdbCover TEXT")
        }
```
- [ ] **Step 4:** `GobeApp.kt` — extend `addMigrations(...)`:
```kotlin
            .addMigrations(GobeDatabase.MIGRATION_1_2, GobeDatabase.MIGRATION_2_3, GobeDatabase.MIGRATION_3_4, GobeDatabase.MIGRATION_4_5, GobeDatabase.MIGRATION_5_6)
```
- [ ] **Step 5:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6:** Commit `git commit -m "feat(db): description + igdbCover columns + MIGRATION_5_6"`

---

## Task 3: `coverUrl` + `igdbCoverUrl` (pure, TDD)

**Files:** Create `data/art/CoverUrl.kt`, test `test/.../data/art/CoverUrlTest.kt`.

- [ ] **Step 1: Write the failing test:**
```kotlin
package com.gobe.tv.data.art

import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverUrlTest {
    private fun game(boxart: String?, cover: String?) = Game(
        id = 1, path = "/x", system = System.SNES, displayName = "X",
        fileName = "x", sizeBytes = 0, dateAdded = 0, boxartName = boxart, igdbCover = cover,
    )
    @Test fun prefersLibretroWhenBoxartPresent() {
        assertTrue(coverUrl(game("Super Mario World", "co123"))!!.contains("thumbnails.libretro.com"))
    }
    @Test fun fallsBackToIgdbWhenNoBoxart() {
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/co123.jpg", coverUrl(game(null, "co123")))
    }
    @Test fun nullWhenNeither() { assertNull(coverUrl(game(null, null))) }
    @Test fun igdbCoverUrlFormats() {
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/abc.jpg", igdbCoverUrl("abc"))
    }
    @Test fun igdbCoverUrlNullOrBlank() { assertNull(igdbCoverUrl(null)); assertNull(igdbCoverUrl("")) }
}
```
- [ ] **Step 2: Run to verify fail** — `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.data.art.CoverUrlTest"` → FAIL.
- [ ] **Step 3: Implement `CoverUrl.kt`:**
```kotlin
package com.gobe.tv.data.art

import com.gobe.tv.domain.Game

/** IGDB CDN cover URL for an image id (t_cover_big), or null when absent. Pure. */
fun igdbCoverUrl(imageId: String?): String? =
    if (imageId.isNullOrBlank()) null
    else "https://images.igdb.com/igdb/image/upload/t_cover_big/$imageId.jpg"

/**
 * Resolve a game's cover URL with a deterministic fallback: libretro thumbnail (when the game matched
 * a boxart name) → IGDB cover (when present) → null (Coil then shows the branded placeholder). Pure.
 */
fun coverUrl(game: Game, builder: BoxartUrlBuilder = BoxartUrlBuilder()): String? =
    builder.url(game.system, game.boxartName) ?: igdbCoverUrl(game.igdbCover)
```
- [ ] **Step 4: Run to verify pass** — same → PASS (5 tests).
- [ ] **Step 5: Commit** `git commit -m "feat(art): coverUrl resolver (libretro -> IGDB -> placeholder)"`

---

## Task 4: DAO — updateMeta (+2) + updateIndexExtras

**Files:** `data/db/GameDao.kt`, `androidTest/.../GameDaoTest.kt`.

- [ ] **Step 1: Change `updateMeta` + add `updateIndexExtras`:**
```kotlin
    @Query("UPDATE games SET players = :players, boxartName = :boxartName, genre = :genre, year = :year, description = :description, igdbCover = :igdbCover WHERE id = :id")
    suspend fun updateMeta(id: Long, players: Int?, boxartName: String?, genre: String?, year: Int?, description: String?, igdbCover: String?)

    @Query("UPDATE games SET recommended = :recommended, description = :description, igdbCover = :igdbCover WHERE id = :id")
    suspend fun updateIndexExtras(id: Long, recommended: Boolean, description: String?, igdbCover: String?)
```
(Leave `updateRecommended` — still used by existing tests.)
- [ ] **Step 2: Fix the existing `updateMeta` call in `GameDaoTest.kt`** (`searchAndUpdateMeta`, ~line 72): add the 2 new args:
```kotlin
        dao.updateMeta(g.id, players = 2, boxartName = "Super Mario World", genre = "Platform", year = 1991, description = null, igdbCover = null)
```
- [ ] **Step 3: Add an instrumented test** to `GameDaoTest.kt`:
```kotlin
    @Test fun updateIndexExtrasWrites() = runBlocking {
        dao.insertAll(listOf(GameEntity(path = "/a", system = System.SNES, displayName = "Alpha", fileName = "a", sizeBytes = 1, dateAdded = 1L)))
        val a = dao.getAll().first()
        dao.updateIndexExtras(a.id, recommended = true, description = "A great game.", igdbCover = "co999")
        val row = dao.getById(a.id)!!
        assertEquals(true, row.recommended)
        assertEquals("A great game.", row.description)
        assertEquals("co999", row.igdbCover)
    }
```
- [ ] **Step 4:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL. (androidTest runs on device later.)
- [ ] **Step 5:** Commit `git commit -m "feat(db): updateMeta carries description+igdbCover; add updateIndexExtras"`

---

## Task 5: LibraryRepository — generalize backfill + threading

**Files:** `data/RecommendedBackfill.kt`, test `test/.../data/RecommendedBackfillTest.kt`, `data/LibraryRepository.kt`.

- [ ] **Step 1: Generalize the pure helper** — replace `RecommendedBackfill.kt` contents:
```kotlin
package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity

/** The index-derived fields refreshed across the whole library on rescan. */
data class IndexExtras(val recommended: Boolean, val description: String?, val igdbCover: String?)

/**
 * Given the current games and a "what should this game's index-derived fields be?" function, return
 * the (id -> IndexExtras) changes needed — only for rows where any of the three differ. Pure/testable.
 * Used by [LibraryRepository.rescan] to backfill recommended/description/igdbCover across the whole
 * library (including already-scanned games), writing only where something changed.
 */
fun indexExtrasBackfillUpdates(
    games: List<GameEntity>,
    desired: (GameEntity) -> IndexExtras,
): List<Pair<Long, IndexExtras>> =
    games.mapNotNull { g ->
        val want = desired(g)
        if (g.recommended != want.recommended || g.description != want.description || g.igdbCover != want.igdbCover)
            g.id to want else null
    }
```
- [ ] **Step 2: Rewrite the test** — replace `RecommendedBackfillTest.kt`:
```kotlin
package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendedBackfillTest {
    private fun g(id: Long, name: String, rec: Boolean, desc: String?, cover: String?) = GameEntity(
        id = id, path = "/$name", system = System.SNES, displayName = name,
        fileName = "$name.sfc", sizeBytes = 0, dateAdded = 0, recommended = rec, description = desc, igdbCover = cover,
    )
    @Test fun returnsOnlyChangedRows() {
        val games = listOf(
            g(1, "a", false, null, null),                 // will change (recommended -> true)
            g(2, "b", true, "d", "c"),                    // unchanged
        )
        val desired = mapOf(
            "a" to IndexExtras(true, null, null),
            "b" to IndexExtras(true, "d", "c"),
        )
        val out = indexExtrasBackfillUpdates(games) { desired[it.displayName]!! }
        assertEquals(listOf(1L to IndexExtras(true, null, null)), out)
    }
    @Test fun backfillsDescriptionAndCoverOnExistingRow() {
        val games = listOf(g(1, "Super Metroid", true, null, null))
        val out = indexExtrasBackfillUpdates(games) { IndexExtras(true, "Explore Zebes.", "co1") }
        assertEquals(listOf(1L to IndexExtras(true, "Explore Zebes.", "co1")), out)
    }
}
```
- [ ] **Step 3: Update `rescan()` in `LibraryRepository.kt`:**
  - Extend the candidate pass's `MetaUpdate` + `updateMeta` call with description/igdbCover:
```kotlin
                MetaUpdate(e.id, meta.players, meta.boxart, meta.genre, meta.year, meta.description, meta.igdbCover)
```
```kotlin
                    updates.forEach { u -> gameDao.updateMeta(u.id, u.players, u.boxart, u.genre, u.year, u.description, u.igdbCover) }
```
  - Extend the private `MetaUpdate` data class with `val description: String?, val igdbCover: String?`.
  - Replace the recommended refresh pass with the generalized one:
```kotlin
        // Refresh index-derived fields (recommended/description/igdbCover) across ALL games — backfills
        // already-scanned libraries; the candidate filter above only touches never-matched rows.
        if (m != null && provider != null) {
            val updates = indexExtrasBackfillUpdates(gameDao.getAll()) { e ->
                val idx = provider(e.system)
                // Empty index = the bundled asset failed to load; keep existing values (no wipe).
                if (idx.isEmpty()) IndexExtras(e.recommended, e.description, e.igdbCover)
                else {
                    val meta = m.match(e.displayName, idx)
                    IndexExtras(meta?.recommended ?: false, meta?.description, meta?.igdbCover)
                }
            }
            if (updates.isNotEmpty()) {
                runInTransaction {
                    updates.forEach { (id, x) -> gameDao.updateIndexExtras(id, x.recommended, x.description, x.igdbCover) }
                }
            }
        }
```
  - `toDomain()` add `description = description, igdbCover = igdbCover`.
- [ ] **Step 4:** `./gradlew :app:testDebugUnitTest` (RecommendedBackfillTest) + `:app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `git commit -m "feat(library): backfill description+igdbCover via generalized index refresh"`

---

## Task 6: UI — cover via `coverUrl` + detail description

**Files:** `ui/home/GameTile.kt`, `ui/detail/DetailScreen.kt`.

- [ ] **Step 1: GameTile** — replace the URL line (`GameTile.kt:65`):
```kotlin
    val url = coverUrl(game)
```
Add `import com.gobe.tv.data.art.coverUrl`. Remove the now-unused `private val boxartUrlBuilder = BoxartUrlBuilder()` + its import if unused.
- [ ] **Step 2: DetailScreen `CoverArt`** — replace its URL line:
```kotlin
    val url = com.gobe.tv.data.art.coverUrl(game)
```
(remove the local `BoxartUrlBuilder().url(...)`.)
- [ ] **Step 3: DetailScreen description paragraph** — after the size-line `Text(...)` (the `detail_size_kb` block) add:
```kotlin
                    if (!g.description.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            g.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 8,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
```
- [ ] **Step 4:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `git commit -m "feat(ui): IGDB cover fallback + detail description"`

---

## Task 7: build.py — IGDB summary + cover + merge

**Files:** `tools/build-metadata-index/build.py`.

- [ ] **Step 1: Refactor `igdb_recommended` → `igdb_data`** returning `(recommended, extras)`. Replace the function:
```python
def igdb_data(tag: str, client_id: str, token: str):
    """Return (recommended_keys: set, extras: dict[key -> {"cover": id|None, "summary": str|None}])."""
    platform = IGDB_PLATFORM.get(tag)
    if platform is None:
        return set(), {}
    best: dict[str, tuple[float, int]] = {}
    extras: dict[str, dict] = {}
    offset = 0
    while True:
        q = (f"fields name,total_rating,total_rating_count,summary,cover.image_id; "
             f"where platforms = ({platform}) & total_rating != null & total_rating_count >= 5; "
             f"sort total_rating desc; limit 500; offset {offset};")
        req = urllib.request.Request(
            IGDB_GAMES_URL, data=q.encode(),
            headers={"Client-ID": client_id, "Authorization": f"Bearer {token}",
                     "User-Agent": "gobe-metadata-builder"}, method="POST",
        )
        try:
            rows = json.loads(get_with(req))
        except Exception as e:
            print(f"  !! IGDB {tag} failed: {e}", file=sys.stderr)
            return set(), {}
        if not rows:
            break
        for row in rows:
            key = normalize(row.get("name", ""))
            if not key:
                continue
            rating = row.get("total_rating")
            votes = row.get("total_rating_count", 0)
            prev = best.get(key)
            if prev is None or (rating or 0) > prev[0]:
                best[key] = (rating or 0, votes or 0)
                extras[key] = {
                    "cover": (row.get("cover") or {}).get("image_id"),
                    "summary": row.get("summary"),
                }
        offset += 500
        if len(rows) < 500:
            break
    recommended = select_recommended((k, r, v) for k, (r, v) in best.items())
    print(f"  {tag}: IGDB recommended = {len(recommended)}, covers = {sum(1 for e in extras.values() if e['cover'])}", flush=True)
    return recommended, extras
```
- [ ] **Step 2: Update the merge in `main()`** — replace the `for key in igdb_recommended(...)` block:
```python
        if igdb_tok:
            recommended, extras = igdb_data(tag, client_id, igdb_tok)
            for key in recommended:
                index.setdefault(key, {})["recommended"] = True
            for key, ex in extras.items():
                if ex.get("cover"):
                    index.setdefault(key, {})["igdbCover"] = ex["cover"]
                if key in recommended and ex.get("summary"):
                    index.setdefault(key, {})["description"] = ex["summary"]
```
- [ ] **Step 3:** Self-test still green (policy unchanged): `python3 tools/build-metadata-index/build.py --self-test` → `select_recommended self-test OK`.
- [ ] **Step 4:** Update the module docstring's RECOMMENDED note to mention `summary`/`cover.image_id` → `description`/`igdbCover`.
- [ ] **Step 5:** Commit `git commit -m "feat(tools): IGDB summary + cover baked as description + igdbCover"`

---

## Task 8: Regenerate the dataset (needs IGDB creds)

- [ ] **Step 1:** Confirm `tools/build-metadata-index/.igdb.env` has valid `IGDB_CLIENT_ID`/`IGDB_SECRET`. **If the user rotated the secret after the recommended run, ask them for a fresh one before running.**
- [ ] **Step 2:** Run (venv from before has msgpack):
```bash
cd tools/build-metadata-index && set -a && source .igdb.env && set +a && ./venv/bin/python build.py
```
Expected: per-console lines print recommended + cover counts; `assets/metadata/*.json` rewritten.
- [ ] **Step 3: Sanity-check** the regenerated assets: boxart/players counts unchanged vs `git show HEAD:` (no regression), `description` present on some recommended entries, `igdbCover` present on many; print resulting sizes.
- [ ] **Step 4:** Commit `git add app/src/main/assets/metadata/ && git commit -m "data(metadata): add IGDB descriptions + covers"`

---

## Task 9: Full sweep

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (MetadataIndex, CoverUrl, RecommendedBackfill).
- [ ] **Step 2:** `./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3:** `python3 tools/build-metadata-index/build.py --self-test` → OK.
- [ ] **Step 4: On-device (ONLY after asking the user):** a recommended game shows its description; a game lacking libretro art but with an IGDB cover shows the IGDB cover (tile + detail); the instrumented `GameDaoTest` passes.

---

## Notes for the implementer
- **Ask before any `adb install`/on-device step** — the user actively uses the ONN.
- Do NOT bump the app version / CHANGELOG here — that's the v0.3.0-beta release packaging, done later with the other v0.3 features.
- Follow existing patterns: `optString(...).ifBlank { null }`, TV `Text`, the `MetaUpdate`/refresh-pass idiom.
