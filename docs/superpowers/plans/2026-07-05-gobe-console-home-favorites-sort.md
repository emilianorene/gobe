# Gobe — Console-first Home + Favorites + Sort Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize Home into a two-level, console-first navigation (level-1 sections → level-2 library), add user favorites (flag + ♥ badge + detail toggle), and a sort selector (Recommended-first / Title / Year).

**Architecture:** A new `LibrarySection` (Console / Recommended / Favorites / SearchAll) maps (pure) to a base DB filter. A new `LibraryScreen` + `LibraryViewModel` renders any section's games with genre chips + a sort control. `HomeScreen` becomes a sections grid; `GobeNavHost` gains a minimal back stack. `favorite` is a new persisted column; `searchGames` grows a `favoritesOnly` filter + a `CASE`-based `sortMode` ordering.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Room (real migrations), Kotlin Flows, JUnit4.

**Spec:** `docs/superpowers/specs/2026-07-05-gobe-console-home-favorites-sort-design.md`

**Build/test env:**
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- Unit tests: `./gradlew :app:testDebugUnitTest` (JVM). Compile: `./gradlew :app:assembleDebug`.
- Gradle is slow (~3–10 min); run builds in the background and wait.
- On-device (ONN): **ask the user before installing** — they use the device.

---

## File Structure

**Create:**
- `app/src/main/java/com/gobe/tv/domain/SortMode.kt` — sort enum.
- `app/src/main/java/com/gobe/tv/ui/library/LibrarySection.kt` — section type + pure `sectionFilter` mapper.
- `app/src/main/java/com/gobe/tv/ui/library/LibraryViewModel.kt`, `LibraryScreen.kt` — level-2 screen.
- `app/src/test/java/com/gobe/tv/ui/library/SectionFilterTest.kt` — mapper test.

**Modify:**
- `data/db/GameEntity.kt`, `domain/Game.kt` — `favorite` field.
- `data/db/GobeDatabase.kt` (version 4→5 + `MIGRATION_4_5`), `GobeApp.kt` (register migration).
- `data/db/GameDao.kt` — `searchGames` (6 args, CASE order) + `updateFavorite`.
- `app/src/androidTest/java/com/gobe/tv/data/db/GameDaoTest.kt` — migrate 8 calls + new tests.
- `data/LibraryRepository.kt` — `searchGames` args + `updateFavorite` + `toDomain`.
- `ui/Routes.kt` — `Route.Library`.
- `ui/GobeNavHost.kt` — back stack + wiring.
- `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt` — sections grid (slimmed VM).
- `ui/detail/DetailScreen.kt` — favorite toggle.
- `ui/home/GameTile.kt` — ♥ badge.
- `res/values/strings.xml`, `res/values-es/strings.xml` — new strings.

**Note:** `SortMode` lives in `domain` (used by repo/DAO layer + UI) to avoid the data layer importing `ui`. `LibrarySection` + mapper live in `ui/library` (a navigation concept).

---

## Task 1: Strings (EN/ES)

Add first so later UI compiles.

**Files:** Modify `res/values/strings.xml`, `res/values-es/strings.xml`.

- [ ] **Step 1: English** — add inside `<resources>`:
```xml
    <!-- v0.3 console-home + favorites + sort -->
    <string name="home_consoles">Consoles</string>
    <string name="section_recommended">Recommended</string>
    <string name="section_favorites">Favorites</string>
    <string name="detail_favorite">Favorite</string>
    <string name="detail_unfavorite">Favorited</string>
    <string name="sort_label">Sort</string>
    <string name="sort_recommended">Recommended</string>
    <string name="sort_title">Title</string>
    <string name="sort_year">Year</string>
    <string name="library_empty">No games here.</string>
```
- [ ] **Step 2: Spanish** — add to `res/values-es/strings.xml`:
```xml
    <string name="home_consoles">Consolas</string>
    <string name="section_recommended">Recomendados</string>
    <string name="section_favorites">Favoritos</string>
    <string name="detail_favorite">Favorito</string>
    <string name="detail_unfavorite">Favorito ✓</string>
    <string name="sort_label">Orden</string>
    <string name="sort_recommended">Recomendados</string>
    <string name="sort_title">Título</string>
    <string name="sort_year">Año</string>
    <string name="library_empty">No hay juegos acá.</string>
```
- [ ] **Step 3:** `./gradlew :app:processDebugResources` → BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit `git add res/values/strings.xml res/values-es/strings.xml && git commit -m "feat(strings): console-home + favorites + sort labels"`

