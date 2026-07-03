# Gobe — Recommended / Essential Game Badge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tag each game with a boolean `recommended` flag (a curated best-of set per console, baked into the bundled metadata index from IGDB ratings at build time), show a badge on the tile + detail, and add a "Recommended only" Home filter + recommended-first sort.

**Architecture:** Extend the existing prebundled metadata pipeline. `build.py` computes the recommended set per console (pure `select_recommended`) and bakes `"recommended": true` into `assets/metadata/<tag>.json`. The app carries `recommended` through `GameMeta → GameEntity/Game`, tags every scanned game (with a **backfill pass** so already-scanned libraries also get flagged), and surfaces it in a badge + filter chip + sort. Any user's library is tagged automatically via the existing name-normalized matching.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Room (real migrations), JUnit4 (JVM), Python 3 (build tool), IGDB API (maintainer-side only).

**Spec:** `docs/superpowers/specs/2026-07-02-gobe-recommended-games-design.md`

**Build/test environment:**
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- Unit tests: `./gradlew :app:testDebugUnitTest` (JVM, no device)
- Compile: `./gradlew :app:assembleDebug`
- On-device (ONN): **ask the user before installing** — they use the device. adb at `192.168.1.219:5555`.
- Python tool self-test: `python3 tools/build-metadata-index/build.py --self-test` (no network).

---

## File Structure

**Modify (app):**
- `app/src/main/java/com/gobe/tv/data/metadata/MetadataIndex.kt` — `GameMeta.recommended` + parse.
- `app/src/main/java/com/gobe/tv/domain/Game.kt` — `recommended` field.
- `app/src/main/java/com/gobe/tv/data/db/GameEntity.kt` — `recommended` column.
- `app/src/main/java/com/gobe/tv/data/db/GobeDatabase.kt` — version 4 + `MIGRATION_3_4`.
- `app/src/main/java/com/gobe/tv/GobeApp.kt` — register `MIGRATION_3_4`.
- `app/src/main/java/com/gobe/tv/data/db/GameDao.kt` — `updateRecommended` + `searchGames(recommendedOnly)`.
- `app/src/main/java/com/gobe/tv/data/LibraryRepository.kt` — backfill pass + `searchGames` arg + `toDomain`.
- `app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt` — `recommendedOnly` flow + `Filters` holder.
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — "Recommended" filter chip.
- `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt` — badge.
- `app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt` — recommended line.
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml` — strings.
- `tools/build-metadata-index/build.py` — IGDB stage + `select_recommended` + merge.

**Create (app):**
- `app/src/main/java/com/gobe/tv/data/RecommendedBackfill.kt` — pure helper.
- Tests: `app/src/test/java/com/gobe/tv/data/RecommendedBackfillTest.kt`; extend `MetadataIndexTest.kt`.

**Note on `updateMeta`:** we deliberately do NOT add `recommended` to `updateMeta`/`MetaUpdate`. The
backfill pass (Task 5) refreshes `recommended` for **all** games each rescan, which uniformly covers
both brand-new and already-scanned games — so touching `updateMeta` would be redundant. This is a
small, intentional simplification of the spec's §3.3 (which allowed either); the spec's core
requirement (existing libraries get backfilled) is fully met by the pass.

**Testing reality:** Room DAO query behavior (the `recommendedOnly` filter + sort) lives in an
**instrumented** test (`androidTest`) that needs a device/emulator — defer running it to the on-device
pass. The JVM-testable gates (index parse, backfill helper) run without a device and are the primary
TDD coverage. `assertEquals(Float,Float)` is not used here.

---

## Task 1: `GameMeta.recommended` + JSON parse

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/data/metadata/MetadataIndex.kt`
- Test: `app/src/test/java/com/gobe/tv/data/metadata/MetadataIndexTest.kt`

- [ ] **Step 1: Extend the failing test**

Replace the body of `MetadataIndexTest` with:

