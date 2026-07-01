# Browse by Genre — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a genre filter to Home — a second scrollable chip row below the system chips — that composes (AND) with the search box and system filter.

**Architecture:** A new `GameDao.distinctGenres()` feeds the chip labels; `searchGames` gains an optional `genre` predicate. `HomeViewModel` holds `selectedGenre` and folds it into the existing `combine → flatMapLatest` filter. `HomeScreen` renders a `LazyRow` of genre `FilterChip`s. No Room migration (the `genre` column already exists).

**Tech Stack:** Kotlin, Jetpack Compose for TV, Room + Kotlin Flows, JUnit + AndroidX instrumented tests (Room in-memory), on-device verification over wireless adb.

**Spec:** `docs/superpowers/specs/2026-07-01-gobe-genre-filter-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch `feat/genre-filter` already exists (spec committed there). Stay on it.
- **Environment gotchas** (subagents MUST know): `java`/`adb` are NOT on PATH.
  - JDK: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"` before any `./gradlew`.
  - adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Device `192.168.1.219:5555` (ONN). Wake with `adb shell input keyevent KEYCODE_WAKEUP`. **`connectedDebugAndroidTest` uninstalls/reinstalls the app** → re-grant `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow` afterward.
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## File map

- `app/src/main/java/com/gobe/tv/data/db/GameDao.kt` — **Modify**: add `distinctGenres()`; add a `genre` param to `searchGames`.
- `app/src/main/java/com/gobe/tv/data/LibraryRepository.kt` — **Modify**: add `genres()`; add `genre: String? = null` to `searchGames` (default keeps the current call site compiling).
- `app/src/androidTest/java/com/gobe/tv/data/db/GameDaoTest.kt` — **Modify**: instrumented tests for `distinctGenres` + genre filtering.
- `app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt` — **Modify**: `selectedGenre` + `genres` + 3-way combine.
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — **Modify**: genre `LazyRow` of chips.

Current facts: `GameDao.searchGames(q, system)` =
`SELECT * FROM games WHERE displayName LIKE '%'||:q||'%' AND (:system IS NULL OR system = :system) ORDER BY displayName COLLATE NOCASE ASC`.
`LibraryRepository.searchGames(query, system)` → `gameDao.searchGames(query, system?.name).map { it.toDomain() }`.
`HomeViewModel` combines `_query` + `_selectedSystem` via `combine(...).flatMapLatest { repo.searchGames(q, s) }`.
`HomeScreen` renders a `Row` of `FilterChip(label, selected, onClick)` (All + one per `System.entries`).
`GameEntity(path, system, displayName, fileName, sizeBytes, dateAdded, players=null, boxartName=null, genre=null, year=null)`.

---

### Task 1: DAO + repository genre plumbing (instrumented TDD)

**Files:**
- Modify: `app/src/androidTest/java/com/gobe/tv/data/db/GameDaoTest.kt`
- Modify: `app/src/main/java/com/gobe/tv/data/db/GameDao.kt`
- Modify: `app/src/main/java/com/gobe/tv/data/LibraryRepository.kt`

- [ ] **Step 1: Write the failing instrumented tests**

Append to `GameDaoTest.kt` (uses the existing `dao`/`db` in-memory setup and `runBlocking`/`.first()`
pattern already in the file; add imports `com.gobe.tv.domain.System`, `kotlinx.coroutines.flow.first`
if not present):
```kotlin
    private fun game(name: String, system: System, genre: String?) = GameEntity(
        path = "/r/$name", system = system, displayName = name,
        fileName = "$name.x", sizeBytes = 1, dateAdded = 1L, genre = genre,
    )

    @Test fun distinctGenresSortedNonEmpty() = runBlocking {
        dao.insertAll(listOf(
            game("A", System.SNES, "Platform"),
            game("B", System.NES, "Action"),
            game("C", System.SNES, "Action"),
            game("D", System.SNES, null),
            game("E", System.SNES, ""),
        ))
        assertEquals(listOf("Action", "Platform"), dao.distinctGenres().first())
    }

    @Test fun searchFiltersByGenreAndSystem() = runBlocking {
        dao.insertAll(listOf(
            game("A", System.SNES, "Action"),
            game("B", System.NES, "Action"),
            game("C", System.SNES, "Platform"),
        ))
        // genre only
        assertEquals(listOf("A", "B"), dao.searchGames("", null, "Action").first().map { it.displayName })
        // genre + system
        assertEquals(listOf("A"), dao.searchGames("", "SNES", "Action").first().map { it.displayName })
        // no genre = unchanged (all)
        assertEquals(3, dao.searchGames("", null, null).first().size)
    }
```

- [ ] **Step 2: Run the tests, verify they fail to compile / fail**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:connectedDebugAndroidTest --tests "com.gobe.tv.data.db.GameDaoTest"
```
Expected: FAIL — `distinctGenres` unresolved and `searchGames` arity mismatch.
(After it runs: `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow`.)

- [ ] **Step 3: Add the DAO query + param**

In `GameDao.kt`, add `distinctGenres` and add the `genre` predicate to `searchGames`:
```kotlin
    @Query("SELECT DISTINCT genre FROM games WHERE genre IS NOT NULL AND genre != '' ORDER BY genre COLLATE NOCASE ASC")
    fun distinctGenres(): Flow<List<String>>

    @Query("SELECT * FROM games WHERE displayName LIKE '%' || :q || '%' AND (:system IS NULL OR system = :system) AND (:genre IS NULL OR genre = :genre) ORDER BY displayName COLLATE NOCASE ASC")
    fun searchGames(q: String, system: String?, genre: String?): Flow<List<GameEntity>>