---

## Task 2: `favorite` column + domain + migration

**Files:** `data/db/GameEntity.kt`, `domain/Game.kt`, `data/db/GobeDatabase.kt`, `GobeApp.kt`.

- [ ] **Step 1:** `GameEntity.kt` — add after `recommended`:
```kotlin
    val favorite: Boolean = false,
```
- [ ] **Step 2:** `domain/Game.kt` — add after `recommended`:
```kotlin
    val favorite: Boolean = false,
```
- [ ] **Step 3:** `GobeDatabase.kt` — bump `version = 4` → `version = 5`; add in `companion object`:
```kotlin
        val MIGRATION_4_5 = androidx.room.migration.Migration(4, 5) { db ->
            db.execSQL("ALTER TABLE games ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
        }
```
- [ ] **Step 4:** `GobeApp.kt` — extend `addMigrations(...)`:
```kotlin
            .addMigrations(GobeDatabase.MIGRATION_1_2, GobeDatabase.MIGRATION_2_3, GobeDatabase.MIGRATION_3_4, GobeDatabase.MIGRATION_4_5)
```
- [ ] **Step 5:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 6:** Commit `git add ... && git commit -m "feat(db): favorite column + MIGRATION_4_5"`

---

## Task 3: `SortMode` + `LibrarySection` + pure `sectionFilter` (TDD)

**Files:** Create `domain/SortMode.kt`, `ui/library/LibrarySection.kt`, test `ui/library/SectionFilterTest.kt`.

- [ ] **Step 1: Write the failing test** (`SectionFilterTest.kt`):
```kotlin
package com.gobe.tv.ui.library

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionFilterTest {
    @Test fun consoleMapsToSystem() {
        val f = sectionFilter(LibrarySection.Console(System.SNES))
        assertEquals(System.SNES, f.system)
        assertTrue(!f.recommendedOnly && !f.favoritesOnly && f.query == "")
    }
    @Test fun recommendedSection() {
        assertTrue(sectionFilter(LibrarySection.Recommended).recommendedOnly)
    }
    @Test fun favoritesSection() {
        assertTrue(sectionFilter(LibrarySection.Favorites).favoritesOnly)
    }
    @Test fun searchAllCarriesQuery() {
        val f = sectionFilter(LibrarySection.SearchAll("zelda"))
        assertEquals("zelda", f.query)
        assertEquals(null, f.system)
    }
}
```
- [ ] **Step 2: Run to verify fail** — `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.library.SectionFilterTest"` → FAIL (unresolved).
- [ ] **Step 3: Implement** — `domain/SortMode.kt`:
```kotlin
package com.gobe.tv.domain

/** Grid sort order. dbValue is the int passed to the DAO's CASE-based ORDER BY. */
enum class SortMode(val dbValue: Int) { RECOMMENDED(0), TITLE(1), YEAR(2) }
```
`ui/library/LibrarySection.kt`:
```kotlin
package com.gobe.tv.ui.library

import com.gobe.tv.domain.System

/** A level-1 Home entry: a console, a special collection, or a search. Opens a LibraryScreen. */
sealed interface LibrarySection {
    data class Console(val system: System) : LibrarySection
    data object Recommended : LibrarySection
    data object Favorites : LibrarySection
    data class SearchAll(val query: String) : LibrarySection
}

/** The fixed base DB filter a section maps to (genre + sort are chosen live in the library). Pure. */
data class SectionFilter(
    val query: String,
    val system: System?,
    val recommendedOnly: Boolean,
    val favoritesOnly: Boolean,
)

fun sectionFilter(section: LibrarySection): SectionFilter = when (section) {
    is LibrarySection.Console -> SectionFilter("", section.system, false, false)
    LibrarySection.Recommended -> SectionFilter("", null, true, false)
    LibrarySection.Favorites -> SectionFilter("", null, false, true)
    is LibrarySection.SearchAll -> SectionFilter(section.query, null, false, false)
}
```
- [ ] **Step 4: Run to verify pass** — same command → PASS (4 tests).
- [ ] **Step 5:** Commit `git add domain/SortMode.kt ui/library/LibrarySection.kt test/... && git commit -m "feat(library): SortMode + LibrarySection + pure sectionFilter"`