```kotlin
package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class MetadataIndexTest {
    private val json = """{"supermarioworld":{"boxart":"Super Mario World","players":2,"recommended":true},
                          "contra":{"boxart":"Contra","players":2}}"""
    @Test fun looksUpNormalized() {
        val idx = MetadataIndex.parse(json)
        assertEquals("Super Mario World", idx["supermarioworld"]?.boxart)
        assertEquals(2, idx["contra"]?.players)
        assertNull(idx["unknown"])
    }
    @Test fun readsRecommendedFlag() {
        val idx = MetadataIndex.parse(json)
        assertTrue(idx["supermarioworld"]?.recommended == true)   // present -> true
        assertFalse(idx["contra"]?.recommended == true)           // absent  -> false
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.data.metadata.MetadataIndexTest"`
Expected: FAIL — `recommended` is not a member of `GameMeta`.

- [ ] **Step 3: Implement**

In `MetadataIndex.kt`, add the field + parse it:

```kotlin
data class GameMeta(
    val boxart: String?,
    val players: Int?,
    val genre: String? = null,
    val year: Int? = null,
    val recommended: Boolean = false,
)
```

and inside `parse`, in the `GameMeta(...)` construction, add **only** the `recommended =` line. The
`year = ...` line below already exists (`MetadataIndex.kt:19`) and is shown only as the anchor —
insert the new line right after it, do NOT duplicate `year`:

```kotlin
                    year = if (o.has("year")) o.optInt("year") else null,   // existing anchor line
                    recommended = o.optBoolean("recommended", false),        // <-- add only this
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.data.metadata.MetadataIndexTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/metadata/MetadataIndex.kt app/src/test/java/com/gobe/tv/data/metadata/MetadataIndexTest.kt
git commit -m "feat(metadata): recommended flag in GameMeta + JSON parse"
```

---

## Task 2: Pure `recommendedBackfillUpdates` helper

Guards the reviewer's blocker (existing libraries must get backfilled) in a JVM-testable unit.

**Files:**
- Create: `app/src/main/java/com/gobe/tv/data/RecommendedBackfill.kt`
- Test: `app/src/test/java/com/gobe/tv/data/RecommendedBackfillTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendedBackfillTest {
    private fun g(id: Long, name: String, recommended: Boolean) = GameEntity(
        id = id, path = "/$name", system = System.SNES, displayName = name,
        fileName = "$name.sfc", sizeBytes = 0, dateAdded = 0, recommended = recommended,
    )

    @Test fun returnsOnlyChangedRows() {
        val games = listOf(g(1, "a", false), g(2, "b", true), g(3, "c", false))
        val desired = mapOf("a" to true, "b" to true, "c" to false)
        val out = recommendedBackfillUpdates(games) { desired[it.displayName] ?: false }
        assertEquals(listOf(1L to true), out) // only 'a' flips; b already true, c already false
    }

    @Test fun backfillsExistingMatchedRow() { // the blocker: already-scanned row, flag currently false
        val games = listOf(g(1, "Super Metroid", false))
        val out = recommendedBackfillUpdates(games) { it.displayName == "Super Metroid" }
        assertEquals(listOf(1L to true), out)
    }

    @Test fun clearsWhenNoLongerRecommended() {
        val games = listOf(g(1, "x", true))
        val out = recommendedBackfillUpdates(games) { false }
        assertEquals(listOf(1L to false), out)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.data.RecommendedBackfillTest"`
Expected: FAIL — unresolved `recommendedBackfillUpdates` (and `GameEntity.recommended` — added in Task 3; if Task 3 not done yet, this also won't compile. Do Task 3's entity change first if needed, or accept the fail is a compile error).

> Ordering note: `GameEntity.recommended` is added in Task 3. If you're doing tasks strictly in order,
> add the `recommended` field to `GameEntity` now as part of this task's Step 3 (it's a one-liner) so
> this test compiles; Task 3 then only adds the migration + Game domain. Either way the field must
> exist for this test.