```
(Replace the existing 2-arg `searchGames`. `Flow` is already imported in this file.)

- [ ] **Step 4: Update the repository**

In `LibraryRepository.kt`: add `genres()` and thread the (defaulted) genre param so the current
`HomeViewModel` call `repo.searchGames(q, s)` still compiles:
```kotlin
    fun genres(): Flow<List<String>> = gameDao.distinctGenres()

    fun searchGames(query: String, system: System?, genre: String? = null): Flow<List<Game>> =
        gameDao.searchGames(query, system?.name, genre).map { list -> list.map { it.toDomain() } }
```
(Ensure `Flow` is imported; it already is via the existing `searchGames`.)

- [ ] **Step 5: Run the tests, verify they pass**

Same command as Step 2. Expected: PASS. Re-grant storage permission afterward.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/db/GameDao.kt app/src/main/java/com/gobe/tv/data/LibraryRepository.kt app/src/androidTest/java/com/gobe/tv/data/db/GameDaoTest.kt
git commit -m "feat(genre): DAO distinctGenres + genre filter in searchGames"
```

---

### Task 2: ViewModel genre state

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt`

No unit test (thin StateFlow wiring; covered by the DAO test + on-device). Keep building green.

- [ ] **Step 1: Add genre state + fold into the filter**

In `HomeViewModel`:
```kotlin
    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre
    fun setGenre(g: String?) { _selectedGenre.value = g }

    val genres: StateFlow<List<String>> =
        repo.genres().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```
Change `filtered` to combine all three:
```kotlin
    private val filtered: Flow<List<Game>> =
        combine(_query, _selectedSystem, _selectedGenre) { q, s, g -> Triple(q, s, g) }
            .flatMapLatest { (q, s, g) -> repo.searchGames(q, s, g) }
```
(`stateIn`, `combine`, `flatMapLatest`, `Triple` are all available from the existing
`kotlinx.coroutines.flow.*` import.)

- [ ] **Step 2: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt
git commit -m "feat(genre): HomeViewModel selectedGenre + genres list"
```

---

### Task 3: Genre chip row on Home

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt`

- [ ] **Step 1: Collect the new state**

Near the existing `val selectedSystem by vm.selectedSystem.collectAsState()`, add:
```kotlin
    val selectedGenre by vm.selectedGenre.collectAsState()
    val genres by vm.genres.collectAsState()
```

- [ ] **Step 2: Render the genre chip row under the system chips**

Immediately after the system chips `Row { … }` block (the one with `FilterChip(All)` + `System.entries`),
add a genre row. Systems are a small fixed enum (`Row` is fine); genres are unbounded, so use a
`LazyRow`. Only show it when there are genres:
```kotlin
        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    FilterChip(
                        label = stringResource(R.string.filter_all),
                        selected = selectedGenre == null,
                        onClick = { vm.setGenre(null) },
                    )
                }
                items(genres, key = { it }) { genre ->
                    FilterChip(
                        label = genre,
                        selected = selectedGenre == genre,
                        // Re-tapping the selected genre clears back to All.
                        onClick = { vm.setGenre(if (selectedGenre == genre) null else genre) },
                    )
                }
            }
        }
```
(`LazyRow` and `items` (the `foundation.lazy.items` overload for a `List`) are already imported in
this file. `FilterChip` is the existing private composable.)

- [ ] **Step 3: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. Resolve any import (e.g. `Spacer`/`height` are in the wildcard
`foundation.layout.*`) and rebuild.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt
git commit -m "feat(genre): genre chip row on Home (scrollable, composes with system + search)"
```

---

### Task 4: On-device acceptance + RESULTS + merge

- [ ] **Step 1: Unit + instrumented tests, then install**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest --tests "com.gobe.tv.data.db.GameDaoTest"
./gradlew :app:installDebug
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
$ADB shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
$ADB shell input keyevent KEYCODE_WAKEUP
$ADB shell am start -n com.gobe.tv/.MainActivity
```
Expected: tests green; app opens.

- [ ] **Step 2: Acceptance checklist (screenshot each; wake screen first)**

  - A **genre chip row** appears below the system chips, with real genres (Action, Platform, …).
  - Selecting a genre **filters the grid** to that genre; re-tapping it (or "All") clears back.
  - Genre **composes with the system filter and the search box** (e.g. SNES + Action + a query).
  - No regression: with no genre selected, the grid is unchanged; Continue-playing + tiles + legend intact.

- [ ] **Step 3: Write + commit RESULTS**

Create `docs/superpowers/plans/2026-07-01-gobe-genre-filter-RESULTS.md`: what shipped, how genres
look on the user's real library (any messy labels → note normalization as a follow-up), screenshots
referenced. Then:
```bash
git add docs/superpowers/plans/2026-07-01-gobe-genre-filter-RESULTS.md
git commit -m "docs(genre): RESULTS — browse by genre on Home"
```

- [ ] **Step 4: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, merge
`feat/genre-filter` → `main`, push. Re-grant `MANAGE_EXTERNAL_STORAGE` after the final install.

---

## Notes for the implementer

- **Row vs LazyRow:** the system filter stays a `Row` (small fixed enum); the genre filter is a
  `LazyRow` because genres are unbounded — intentional.
- **Green builds:** the repo's `genre` default (`= null`) keeps the ViewModel compiling after Task 1;
  Task 2 then passes the real value.
- **Don't touch** the metadata pipeline, detail, or emulation — scope is the two data files + VM +
  HomeScreen + the DAO test.
- A stale `selectedGenre` (its last game removed) simply yields an empty grid until the user taps
  "All" — acceptable, not handled specially.