---

## Task 4: DAO — 6-arg `searchGames` + `updateFavorite` + migrate tests

**Files:** `data/db/GameDao.kt`, `androidTest/.../GameDaoTest.kt`.

- [ ] **Step 1: Change the DAO** — replace `searchGames` and add `updateFavorite`:
```kotlin
    @Query("SELECT * FROM games WHERE displayName LIKE '%' || :q || '%' AND (:system IS NULL OR system = :system) AND (:genre IS NULL OR genre = :genre) AND (:recommendedOnly = 0 OR recommended = 1) AND (:favoritesOnly = 0 OR favorite = 1) ORDER BY CASE WHEN :sortMode = 0 THEN recommended ELSE 0 END DESC, CASE WHEN :sortMode = 2 THEN year END DESC, displayName COLLATE NOCASE ASC")
    fun searchGames(q: String, system: String?, genre: String?, recommendedOnly: Int, favoritesOnly: Int, sortMode: Int): Flow<List<GameEntity>>

    @Query("UPDATE games SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)
```
- [ ] **Step 2: Migrate the 8 existing `searchGames` calls** in `GameDaoTest.kt` (lines ~73, 80, 82, 108, 110, 112, 125, 127): append `, 0, 0` (favoritesOnly=0, sortMode=0). Grep to be sure: `grep -rn "\.searchGames(" app/src` — every call must have 6 args (except `LibraryRepository`, updated in Task 5).
- [ ] **Step 3: Add new instrumented tests** to `GameDaoTest.kt` (uses the existing in-memory Room `dao`; build entities with the `GameEntity(...)` constructor — required fields `path, system, displayName, fileName, sizeBytes, dateAdded`):
```kotlin
    @Test fun favoritesOnlyFilters() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/a", system = System.SNES, displayName = "Alpha", fileName = "a", sizeBytes = 1, dateAdded = 1L),
            GameEntity(path = "/b", system = System.SNES, displayName = "Beta", fileName = "b", sizeBytes = 1, dateAdded = 1L),
        ))
        val beta = dao.getAll().first { it.displayName == "Beta" }
        dao.updateFavorite(beta.id, true)
        assertEquals(listOf("Beta"), dao.searchGames("", null, null, 0, 1, 0).first().map { it.displayName })
        // favorite persists after an unrelated recommended write (independence)
        dao.updateRecommended(beta.id, true)
        assertEquals(true, dao.getById(beta.id)!!.favorite)
    }

    @Test fun sortModeTitleAndYear() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/z", system = System.SNES, displayName = "Zeta", fileName = "z", sizeBytes = 1, dateAdded = 1L, year = 1990),
            GameEntity(path = "/a", system = System.SNES, displayName = "Alpha", fileName = "a", sizeBytes = 1, dateAdded = 1L, year = 1995),
            GameEntity(path = "/n", system = System.SNES, displayName = "NoYear", fileName = "n", sizeBytes = 1, dateAdded = 1L, year = null),
        ))
        // TITLE (mode 1): alphabetical
        assertEquals(listOf("Alpha", "NoYear", "Zeta"), dao.searchGames("", null, null, 0, 0, 1).first().map { it.displayName })
        // YEAR (mode 2): newest first, unknown year last
        assertEquals(listOf("Alpha", "Zeta", "NoYear"), dao.searchGames("", null, null, 0, 0, 2).first().map { it.displayName })
    }
```
- [ ] **Step 4: Update `LibraryRepository` in the SAME task (else the build breaks).** Changing the
  DAO to 6 args breaks `LibraryRepository.searchGames` (it calls the old 4-arg form at
  `LibraryRepository.kt:104`), so this must be fixed here, not in a later task. Add import
  `import com.gobe.tv.domain.SortMode`; replace `searchGames` + add `updateFavorite`:
```kotlin
    fun searchGames(
        query: String, system: System?, genre: String? = null,
        recommendedOnly: Boolean = false, favoritesOnly: Boolean = false,
        sortMode: SortMode = SortMode.RECOMMENDED,
    ): Flow<List<Game>> =
        gameDao.searchGames(
            query, system?.name, genre,
            if (recommendedOnly) 1 else 0, if (favoritesOnly) 1 else 0, sortMode.dbValue,
        ).map { list -> list.map { it.toDomain() } }

    suspend fun updateFavorite(id: Long, favorite: Boolean) = gameDao.updateFavorite(id, favorite)
```
In `toDomain()` add `favorite = favorite` to the `Game(...)`:
```kotlin
        players = players, boxartName = boxartName, genre = genre, year = year,
        recommended = recommended, favorite = favorite,
```
(`HomeViewModel` still calls `searchGames` with the new params defaulted — compiles fine; it's removed in Task 8.)
- [ ] **Step 5:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL. (Instrumented tests run on device later; do NOT run `connectedAndroidTest` now.)
- [ ] **Step 6:** Commit `git add data/db/GameDao.kt androidTest/.../GameDaoTest.kt data/LibraryRepository.kt && git commit -m "feat(db): favoritesOnly filter + CASE sortMode order + updateFavorite + repo threading"`

---

## Task 5: (merged into Task 4)

The `LibraryRepository` threading is done in Task 4 Step 4 (it must be, or Task 4's build breaks). This
task number is intentionally a no-op; continue at Task 6.

---

## Task 6: Navigation — `Route.Library` + back stack

**Files:** `ui/Routes.kt`, `ui/GobeNavHost.kt`.

- [ ] **Step 1:** `Routes.kt` — add the route (import the section):
```kotlin
import com.gobe.tv.ui.library.LibrarySection
```
```kotlin
    data class Library(val section: LibrarySection) : Route
```
- [ ] **Step 2:** Rewrite `GobeNavHost.kt` to use a back stack. Full file:
```kotlin
package com.gobe.tv.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gobe.tv.GobeApp
import com.gobe.tv.data.StoragePermission
import com.gobe.tv.ui.detail.DetailScreen
import com.gobe.tv.ui.folders.FolderBrowserScreen
import com.gobe.tv.ui.folders.FoldersScreen
import com.gobe.tv.ui.home.HomeScreen
import com.gobe.tv.ui.library.LibraryScreen
import com.gobe.tv.ui.library.LibrarySection
import com.gobe.tv.ui.onboarding.PermissionScreen
import com.gobe.tv.ui.settings.SettingsScreen

@Composable
fun GobeNavHost(app: GobeApp) {
    // Minimal back stack: push to navigate, pop on back. Top of stack is the current screen.
    var stack by remember {
        mutableStateOf(listOf<Route>(if (StoragePermission.isGranted()) Route.Home else Route.Permission))
    }
    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun resetTo(r: Route) { stack = listOf(r) }
    val route = stack.last()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (app.returnToHomeOnResume) {
                    app.returnToHomeOnResume = false
                    if (StoragePermission.isGranted()) { resetTo(Route.Home); return@LifecycleEventObserver }
                }
                // Read the stack FRESH here — do not use the composition-captured `route` local,
                // which the observer closure would freeze at first composition (stale-value bug).
                if (!StoragePermission.isGranted()) resetTo(Route.Permission)
                else if (stack.last() is Route.Permission) resetTo(Route.Home)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val r = route) {
        is Route.Permission -> PermissionScreen(onGranted = { resetTo(Route.Home) })
        is Route.Home -> HomeScreen(
            app = app,
            onOpenSection = { push(Route.Library(it)) },
            onOpenGame = { push(Route.Detail(it)) },
            onOpenSettings = { push(Route.Settings) },
        )
        is Route.Library -> LibraryScreen(
            app = app,
            section = r.section,
            onOpenGame = { push(Route.Detail(it)) },
            onBack = { pop() },
        )
        is Route.Settings -> SettingsScreen(
            onOpenFolders = { push(Route.Folders) },
            onBack = { pop() },
        )
        is Route.Folders -> FoldersScreen(
            app = app,
            onBrowse = { start -> push(Route.FolderBrowser(start)) },
            onBack = { pop() },
        )
        is Route.FolderBrowser -> FolderBrowserScreen(
            startPath = r.startPath,
            onPicked = { pop() },
            onBack = { pop() },
        )
        is Route.Detail -> DetailScreen(
            app = app, gameId = r.gameId, onBack = { pop() },
        )
    }
}
```
- [ ] **Step 3:** This won't compile yet — `HomeScreen(onOpenSection=…)` and `LibraryScreen` don't exist. That's expected; they're added in Tasks 7–8. To keep this task independently committable, you may temporarily stub, but simpler: **do Tasks 6→7→8 together and compile after Task 8.** (Mark Task 6 done once Tasks 7–8 land and `assembleDebug` is green.)
- [ ] **Step 4:** Commit with Tasks 7–8 (or alone if stubbed): `git commit -m "feat(nav): Route.Library + back stack"`

---

## Task 7: `LibraryViewModel` + `LibraryScreen`

**Files:** Create `ui/library/LibraryViewModel.kt`, `ui/library/LibraryScreen.kt`.

- [ ] **Step 1: `LibraryViewModel.kt`:**
```kotlin
package com.gobe.tv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.SortMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val repo: LibraryRepository, section: LibrarySection) : ViewModel() {
    private val base = sectionFilter(section)

    private val _genre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _genre
    private val _sort = MutableStateFlow(SortMode.RECOMMENDED)
    val sortMode: StateFlow<SortMode> = _sort

    fun setGenre(g: String?) { _genre.value = g }
    fun cycleSort() { _sort.value = SortMode.entries[(_sort.value.ordinal + 1) % SortMode.entries.size] }

    // Only genre + sort are reactive; the base filter is fixed by the section.
    val games: StateFlow<List<Game>> =
        combine(_genre, _sort) { g, s -> g to s }
            .flatMapLatest { (g, s) ->
                repo.searchGames(base.query, base.system, g, base.recommendedOnly, base.favoritesOnly, s)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Genres present in THIS section (derived from the section's games, ignoring the genre chip).
    val genres: StateFlow<List<String>> =
        repo.searchGames(base.query, base.system, null, base.recommendedOnly, base.favoritesOnly, SortMode.TITLE)
            .map { list -> list.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```
- [ ] **Step 2: `LibraryScreen.kt`** (mirrors HomeScreen's grid + FilterChip patterns):
```kotlin
package com.gobe.tv.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.SortMode
import com.gobe.tv.ui.folders.vmFactory
import com.gobe.tv.ui.home.GameTile

@Composable
fun LibraryScreen(app: GobeApp, section: LibrarySection, onOpenGame: (Long) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    // MUST key by section: there is one ViewModelStoreOwner (the Activity), so an unkeyed
    // viewModel() caches by class name and a second section would reuse the first's VM (showing the
    // wrong games). `section.toString()` is a stable per-section key (data class/object toString).
    val vm: LibraryViewModel = viewModel(key = section.toString(), factory = vmFactory { LibraryViewModel(app.repository, section) })
    val games by vm.games.collectAsState()
    val genres by vm.genres.collectAsState()
    val selectedGenre by vm.selectedGenre.collectAsState()
    val sortMode by vm.sortMode.collectAsState()

    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Text(sectionTitle(section), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Sort control (cycles Recommended -> Title -> Year).
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.cycleSort() }) {
                Text("↕ " + stringResource(R.string.sort_label) + ": " + sortLabel(sortMode))
            }
        }

        if (genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Button(onClick = { vm.setGenre(null) }) {
                        Text((if (selectedGenre == null) "● " else "") + stringResource(R.string.filter_all))
                    }
                }
                items(genres, key = { it }) { genre ->
                    Button(onClick = { vm.setGenre(if (selectedGenre == genre) null else genre) }) {
                        Text((if (selectedGenre == genre) "● " else "") + genre)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        if (games.isEmpty()) {
            Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(132.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(games, key = { it.id }) { g ->
                    GameTile(game = g, onClick = { onOpenGame(g.id) }, requestInitialFocus = g == games.first())
                }
            }
        }
    }
}

@Composable
private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.RECOMMENDED -> stringResource(R.string.sort_recommended)
    SortMode.TITLE -> stringResource(R.string.sort_title)
    SortMode.YEAR -> stringResource(R.string.sort_year)
}

@Composable
private fun sectionTitle(section: LibrarySection): String = when (section) {
    is LibrarySection.Console -> section.system.displayName
    LibrarySection.Recommended -> stringResource(R.string.section_recommended)
    LibrarySection.Favorites -> stringResource(R.string.section_favorites)
    is LibrarySection.SearchAll -> "\"" + section.query + "\""
}
```
> Note: `GameTile` is `public` in `ui/home`; import it. `vmFactory` is in `ui/folders`.
- [ ] **Step 3:** Compile happens after Task 8 (Home provides `onOpenSection`). Commit with Task 8 or alone once the app compiles: `git commit -m "feat(library): LibraryScreen + LibraryViewModel"`

---

## Task 8: HomeScreen rework (sections grid) + slim HomeViewModel

**Files:** `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`.

- [ ] **Step 1: Slim `HomeViewModel.kt`** to scanning + continue-playing only:
```kotlin
package com.gobe.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val continuePlaying: List<Game> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    private val scanning = MutableStateFlow(true)

    val state: StateFlow<HomeState> =
        combine(repo.observeContinuePlaying(), scanning) { cont, scan ->
            HomeState(continuePlaying = cont, loading = scan && cont.isEmpty())
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), HomeState())

    init {
        viewModelScope.launch {
            scanning.value = true
            repo.ensureDefaultFolder(defaultPath)
            repo.rescan()
            scanning.value = false
        }
    }
}
```
- [ ] **Step 2: Rewrite `HomeScreen.kt`** — sections grid + search that navigates. Full file:
```kotlin
package com.gobe.tv.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.GobeApp
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.folders.vmFactory
import com.gobe.tv.ui.library.LibrarySection

@Composable
fun HomeScreen(
    app: GobeApp,
    onOpenSection: (LibrarySection) -> Unit,
    onOpenGame: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = vmFactory { HomeViewModel(app.repository, app.defaultRomPath) })
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val settingsFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Level-1 sections: special collections first, then one per console.
    val sections: List<Pair<String, LibrarySection>> = buildList {
        add(stringResource(R.string.section_recommended) to LibrarySection.Recommended)
        add(stringResource(R.string.section_favorites) to LibrarySection.Favorites)
        System.entries.forEach { add(it.displayName to LibrarySection.Console(it)) }
    }

    Column(
        Modifier.fillMaxSize().padding(40.dp).onPreviewKeyEvent { event ->
            val action = keyToHomeAction(event.key.nativeKeyCode) ?: return@onPreviewKeyEvent false
            if (event.type == KeyEventType.KeyDown) when (action) {
                HomeKeyAction.Search -> { runCatching { searchFocus.requestFocus() }; keyboard?.show() }
                HomeKeyAction.Settings -> onOpenSettings()
            }
            true
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.gobe_logo), "Gobe", modifier = Modifier.height(56.dp))
            Spacer(Modifier.width(24.dp))
            SearchField(
                value = query, onValueChange = { query = it },
                onSubmit = { if (query.isNotBlank()) onOpenSection(LibrarySection.SearchAll(query.trim())) },
                focusRequester = searchFocus, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(24.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.focusRequester(settingsFocus)) {
                Text("⚙ " + stringResource(R.string.home_settings))
            }
        }
        Spacer(Modifier.height(24.dp))

        val hasContinue = state.continuePlaying.isNotEmpty()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Text(stringResource(R.string.home_scanning), style = MaterialTheme.typography.bodyLarge)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (hasContinue) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                Text(stringResource(R.string.home_continue_playing), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(state.continuePlaying, key = { it.id }) { g ->
                                        GameTile(game = g, onClick = { onOpenGame(g.id) },
                                            requestInitialFocus = g == state.continuePlaying.first())
                                    }
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(R.string.home_consoles), style = MaterialTheme.typography.titleLarge)
                    }
                    itemsIndexed(sections) { i, (label, section) ->
                        SectionTile(
                            label = label,
                            onClick = { onOpenSection(section) },
                            requestInitialFocus = !hasContinue && i == 0,
                        )
                    }
                }
            }
        }
        HomeControlLegend()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTile(label: String, onClick: () -> Unit, requestInitialFocus: Boolean) {
    val focus = remember { FocusRequester() }
    if (requestInitialFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Card(
        onClick = onClick,
        modifier = (if (requestInitialFocus) Modifier.focusRequester(focus) else Modifier)
            .height(96.dp).fillMaxWidth(),
        colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SearchField(
    value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit,
    focusRequester: FocusRequester, modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() { keyboard?.hide(); onSubmit(); focusManager.moveFocus(FocusDirection.Down) }
    Box(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(textColor),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submit() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(focusRequester)
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                        keyboard?.hide(); focusManager.moveFocus(FocusDirection.Down); true
                    } else false
                },
            decorationBox = { inner ->
                if (value.isEmpty()) Text(stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge, color = textColor.copy(alpha = 0.5f))
                inner()
            },
        )
    }
}

@Composable
private fun HomeControlLegend() {
    val select = stringResource(R.string.legend_select)
    val back = stringResource(R.string.legend_back)
    val search = stringResource(R.string.legend_search)
    val settings = stringResource(R.string.legend_settings)
    Text("Ⓐ $select  ·  Ⓑ $back  ·  L1 $search  ·  R1 $settings",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
}
```
> Needs `import androidx.compose.foundation.lazy.grid.itemsIndexed`. Keeps `HomeKeyMap.kt` (`keyToHomeAction`, `HomeKeyAction`) unchanged. The old `SearchField`/`FilterChip`/`EmptyState` are replaced by the above; delete the leftover `FilterChip`/`EmptyState` if unused (or leave `EmptyState` out — not referenced now).
- [ ] **Step 3:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (this is where Tasks 6+7+8 compile together).
- [ ] **Step 4:** Commit `git add ui/Routes.kt ui/GobeNavHost.kt ui/library/ ui/home/HomeViewModel.kt ui/home/HomeScreen.kt && git commit -m "feat(home): console-first sections grid + level-2 library + back-stack nav"`

---

## Task 9: DetailScreen favorite toggle

**Files:** `ui/detail/DetailScreen.kt`.

- [ ] **Step 1:** Add favorite state + toggle. After `var game by remember...` add a coroutine scope + a favorite mirror:
```kotlin
    val scope = androidx.compose.runtime.rememberCoroutineScope()
```
The `game` already carries `favorite`. Add a local mutable mirror so the button reflects taps immediately:
```kotlin
    var favorite by remember { mutableStateOf(false) }
    LaunchedEffect(game) { favorite = game?.favorite ?: false }
```
Add the toggle button in the actions column, between the Play/Resume block and the Back button (after the `Spacer(Modifier.height(12.dp))` that precedes Back):
```kotlin
                    Button(onClick = {
                        val id = g.id
                        favorite = !favorite
                        scope.launch { app.repository.updateFavorite(id, favorite) }
                    }) {
                        Text((if (favorite) "♥ " else "♡ ") +
                            stringResource(if (favorite) R.string.detail_unfavorite else R.string.detail_favorite))
                    }
                    Spacer(Modifier.height(12.dp))
```
Add import `import kotlinx.coroutines.launch`.
- [ ] **Step 2:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Commit `git add ui/detail/DetailScreen.kt && git commit -m "feat(detail): favorite toggle"`

---

## Task 10: GameTile ♥ badge

**Files:** `ui/home/GameTile.kt`.

- [ ] **Step 1:** Inside the cover `Box(Modifier.fillMaxSize())`, after the recommended `if (game.recommended) { ... }` block, add a favorite heart at the bottom-start:
```kotlin
                if (game.favorite) {
                    Text(
                        "♥",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
```
- [ ] **Step 2:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Commit `git add ui/home/GameTile.kt && git commit -m "feat(home): favorite heart badge on tile"`

---

## Task 11: Full sweep

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (SectionFilterTest + existing pure tests).
- [ ] **Step 2:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3: On-device (ONLY after asking the user):** Home shows Recommended/Favorites + console sections; entering SNES shows only SNES with ★/♥; the sort button cycles Recommended→Title→Year and reorders; marking a favorite in Detail shows the ♥ and it appears in the Favorites section; Back returns Library→Home; global search from Home opens a results library; instrumented `GameDaoTest` (favoritesOnly + sortMode) passes via `connectedDebugAndroidTest`.

---

## Notes for the implementer
- **Ask before any `adb install`/on-device step** — the user actively uses the ONN.
- Tasks 6–8 compile together (nav references Home/Library which are built in 7–8); commit them as a group if needed, but keep the commit messages per-task.
- Do NOT bump the app version / CHANGELOG here — that's release packaging, done later (this ships in v0.3.0-beta with the recommended feature).
- Follow existing patterns: TV `Card`/`Button`/`Text`, `vmFactory`, `GameTile`, `stringResource`, the `● ` selected-prefix idiom.