- [ ] **Step 3: Implement**

Create `RecommendedBackfill.kt`:

```kotlin
package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity

/**
 * Given the current games and a "should this game be recommended?" predicate, return the
 * (id -> recommended) changes needed — only for rows whose stored flag differs from the desired
 * value. Pure/testable. Used by [LibraryRepository.rescan] to backfill the flag across the whole
 * library (including already-scanned games) with DB writes only where something changed.
 */
fun recommendedBackfillUpdates(
    games: List<GameEntity>,
    desiredRecommended: (GameEntity) -> Boolean,
): List<Pair<Long, Boolean>> =
    games.mapNotNull { g ->
        val want = desiredRecommended(g)
        if (g.recommended != want) g.id to want else null
    }
```

If not already present (see ordering note), add to `GameEntity`:
```kotlin
    val recommended: Boolean = false,
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.data.RecommendedBackfillTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/RecommendedBackfill.kt app/src/test/java/com/gobe/tv/data/RecommendedBackfillTest.kt app/src/main/java/com/gobe/tv/data/db/GameEntity.kt
git commit -m "feat(library): pure recommendedBackfillUpdates helper"
```

---

## Task 3: Persistence — entity, domain, migration

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/data/db/GameEntity.kt` (if not already done in Task 2)
- Modify: `app/src/main/java/com/gobe/tv/domain/Game.kt`
- Modify: `app/src/main/java/com/gobe/tv/data/db/GobeDatabase.kt`
- Modify: `app/src/main/java/com/gobe/tv/GobeApp.kt`

- [ ] **Step 1: Add the column + domain field**

`GameEntity.kt` — add (after `year`), if not present:
```kotlin
    val recommended: Boolean = false,
```

`domain/Game.kt` — add (after `year`):
```kotlin
    val recommended: Boolean = false,
```

- [ ] **Step 2: Add the Room migration (version 3 → 4)**

In `GobeDatabase.kt`, change `version = 3` to `version = 4`, and add inside `companion object`:
```kotlin
        val MIGRATION_3_4 = androidx.room.migration.Migration(3, 4) { db ->
            db.execSQL("ALTER TABLE games ADD COLUMN recommended INTEGER NOT NULL DEFAULT 0")
        }
```

- [ ] **Step 3: Register the migration**

In `GobeApp.kt`, extend the `addMigrations(...)` call:
```kotlin
            .addMigrations(GobeDatabase.MIGRATION_1_2, GobeDatabase.MIGRATION_2_3, GobeDatabase.MIGRATION_3_4)
```

- [ ] **Step 4: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/db/GameEntity.kt app/src/main/java/com/gobe/tv/domain/Game.kt app/src/main/java/com/gobe/tv/data/db/GobeDatabase.kt app/src/main/java/com/gobe/tv/GobeApp.kt
git commit -m "feat(db): recommended column + MIGRATION_3_4"
```

---

## Task 4: DAO — `updateRecommended` + `searchGames(recommendedOnly)`

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/data/db/GameDao.kt`
- Test (instrumented, run later on device): `app/src/androidTest/java/com/gobe/tv/data/db/GameDaoTest.kt`

- [ ] **Step 1: Change the DAO**

In `GameDao.kt`, update `searchGames` and add `updateRecommended`:

```kotlin
    @Query("SELECT * FROM games WHERE displayName LIKE '%' || :q || '%' AND (:system IS NULL OR system = :system) AND (:genre IS NULL OR genre = :genre) AND (:recommendedOnly = 0 OR recommended = 1) ORDER BY recommended DESC, displayName COLLATE NOCASE ASC")
    fun searchGames(q: String, system: String?, genre: String?, recommendedOnly: Int): Flow<List<GameEntity>>

    @Query("UPDATE games SET recommended = :recommended WHERE id = :id")
    suspend fun updateRecommended(id: Long, recommended: Boolean)
```

(Leave `updateMeta` unchanged — see the plan's "Note on `updateMeta`".)

- [ ] **Step 2: Update the existing `searchGames` call sites (REQUIRED — build break otherwise)**

Room DAO methods can't have Kotlin default args, so the new 4th param breaks every existing 3-arg
call. `GameDaoTest.kt` calls `dao.searchGames(...)` at **lines 73, 80, 82, 108, 110, 112** — append
`, 0` to each (the un-filtered case), e.g. `dao.searchGames("Mario", System.SNES.name, null)` →
`dao.searchGames("Mario", System.SNES.name, null, 0)`. (The app-side call in `LibraryRepository` is
updated in Task 5.) Grep to be sure none are missed:
`grep -rn "\.searchGames(" app/src` — every call must pass 4 args after this task.

- [ ] **Step 3: Add an instrumented DAO test (to run on device later)**

Append to `GameDaoTest.kt` a test mirroring its existing style (in-memory Room). **Build entities with
the real `GameEntity(...)` constructor** (there is no `gameEntity(...)` factory; read the top of the
file for its private `game(...)` helper and required fields — `path`, `system`, `displayName`,
`fileName`, `sizeBytes`, `dateAdded` are required):
```kotlin
    @Test fun searchRecommendedOnlyAndSortsRecommendedFirst() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/a", system = System.SNES, displayName = "Alpha",
                       fileName = "a.sfc", sizeBytes = 1, dateAdded = 1L),           // recommended=false (default)
            GameEntity(path = "/z", system = System.SNES, displayName = "Zeta",
                       fileName = "z.sfc", sizeBytes = 1, dateAdded = 1L),
        ))
        val zeta = dao.getAll().first { it.displayName == "Zeta" }
        dao.updateRecommended(zeta.id, true)
        // recommendedOnly = 1 -> only Zeta
        assertEquals(listOf("Zeta"), dao.searchGames("", null, null, 1).first().map { it.displayName })
        // recommendedOnly = 0 -> Zeta first (recommended DESC), then Alpha
        assertEquals(listOf("Zeta", "Alpha"), dao.searchGames("", null, null, 0).first().map { it.displayName })
    }
```

- [ ] **Step 4: Compile (unit) — instrumented test runs on device later**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (Do NOT run `connectedAndroidTest` now — needs the ONN; defer.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/db/GameDao.kt app/src/androidTest/java/com/gobe/tv/data/db/GameDaoTest.kt
git commit -m "feat(db): DAO recommendedOnly filter + recommended-first sort + updateRecommended"
```

---

## Task 5: LibraryRepository — backfill pass + query + toDomain

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/data/LibraryRepository.kt`

- [ ] **Step 1: Thread `recommended` through `toDomain` + `searchGames`**

In `toDomain()` add `recommended = recommended` to the `Game(...)`:
```kotlin
    private fun GameEntity.toDomain() = Game(
        id, path, system, displayName, fileName, sizeBytes, lastPlayed, dateAdded,
        players = players, boxartName = boxartName, genre = genre, year = year,
        recommended = recommended,
    )
```

Update `searchGames`:
```kotlin
    fun searchGames(query: String, system: System?, genre: String? = null, recommendedOnly: Boolean = false): Flow<List<Game>> =
        gameDao.searchGames(query, system?.name, genre, if (recommendedOnly) 1 else 0)
            .map { list -> list.map { it.toDomain() } }
```

- [ ] **Step 2: Add the recommended backfill pass in `rescan()`**

In `rescan()`, right **before** `return gameDao.getAll().size`, add (uses the Task 2 helper):
```kotlin
        // Refresh `recommended` across ALL games (backfills already-scanned libraries; the candidate
        // filter above only re-matches never-matched rows). Cheap in-memory match; writes on change.
        if (m != null && provider != null) {
            val recoUpdates = recommendedBackfillUpdates(gameDao.getAll()) { e ->
                m.match(e.displayName, provider(e.system))?.recommended ?: false
            }
            if (recoUpdates.isNotEmpty()) {
                runInTransaction {
                    recoUpdates.forEach { (id, reco) -> gameDao.updateRecommended(id, reco) }
                }
            }
        }
```
(`m` and `provider` are the existing `val m = matcher` / `val provider = indexProvider` locals already
declared earlier in `rescan()`. If they're scoped inside the earlier `if` block, hoist those two
`val`s to the top of `rescan()` so both passes can use them.)

- [ ] **Step 3: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/LibraryRepository.kt
git commit -m "feat(library): backfill recommended across all games + searchGames arg"
```

---

## Task 6: HomeViewModel — `recommendedOnly` filter state

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Add state + switch the combine to a 4-field holder**

Add near the other filter flows:
```kotlin
    private val _recommendedOnly = MutableStateFlow(false)
    val recommendedOnly: StateFlow<Boolean> = _recommendedOnly
    fun setRecommendedOnly(v: Boolean) { _recommendedOnly.value = v }
```

Add a private holder (top-level in the file or nested):
```kotlin
    private data class Filters(val q: String, val system: System?, val genre: String?, val recommendedOnly: Boolean)
```

Replace the `filtered` flow (the `Triple` combine can't take a 4th flow):
```kotlin
    private val filtered: Flow<List<Game>> =
        combine(_query, _selectedSystem, _selectedGenre, _recommendedOnly) { q, s, g, r -> Filters(q, s, g, r) }
            .flatMapLatest { f -> repo.searchGames(f.q, f.system, f.genre, f.recommendedOnly) }
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt
git commit -m "feat(home): recommendedOnly filter state"
```

---

## Task 7: HomeScreen — "Recommended" filter chip

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt`

- [ ] **Step 1: Collect the state + add the chip**

Near the other `collectAsState()` calls (~line 52), add:
```kotlin
    val recommendedOnly by vm.recommendedOnly.collectAsState()
```

In the system-chip `Row` (the `Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ... }` that
holds the "All" + `System.entries` chips, ~line 96), add after the `System.entries.forEach { ... }`
block, still inside the `Row`:
```kotlin
            FilterChip(
                label = stringResource(R.string.filter_recommended),
                selected = recommendedOnly,
                onClick = { vm.setRecommendedOnly(!recommendedOnly) },
            )
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt
git commit -m "feat(home): 'Recommended' filter chip"
```

---

## Task 8: GameTile — badge

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt`

- [ ] **Step 1: Add the badge overlay**

Inside the `Box(Modifier.fillMaxSize())` that holds the cover, after the players `Text` block (the
`if (p != null && p >= 2) { ... }`), add a gold star at the top-start corner:
```kotlin
                if (game.recommended) {
                    Text(
                        "★",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/GameTile.kt
git commit -m "feat(home): recommended star badge on tile"
```

---

## Task 9: DetailScreen line + strings (EN/ES)

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Add strings**

`values/strings.xml` (inside `<resources>`):
```xml
    <!-- Recommended badge -->
    <string name="game_recommended">Recommended</string>
    <string name="filter_recommended">Recommended</string>
```
`values-es/strings.xml`:
```xml
    <string name="game_recommended">Recomendado</string>
    <string name="filter_recommended">Recomendados</string>
```

- [ ] **Step 2: Add the detail line**

In `DetailScreen.kt`, after the `detail_year` `Text(...)` + its trailing `Spacer(Modifier.height(4.dp))`
(~line 119), add:
```kotlin
                    if (g.recommended) {
                        Text(
                            "★ " + stringResource(R.string.game_recommended),
                            style = MaterialTheme.typography.bodyLarge,
                            color = androidx.compose.ui.graphics.Color(0xFFFFD54F),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
```

- [ ] **Step 3: Compile**

Run: `./gradlew :app:assembleDebug :app:processDebugResources`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(detail): recommended line + strings (EN/ES)"
```

---

## Task 10: build.py — `select_recommended` + IGDB stage

Maintainer-side data generation. `select_recommended` is pure and self-tested (no network). The IGDB
fetch runs only when `IGDB_CLIENT_ID`/`IGDB_SECRET` are set; otherwise the stage is skipped and the
index builds without flags (graceful).

**Files:**
- Modify: `tools/build-metadata-index/build.py`

- [ ] **Step 1: Add the pure policy + a `--self-test`**

Add this function and a self-test near the top of `build.py`:
```python
def select_recommended(entries, rating_threshold=75, min_votes=5, floor=100, cap=200):
    """entries: iterable of (key, rating, votes). Return the set of recommended keys.

    Policy: only games with votes >= min_votes are candidates; rank them by rating desc; include at
    least the top `floor` (or all candidates if fewer) and everyone at/above `rating_threshold`,
    capped at `cap`. Quality decides; floor/cap keep the set neither too sparse nor too large.
    """
    ranked = sorted(
        [(k, r) for (k, r, v) in entries if r is not None and (v or 0) >= min_votes],
        key=lambda kr: kr[1], reverse=True,
    )
    n_pass = sum(1 for _, r in ranked if r >= rating_threshold)
    n = max(n_pass, min(floor, len(ranked)))  # floor: at least top-`floor` (bounded by size)
    n = min(n, cap)                            # soft cap
    return {k for k, _ in ranked[:n]}


def _self_test():
    # threshold selects high-rated
    assert select_recommended([("a", 90, 50), ("b", 80, 50), ("c", 60, 50)],
                              rating_threshold=75, min_votes=5, floor=1, cap=100) == {"a", "b"}
    # min_votes drops obscure games
    assert select_recommended([("a", 99, 1)], min_votes=5, floor=0, cap=100) == set()
    # floor: few pass threshold -> include the top-floor anyway
    assert select_recommended([("a", 70, 50), ("b", 65, 50), ("c", 60, 50)],
                              rating_threshold=90, min_votes=5, floor=2, cap=100) == {"a", "b"}
    # cap: many pass -> exactly cap
    big = [(f"g{i}", 80 + (i % 10), 50) for i in range(300)]
    assert len(select_recommended(big, rating_threshold=75, min_votes=5, floor=100, cap=200)) == 200
    # fewer candidates than floor -> all candidates
    assert select_recommended([("a", 70, 9), ("b", 50, 9)],
                              rating_threshold=90, min_votes=5, floor=100, cap=200) == {"a", "b"}
    print("select_recommended self-test OK")
```

In `__main__`, before the normal build, handle the flag:
```python
if __name__ == "__main__":
    if "--self-test" in sys.argv:
        _self_test()
        sys.exit(0)
    main()
```
(If the file already calls `main()` at bottom, guard it as above.)

- [ ] **Step 2: Run the self-test (no network)**

Run: `python3 tools/build-metadata-index/build.py --self-test`
Expected: `select_recommended self-test OK`

- [ ] **Step 3: Add the IGDB fetch + merge**

Add the platform map and a fetch that fills `recommended` for each system. IGDB platform IDs:
`SNES=19, NES=18, N64=4, Arcade=52`. Add near `SYSTEMS`:
```python
IGDB_PLATFORM = {"snes": 19, "nes": 18, "n64": 4, "arcade": 52}
IGDB_TOKEN_URL = "https://id.twitch.tv/oauth2/token"
IGDB_GAMES_URL = "https://api.igdb.com/v4/games"


def igdb_token(client_id: str, secret: str) -> str:
    body = urllib.parse.urlencode({
        "client_id": client_id, "client_secret": secret, "grant_type": "client_credentials",
    }).encode()
    req = urllib.request.Request(IGDB_TOKEN_URL, data=body, method="POST")
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read())["access_token"]


def igdb_recommended(tag: str, client_id: str, token: str) -> set:
    """Return the set of normalized keys recommended for this system, or empty on any failure."""
    platform = IGDB_PLATFORM.get(tag)
    if platform is None:
        return set()
    best: dict[str, tuple[float, int]] = {}  # key -> (rating, votes)
    offset = 0
    while True:
        q = (f"fields name,total_rating,total_rating_count; "
             f"where platforms = ({platform}) & total_rating != null & total_rating_count >= 3; "
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
            return set()
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
        offset += 500
        if len(rows) < 500:
            break
    recommended = select_recommended((k, r, v) for k, (r, v) in best.items())
    print(f"  {tag}: IGDB recommended = {len(recommended)}")
    return recommended
```
Add a small POST-capable helper alongside the existing `get()` (which is GET-only):
```python
def get_with(req: urllib.request.Request) -> bytes:
    with urllib.request.urlopen(req) as r:
        return r.read()
```
Then, in the per-system build loop where the output dict for a system is assembled (after boxart +
player/genre/year merge, before writing the JSON), merge the flag:
```python
    client_id = os.environ.get("IGDB_CLIENT_ID")
    secret = os.environ.get("IGDB_SECRET")
    reco_keys = set()
    if client_id and secret:
        try:
            token = igdb_token(client_id, secret)
            reco_keys = igdb_recommended(tag, client_id, token)
        except Exception as e:
            print(f"  !! IGDB auth failed, skipping recommended for {tag}: {e}", file=sys.stderr)
    else:
        print(f"  (no IGDB creds; recommended flag skipped for {tag})", file=sys.stderr)
    for key in reco_keys:
        entry = index.setdefault(key, {})   # tag even games without boxart/players
        entry["recommended"] = True
```
(In the real `main()` the per-system output dict is named **`index`** and the loop var is **`tag`** —
use those. After building each system's dict and before writing the JSON, set `recommended=True` on
the recommended keys, creating bare entries for recommended games that had no other metadata.
Optional: hoist the `igdb_token(...)` call above the per-system loop so it authenticates once instead
of 4×; harmless either way.)

Update the module docstring to mention the IGDB source + the `IGDB_CLIENT_ID`/`IGDB_SECRET` env vars.

- [ ] **Step 4: Verify it still runs without creds (no regression)**

Run (no creds set): `python3 tools/build-metadata-index/build.py --self-test`
Expected: self-test OK. (A full `build.py` run needs network + libretro sources; only run it when
regenerating assets. With no IGDB creds it prints the "skipped" notice and omits flags.)

- [ ] **Step 5: Commit**

```bash
git add tools/build-metadata-index/build.py
git commit -m "feat(tools): IGDB-derived recommended flag in metadata build"
```

---

## Task 11: Full sweep

- [ ] **Step 1: Unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all pass (MetadataIndex, RecommendedBackfill, existing).

- [ ] **Step 2: Assemble**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Python self-test**

Run: `python3 tools/build-metadata-index/build.py --self-test`
Expected: `select_recommended self-test OK`.

- [ ] **Step 4: On-device (ONLY after asking the user)**

Once a dataset with `recommended` flags has been generated (needs IGDB creds) and committed:
regenerate assets, install, and verify: the ★ badge shows on known classics (Super Metroid, Zelda)
and not on shovelware; the "Recommended" chip filters the grid; recommended sort-first holds with the
chip off; the instrumented `GameDaoTest.searchRecommendedOnly...` passes
(`./gradlew :app:connectedDebugAndroidTest`). Until the dataset exists, the plumbing is in place but
no badges appear (expected).

---

## Notes for the implementer

- **Ask before any `adb install` / on-device step** — the user actively uses the ONN.
- The feature is fully built and unit-tested WITHOUT the dataset; badges light up once `build.py` is
  run with IGDB creds and the regenerated `assets/metadata/*.json` are committed. Coordinate the creds
  step with the user.
- Follow existing patterns: `FilterChip`, `stringResource`, TV `Text`/`Card`, the `● `/overlay-chip
  idioms already in `GameTile`/`HomeScreen`.
- YAGNI: no numeric score in the app, no "Essentials" section — boolean flag + badge + filter + sort only.
