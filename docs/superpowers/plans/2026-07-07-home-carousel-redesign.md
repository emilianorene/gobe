# Home Screen Carousel Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Gobe's flat home grid with a cinematic full-screen console carousel where choosing a console is the hero action, using real public-domain controller photos; demote Continue-playing to a per-console contextual strip; move Recommended/Favorites off home into a per-console Library filter.

**Architecture:** Single-Activity Jetpack Compose for TV app with a hand-rolled nav stack (`GobeNavHost`). The home screen is one `@Composable` (`HomeScreen`) fed by `HomeViewModel` over a Room-backed `LibraryRepository`. We add two Room queries (counts-by-system, recent-by-system), a handful of **pure** functions (carousel navigation, console art mapping, collection-filter flags) that carry the testable logic, then rewrite the `HomeScreen` composable and extend `LibraryViewModel`/`LibraryScreen`. Following the codebase's testing style: pure functions get JVM unit tests, new DAO queries get instrumented Room tests, and the Compose surface is verified on-device (no Compose UI test harness exists in this project).

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv.material3`), Room, Kotlin coroutines/Flow, Coil (existing). Tests: JUnit4 + `coroutines-test` (unit, `app/src/test`), Room in-memory + AndroidJUnit4 (instrumented, `app/src/androidTest`). Build: Gradle wrapper (`./gradlew`).

**Conventions observed:**
- Pure logic lives in small top-level functions beside their composable (see `tileCaption` in `GameTile.kt`, `sectionFilter` in `LibrarySection.kt`, `keyToHomeAction` in `HomeKeyMap.kt`) and is unit-tested (`app/src/test/.../ui/...`).
- `System.entries` order is the canonical console order: **NES, SNES, N64, ARCADE**.
- Per-console accent colors already exist in `ui/theme/Color.kt` (`GobeAccentNes/Snes/N64/Arcade`).
- Games' `system` column is stored as the enum **name** string (see `searchGames(system?.name, ...)`).

**Unit-test command:** `./gradlew :app:testDebugUnitTest`
**Instrumented-test command:** `./gradlew :app:connectedDebugAndroidTest` (needs a running Android TV emulator/device; if none is available, note it and defer running — do not delete the test).
**Build command:** `./gradlew :app:assembleDebug`

**Recommended:** execute in a dedicated git worktree.

---

## File Structure

**Create:**
- `app/src/main/res/drawable-nodpi/console_nes.png` — NES controller photo (PD, resized)
- `app/src/main/res/drawable-nodpi/console_snes.png` — SNES controller photo
- `app/src/main/res/drawable-nodpi/console_n64.png` — N64 controller photo
- `app/src/main/java/com/gobe/tv/ui/home/ConsoleArt.kt` — pure `System → (art drawable, isPhoto, accent)` map
- `app/src/main/java/com/gobe/tv/ui/home/HomeCarousel.kt` — pure carousel model (visible list, default focus, move)
- `app/src/main/java/com/gobe/tv/ui/home/ConsoleHero.kt` — the plate+photo+name+count hero composable
- `app/src/main/java/com/gobe/tv/ui/library/CollectionFilter.kt` — pure filter enum → flags
- `app/src/test/java/com/gobe/tv/ui/home/ConsoleArtTest.kt`
- `app/src/test/java/com/gobe/tv/ui/home/HomeCarouselTest.kt`
- `app/src/test/java/com/gobe/tv/ui/library/CollectionFilterTest.kt`
- `app/src/androidTest/java/com/gobe/tv/data/db/GameDaoHomeQueriesTest.kt`

**Modify:**
- `app/src/main/java/com/gobe/tv/data/db/GameDao.kt` — add `observeCountsBySystem()`, `observeContinuePlayingBySystem()`, and `SystemCount` projection
- `app/src/main/java/com/gobe/tv/data/LibraryRepository.kt` — wrapper flows for the two new queries
- `app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt` — new `HomeState` (consoles + focusedSystem + continueForFocused)
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — rewrite as the carousel
- `app/src/main/java/com/gobe/tv/ui/home/SectionVisuals.kt` — drop Recommended/Favorites branches (Task 9)
- `app/src/main/java/com/gobe/tv/ui/library/LibrarySection.kt` — remove Recommended/Favorites variants + branches (Task 9)
- `app/src/main/java/com/gobe/tv/ui/library/LibraryViewModel.kt` — add reactive collection filter
- `app/src/main/java/com/gobe/tv/ui/library/LibraryScreen.kt` — add filter chips
- `app/src/test/java/com/gobe/tv/ui/home/SectionVisualsTest.kt` — drop Recommended/Favorites assertions (Task 9)
- `app/src/test/java/com/gobe/tv/ui/library/SectionFilterTest.kt` — update for removed variants (Task 9)

**Unchanged:** `GobeNavHost.kt`, `Routes.kt`, `DetailScreen.kt`, Settings, `HomeKeyMap.kt` (L1/R1 semantics kept), `GameTile.kt`, theme tokens.

---

## Task 1: Bundle the console controller photos

Real public-domain photos (Evan-Amos / Vanamo, via Wikimedia Commons — public domain). Downloaded via `Special:FilePath` (redirects to the current file), resized to a TV-friendly 1080px long edge, saved as PNG into `drawable-nodpi/` (same folder as `gobe_logo.png`). Validated: `sips -Z 1080 -s format png` yields ~0.5–0.7 MB PNGs.

**Files:**
- Create: `app/src/main/res/drawable-nodpi/console_nes.png`, `console_snes.png`, `console_n64.png`

- [ ] **Step 1: Download + convert the three photos**

Run (from repo root):

```bash
DEST="app/src/main/res/drawable-nodpi"
tmp="$(mktemp -d)"
dl() { curl -sL -A "Mozilla/5.0" "https://commons.wikimedia.org/wiki/Special:FilePath/$1" -o "$tmp/src"; sips -Z 1080 -s format png "$tmp/src" --out "$DEST/$2" >/dev/null; }
dl "NES-Controller-Flat.jpg"            console_nes.png
dl "Nintendo-Super-NES-Controller.jpg"  console_snes.png
dl "N64-Controller-Gray.jpg"            console_n64.png
ls -lh "$DEST"/console_*.png
```

Expected: three PNGs listed, each roughly 0.3–0.8 MB, no errors. (If a filename ever 404s, re-resolve it via `https://commons.wikimedia.org/w/api.php?action=query&titles=File:<name>&prop=imageinfo&iiprop=url&format=json`.)

- [ ] **Step 2: Verify Android accepts the resource names**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (Resource names are lowercase with underscores, so `aapt` accepts them; `R.drawable.console_nes/console_snes/console_n64` now exist.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable-nodpi/console_nes.png app/src/main/res/drawable-nodpi/console_snes.png app/src/main/res/drawable-nodpi/console_n64.png
git commit -m "assets(home): bundle public-domain NES/SNES/N64 controller photos"
```

> **Follow-up (not this plan):** source a proper PD **Arcade** photo; until then Arcade uses its existing vector icon (Task 2). Credit Evan Amos on the About/licenses screen.

---

## Task 2: Console art mapping (pure)

A single pure table from `System` to its home-carousel art: a **photo** for NES/SNES/N64, the existing **vector** placeholder for Arcade, plus the accent color. The `isPhoto` flag tells the hero whether to apply the white-knockout blend (photos) or draw straight (transparent vector).

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/home/ConsoleArt.kt`
- Test: `app/src/test/java/com/gobe/tv/ui/home/ConsoleArtTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.ui.home

import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleArtTest {
    @Test fun photoConsolesUsePhotosAndAccents() {
        assertEquals(R.drawable.console_nes, consoleArt(System.NES).art)
        assertTrue(consoleArt(System.NES).isPhoto)
        assertEquals(GobeAccentNes, consoleArt(System.NES).accent)
        assertEquals(R.drawable.console_snes, consoleArt(System.SNES).art)
        assertEquals(GobeAccentSnes, consoleArt(System.SNES).accent)
        assertEquals(R.drawable.console_n64, consoleArt(System.N64).art)
        assertEquals(GobeAccentN64, consoleArt(System.N64).accent)
    }
    @Test fun arcadeFallsBackToVectorIcon() {
        val arcade = consoleArt(System.ARCADE)
        assertEquals(R.drawable.ic_controller_arcade, arcade.art)
        assertFalse(arcade.isPhoto)
        assertEquals(GobeAccentArcade, arcade.accent)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.ConsoleArtTest"`
Expected: FAIL — `consoleArt` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.gobe.tv.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.theme.*

/** Home-carousel art for a console: a photo (NES/SNES/N64) or the vector placeholder (Arcade). */
data class ConsoleArt(@DrawableRes val art: Int, val isPhoto: Boolean, val accent: Color)

/** Pure map from a [System] to its carousel art + accent. Single source of truth. */
fun consoleArt(system: System): ConsoleArt = when (system) {
    System.NES    -> ConsoleArt(R.drawable.console_nes, isPhoto = true, accent = GobeAccentNes)
    System.SNES   -> ConsoleArt(R.drawable.console_snes, isPhoto = true, accent = GobeAccentSnes)
    System.N64    -> ConsoleArt(R.drawable.console_n64, isPhoto = true, accent = GobeAccentN64)
    System.ARCADE -> ConsoleArt(R.drawable.ic_controller_arcade, isPhoto = false, accent = GobeAccentArcade)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.ConsoleArtTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/ConsoleArt.kt app/src/test/java/com/gobe/tv/ui/home/ConsoleArtTest.kt
git commit -m "feat(home): pure console-art mapping (photo vs vector + accent)"
```

---

## Task 3: Carousel model (pure navigation + selection)

Pure logic for: which consoles are visible (have ≥1 game, in `System.entries` order), the default focus, and moving focus left/right **without wrapping** (per spec). This is the testable core of the carousel; the composable just calls these.

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/home/HomeCarousel.kt`
- Test: `app/src/test/java/com/gobe/tv/ui/home/HomeCarouselTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.ui.home

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCarouselTest {
    @Test fun visibleKeepsOnlyNonEmptyInSystemOrder() {
        val counts = mapOf(System.SNES to 3, System.NES to 5, System.ARCADE to 0)
        assertEquals(listOf(System.NES, System.SNES), visibleConsoles(counts).map { it.system })
    }
    @Test fun visibleCarriesCounts() {
        assertEquals(5, visibleConsoles(mapOf(System.NES to 5)).first().count)
    }
    @Test fun defaultFocusIsFirstVisibleOrNull() {
        assertEquals(System.NES, defaultFocus(visibleConsoles(mapOf(System.NES to 1, System.N64 to 2))))
        assertNull(defaultFocus(emptyList()))
    }
    @Test fun moveClampsAtEndsNoWrap() {
        val v = visibleConsoles(mapOf(System.NES to 1, System.SNES to 1, System.N64 to 1))
        assertEquals(System.SNES, moveFocus(v, System.NES, +1))
        assertEquals(System.NES, moveFocus(v, System.NES, -1))   // clamp at left
        assertEquals(System.N64, moveFocus(v, System.N64, +1))   // clamp at right
    }
    @Test fun moveFromUnknownStartsAtFirst() {
        val v = visibleConsoles(mapOf(System.NES to 1, System.SNES to 1))
        assertEquals(System.SNES, moveFocus(v, null, +1))
        assertNull(moveFocus(emptyList(), null, +1))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.HomeCarouselTest"`
Expected: FAIL — `visibleConsoles`/`defaultFocus`/`moveFocus` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.gobe.tv.ui.home

import com.gobe.tv.domain.System

/** A console shown in the carousel: the system and how many games it has. */
data class ConsoleEntry(val system: System, val count: Int)

/** Consoles with at least one game, in canonical [System.entries] order. Pure. */
fun visibleConsoles(counts: Map<System, Int>): List<ConsoleEntry> =
    System.entries.mapNotNull { s ->
        val c = counts[s] ?: 0
        if (c > 0) ConsoleEntry(s, c) else null
    }

/** First visible console, or null when the library is empty. Pure. */
fun defaultFocus(visible: List<ConsoleEntry>): System? = visible.firstOrNull()?.system

/** Move focus by [delta] within [visible], clamped to the ends (no wrap). Pure. */
fun moveFocus(visible: List<ConsoleEntry>, current: System?, delta: Int): System? {
    if (visible.isEmpty()) return null
    val idx = visible.indexOfFirst { it.system == current }.let { if (it < 0) 0 else it }
    return visible[(idx + delta).coerceIn(0, visible.size - 1)].system
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.HomeCarouselTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeCarousel.kt app/src/test/java/com/gobe/tv/ui/home/HomeCarouselTest.kt
git commit -m "feat(home): pure carousel model (visible consoles, focus, clamp nav)"
```

---

## Task 4: DAO — counts-by-system and recent-by-system

Two new Room queries: a reactive per-system game count (drives which consoles show + the "N games" subtitle), and continue-playing filtered to one system (drives the contextual strip). Instrumented test mirrors the existing `GameDaoTest` pattern (in-memory Room, `runBlocking`, `flow.first()`).

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/data/db/GameDao.kt`
- Test: `app/src/androidTest/java/com/gobe/tv/data/db/GameDaoHomeQueriesTest.kt`

- [ ] **Step 1: Write the failing instrumented test**

```kotlin
package com.gobe.tv.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gobe.tv.domain.System
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDaoHomeQueriesTest {
    private lateinit var db: GobeDatabase
    private lateinit var dao: GameDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), GobeDatabase::class.java
        ).build()
        dao = db.gameDao()
    }
    @After fun close() = db.close()

    private fun game(path: String, system: System) = GameEntity(
        path = path, system = system, displayName = path, fileName = path, sizeBytes = 1, dateAdded = 1L,
    )

    @Test fun countsBySystemGroupsAndCounts() = runBlocking {
        dao.insertAll(listOf(
            game("/a.nes", System.NES), game("/b.nes", System.NES),
            game("/c.sfc", System.SNES),
        ))
        val counts = dao.observeCountsBySystem().first().associate { it.system to it.count }
        assertEquals(2, counts[System.NES])
        assertEquals(1, counts[System.SNES])
        assertEquals(null, counts[System.N64])   // absent systems not present
    }

    @Test fun recentBySystemFiltersOrdersAndLimits() = runBlocking {
        dao.insertAll(listOf(
            game("/a.nes", System.NES), game("/b.nes", System.NES), game("/z.sfc", System.SNES),
        ))
        val all = dao.getAll()
        dao.touchLastPlayed(all.first { it.path == "/a.nes" }.id, 1000L)
        dao.touchLastPlayed(all.first { it.path == "/b.nes" }.id, 2000L)
        dao.touchLastPlayed(all.first { it.path == "/z.sfc" }.id, 3000L)

        val nes = dao.observeContinuePlayingBySystem(System.NES.name, 10).first()
        assertEquals(listOf("/b.nes", "/a.nes"), nes.map { it.path })  // SNES excluded, newest first

        val nesLimited = dao.observeContinuePlayingBySystem(System.NES.name, 1).first()
        assertEquals(listOf("/b.nes"), nesLimited.map { it.path })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.gobe.tv.data.db.GameDaoHomeQueriesTest"`
Expected: FAIL — `observeCountsBySystem` / `observeContinuePlayingBySystem` / `SystemCount` unresolved (or, if no device: compilation failure of the test module). If no emulator/device is available, note it and proceed; run this once a device is available.

- [ ] **Step 3: Add the queries + projection to `GameDao.kt`**

Add inside the `GameDao` interface (after `observeContinuePlaying`):

```kotlin
    @Query("SELECT system AS system, COUNT(*) AS count FROM games GROUP BY system")
    fun observeCountsBySystem(): Flow<List<SystemCount>>

    @Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL AND system = :system ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeContinuePlayingBySystem(system: String, limit: Int): Flow<List<GameEntity>>
```

And add this top-level projection class in the same file (below the interface):

```kotlin
/** Room projection for a per-system game count. `system` maps back via the enum type converter. */
data class SystemCount(val system: com.gobe.tv.domain.System, val count: Int)
```

(The `games.system` column is stored via the existing `System` type converter, so Room maps the grouped column straight back to the enum.)

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.gobe.tv.data.db.GameDaoHomeQueriesTest"`
Expected: PASS (with a device). Also run `./gradlew :app:assembleDebug` to confirm Room codegen compiles the new query even without a device.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/db/GameDao.kt app/src/androidTest/java/com/gobe/tv/data/db/GameDaoHomeQueriesTest.kt
git commit -m "feat(data): DAO queries for per-system counts and per-system recents"
```

---

## Task 5: Repository wrappers for the new queries

Thin domain-facing flows mirroring the existing `observeContinuePlaying`. Covered indirectly by the DAO test; no separate test (matches how `observeContinuePlaying`/`observeGames` are untested wrappers).

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/data/LibraryRepository.kt`

- [ ] **Step 1: Add wrappers** (after the existing `observeContinuePlaying`):

```kotlin
    /** Per-system game counts (only systems with ≥1 game appear). */
    fun observeCountsBySystem(): Flow<Map<System, Int>> =
        gameDao.observeCountsBySystem().map { rows -> rows.associate { it.system to it.count } }

    /** Recently played games for one system, newest first. */
    fun observeContinuePlayingBySystem(system: System, limit: Int = 12): Flow<List<Game>> =
        gameDao.observeContinuePlayingBySystem(system.name, limit).map { list -> list.map { it.toDomain() } }
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/data/LibraryRepository.kt
git commit -m "feat(data): repository flows for per-system counts and recents"
```

---

## Task 6: HomeViewModel — carousel state

Rewrite `HomeState` to carry the visible consoles, the focused system, and the focused console's recents. Focus is user-controlled state; when the console list first loads (or the focused console disappears), it falls back to `defaultFocus`. Uses the pure helpers from Task 3. No VM unit test (consistent with the codebase — `HomeViewModel`/`LibraryViewModel` are not unit-tested); correctness rests on the tested pure helpers + DAO queries, then on-device verification.

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.gobe.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val consoles: List<ConsoleEntry> = emptyList(),
    val focusedSystem: System? = null,
    val continueForFocused: List<Game> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    private val scanning = MutableStateFlow(true)

    /** User-chosen focused console; null means "follow defaultFocus". */
    private val focusOverride = MutableStateFlow<System?>(null)

    private val visible: StateFlow<List<ConsoleEntry>> =
        repo.observeCountsBySystem()
            .map { counts -> visibleConsoles(counts) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Effective focus = override if still visible, else the first visible console. */
    private val focused: StateFlow<System?> =
        combine(visible, focusOverride) { v, override ->
            val stillValid = override != null && v.any { it.system == override }
            if (stillValid) override else defaultFocus(v)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val continueForFocused: StateFlow<List<Game>> =
        focused.flatMapLatest { s ->
            if (s == null) flowOf(emptyList()) else repo.observeContinuePlayingBySystem(s)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<HomeState> =
        combine(visible, focused, continueForFocused, scanning) { v, f, cont, scan ->
            HomeState(consoles = v, focusedSystem = f, continueForFocused = cont, loading = scan && v.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    /** Move the focused console left/right (clamped, no wrap). */
    fun move(delta: Int) {
        focusOverride.value = moveFocus(visible.value, focused.value, delta)
    }

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

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: FAILS to compile `HomeScreen.kt` (still references the old `HomeState.continuePlaying`). That's expected — Task 7 rewrites the screen. Do **not** commit yet; proceed to Task 7 and commit them together.

---

## Task 7: HomeScreen — the carousel UI

Rewrite `HomeScreen` as the full-screen carousel: persistent top bar (logo + search + settings, reusing the existing `SearchField`), a color-tinted stage, the `ConsoleHero`, a console-name dot row, left/right chevrons, and the contextual Continue strip. D-pad Left/Right change the focused console; center/OK opens that console's Library; Down moves to the Continue strip when present; L1/R1 keep their Search/Settings shortcuts. The `ConsoleHero` composable (new file) renders the controller photo on a light "display plate" with an accent glow, applying `BlendMode.Multiply` for photos so the white background merges into the plate (Arcade's transparent vector draws straight).

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/home/ConsoleHero.kt`
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt`

- [ ] **Step 1: Create `ConsoleHero.kt`**

```kotlin
package com.gobe.tv.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val PlateLight = Color(0xFFF6F7F9)
private val PlateDark = Color(0xFFE6E8EE)

/** Controller art on a light "display plate" with an accent glow. Photos are multiply-blended so
 *  their white background disappears into the plate; the Arcade vector draws straight. */
@Composable
fun ConsoleHero(art: ConsoleArt, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(PlateLight, PlateDark)))
                .border(3.dp, art.accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (art.isPhoto) {
                val bmp: ImageBitmap = ImageBitmap.imageResource(art.art)
                Canvas(Modifier.fillMaxSize().padding(28.dp)) {
                    val scale = min(size.width / bmp.width, size.height / bmp.height)
                    val w = (bmp.width * scale); val h = (bmp.height * scale)
                    val left = ((size.width - w) / 2f); val top = ((size.height - h) / 2f)
                    // Fill the plate area then multiply the photo over it (white -> plate).
                    drawRect(PlateLight)
                    drawImage(
                        image = bmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bmp.width, bmp.height),
                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                        dstSize = IntSize(w.toInt(), h.toInt()),
                        blendMode = BlendMode.Multiply,
                    )
                }
            } else {
                Image(
                    painter = painterResource(art.art),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(0.7f),
                )
            }
        }
    }
}
```

> Note for the implementer: the glow here is a colored border; if it reads weak on-device, add an outer radial-gradient halo Box behind the plate (accent → transparent) — a visual tweak, not a logic change. Verify plate rendering on-device before polishing.

- [ ] **Step 2: Rewrite `HomeScreen.kt`**

Replace the whole file with:

```kotlin
package com.gobe.tv.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
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
import com.gobe.tv.ui.theme.GobeBg

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
    val searchFocus = remember { FocusRequester() }
    val heroFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val focused = state.focusedSystem
    val accent = if (focused != null) consoleArt(focused).accent else MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        // Tint the stage toward the focused console's accent.
        lerpToward(GobeBg, accent, 0.22f), label = "stageBg",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg, GobeBg)))
            .padding(40.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                keyToHomeAction(event.key.nativeKeyCode)?.let { action ->
                    when (action) {
                        HomeKeyAction.Search -> { runCatching { searchFocus.requestFocus() }; keyboard?.show() }
                        HomeKeyAction.Settings -> onOpenSettings()
                    }
                    return@onPreviewKeyEvent true
                }
                false
            },
    ) {
        // --- Top bar ---
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.gobe_logo), "Gobe", modifier = Modifier.height(56.dp))
            Spacer(Modifier.width(24.dp))
            SearchField(
                value = query, onValueChange = { query = it },
                onSubmit = { if (query.isNotBlank()) onOpenSection(LibrarySection.SearchAll(query.trim())) },
                focusRequester = searchFocus, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(24.dp))
            Button(onClick = onOpenSettings) { Text("⚙ " + stringResource(R.string.home_settings)) }
        }
        Spacer(Modifier.height(24.dp))

        // --- Stage ---
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> Text(
                    stringResource(R.string.home_scanning),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
                focused == null -> EmptyLibrary(onOpenSettings = onOpenSettings, modifier = Modifier.align(Alignment.Center))
                else -> CarouselStage(
                    consoles = state.consoles,
                    focused = focused,
                    accent = accent,
                    continueForFocused = state.continueForFocused,
                    heroFocus = heroFocus,
                    onMove = { delta -> vm.move(delta) },
                    onOpenConsole = { onOpenSection(LibrarySection.Console(focused)) },
                    onOpenGame = onOpenGame,
                    onFocusUp = { runCatching { searchFocus.requestFocus() } },
                )
            }
        }
        HomeControlLegend()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CarouselStage(
    consoles: List<ConsoleEntry>,
    focused: System,
    accent: Color,
    continueForFocused: List<com.gobe.tv.domain.Game>,
    heroFocus: FocusRequester,
    onMove: (Int) -> Unit,
    onOpenConsole: () -> Unit,
    onOpenGame: (Long) -> Unit,
    onFocusUp: () -> Unit,
) {
    val count = consoles.firstOrNull { it.system == focused }?.count ?: 0
    val hasContinue = continueForFocused.isNotEmpty()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { runCatching { heroFocus.requestFocus() } }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Hero + chevrons
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("‹", style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.align(Alignment.CenterStart))
            Text("›", style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.align(Alignment.CenterEnd))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    onClick = onOpenConsole,
                    modifier = Modifier
                        .focusRequester(heroFocus)
                        .fillMaxWidth(0.42f)
                        .aspectRatio(1.4f)
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.DirectionLeft -> { onMove(-1); true }
                                Key.DirectionRight -> { onMove(+1); true }
                                Key.DirectionUp -> { onFocusUp(); true }
                                Key.DirectionDown -> {
                                    if (hasContinue) { focusManager.moveFocus(FocusDirection.Down); true } else true
                                }
                                else -> false
                            }
                        },
                    colors = CardDefaults.colors(containerColor = Color.Transparent),
                ) {
                    ConsoleHero(consoleArt(focused), Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(20.dp))
                Text(focused.displayName, fontSize = 44.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("$count " + stringResource(R.string.home_games_count),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
                Spacer(Modifier.height(16.dp))
                ConsoleDots(consoles = consoles, focused = focused, accent = accent)
            }
        }

        // Contextual Continue strip
        if (hasContinue) {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_continue_playing),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(continueForFocused, key = { it.id }) { g ->
                        GameTile(game = g, onClick = { onOpenGame(g.id) }, requestInitialFocus = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleDots(consoles: List<ConsoleEntry>, focused: System, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        consoles.forEach { entry ->
            val on = entry.system == focused
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) accent else MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    entry.system.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_no_games), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.home_add_roms)) }
    }
}

/** Linear blend of two colors in sRGB; small helper for the stage tint. */
private fun lerpToward(from: Color, to: Color, t: Float): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = 1f,
)

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

- [ ] **Step 3: Add the new string resources**

In `app/src/main/res/values/strings.xml` add:

```xml
<string name="home_games_count">games</string>
<string name="home_no_games">No games yet</string>
<string name="home_add_roms">Add ROM folders</string>
```

And in `app/src/main/res/values-es/strings.xml` add:

```xml
<string name="home_games_count">juegos</string>
<string name="home_no_games">Todavía no hay juegos</string>
<string name="home_add_roms">Agregar carpetas de ROMs</string>
```

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (Fix any missing imports the compiler flags; the `home_consoles` string is now unused — that's fine, leave it.)

- [ ] **Step 5: Unit tests still green**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (existing tests unaffected).

- [ ] **Step 6: Commit (Task 6 + 7 together)**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeViewModel.kt \
        app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt \
        app/src/main/java/com/gobe/tv/ui/home/ConsoleHero.kt \
        app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(home): cinematic console carousel home screen"
```

- [ ] **Step 7: On-device verification (Android TV / ONN)**

Install and drive it (use the project's existing on-device flow / `/run` or `/verify` skill if set up):

```bash
./gradlew :app:installDebug
```

Verify manually:
- Only consoles with games appear; each shows its controller photo on a plate + "N games".
- D-pad Left/Right changes the focused console and the whole background re-tints; clamps at NES (left) and the last console (right) with no wrap.
- The dot row highlights the focused console.
- A/OK on the hero opens that console's Library.
- The Continue strip shows only the focused console's recents and disappears for consoles with none; A on a tile opens Detail.
- Up returns to the search field; L1 focuses search; R1 opens Settings.
- Recommended/Favorites tiles are gone from home.

---

## Task 8: Library collection filter (Favorites / Recommended per console)

Replace the deleted home tiles' capability: a filter control inside each console's Library. Add a pure `CollectionFilter` → flags map (tested), make `LibraryViewModel` drive `recommendedOnly`/`favoritesOnly` reactively, and add filter chips to `LibraryScreen`.

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/library/CollectionFilter.kt`
- Test: `app/src/test/java/com/gobe/tv/ui/library/CollectionFilterTest.kt`
- Modify: `LibraryViewModel.kt`, `LibraryScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionFilterTest {
    @Test fun allClearsBothFlags() =
        assertEquals(CollectionFlags(false, false), collectionFlags(CollectionFilter.ALL))
    @Test fun recommendedSetsOnlyRecommended() =
        assertEquals(CollectionFlags(true, false), collectionFlags(CollectionFilter.RECOMMENDED))
    @Test fun favoritesSetsOnlyFavorites() =
        assertEquals(CollectionFlags(false, true), collectionFlags(CollectionFilter.FAVORITES))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.library.CollectionFilterTest"`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement `CollectionFilter.kt`**

```kotlin
package com.gobe.tv.ui.library

/** In-library collection filter, replacing the old top-level Recommended/Favorites sections. */
enum class CollectionFilter { ALL, RECOMMENDED, FAVORITES }

/** DB flags a [CollectionFilter] maps to. */
data class CollectionFlags(val recommendedOnly: Boolean, val favoritesOnly: Boolean)

fun collectionFlags(filter: CollectionFilter): CollectionFlags = when (filter) {
    CollectionFilter.ALL -> CollectionFlags(false, false)
    CollectionFilter.RECOMMENDED -> CollectionFlags(true, false)
    CollectionFilter.FAVORITES -> CollectionFlags(false, true)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.library.CollectionFilterTest"`
Expected: PASS.

- [ ] **Step 5: Wire the filter into `LibraryViewModel.kt`**

Add the filter state and fold it into `games` (replace the `_genre`/`_sort` combine):

```kotlin
    private val _filter = MutableStateFlow(CollectionFilter.ALL)
    val collectionFilter: StateFlow<CollectionFilter> = _filter
    fun setFilter(f: CollectionFilter) { _filter.value = f }

    // Genre + sort + collection filter are reactive; system/query stay fixed by the section.
    val games: StateFlow<List<Game>> =
        combine(_genre, _sort, _filter) { g, s, f -> Triple(g, s, f) }
            .flatMapLatest { (g, s, f) ->
                val flags = collectionFlags(f)
                repo.searchGames(
                    base.query, base.system, g,
                    base.recommendedOnly || flags.recommendedOnly,
                    base.favoritesOnly || flags.favoritesOnly, s,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

- [ ] **Step 6: Add filter chips to `LibraryScreen.kt`**

Collect the filter and render a chip row. In the composable body (after `val sortMode by ...`):

```kotlin
    val collectionFilter by vm.collectionFilter.collectAsState()
```

Add a chip row inside the top-bar `Row`, before the sort `Button` (so it sits with the sort control):

```kotlin
            FilterChip(stringResource(R.string.filter_all), collectionFilter == CollectionFilter.ALL) { vm.setFilter(CollectionFilter.ALL) }
            Spacer(Modifier.width(8.dp))
            FilterChip("★ " + stringResource(R.string.section_recommended), collectionFilter == CollectionFilter.RECOMMENDED) {
                vm.setFilter(if (collectionFilter == CollectionFilter.RECOMMENDED) CollectionFilter.ALL else CollectionFilter.RECOMMENDED)
            }
            Spacer(Modifier.width(8.dp))
            FilterChip("♥ " + stringResource(R.string.section_favorites), collectionFilter == CollectionFilter.FAVORITES) {
                vm.setFilter(if (collectionFilter == CollectionFilter.FAVORITES) CollectionFilter.ALL else CollectionFilter.FAVORITES)
            }
            Spacer(Modifier.width(12.dp))
```

And add this small helper composable at file scope:

```kotlin
@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) { Text((if (selected) "● " else "") + label) }
}
```

(The `section_recommended` / `section_favorites` string resources already exist and are reused here.)

- [ ] **Step 7: Build + unit tests**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/library/CollectionFilter.kt \
        app/src/test/java/com/gobe/tv/ui/library/CollectionFilterTest.kt \
        app/src/main/java/com/gobe/tv/ui/library/LibraryViewModel.kt \
        app/src/main/java/com/gobe/tv/ui/library/LibraryScreen.kt
git commit -m "feat(library): per-console Favorites/Recommended filter"
```

---

## Task 9: Remove orphaned Recommended/Favorites sections

With the home tiles gone (Task 7) and the capability moved into the Library filter (Task 8), the global `LibrarySection.Recommended` / `Favorites` variants have no remaining route to them. Remove them and their branches so no dead entry points linger.

**Files:**
- Modify: `LibrarySection.kt`, `SectionVisuals.kt`, `LibraryScreen.kt` (sectionTitle), `SectionVisualsTest.kt`, `SectionFilterTest.kt`

- [ ] **Step 1: Find all references**

Run: `grep -rn "LibrarySection.Recommended\|LibrarySection.Favorites\|Recommended ->\|Favorites ->" app/src`
Expected: references in `LibrarySection.kt`, `SectionVisuals.kt`, `LibraryScreen.kt` (`sectionTitle`), and the two test files. (HomeScreen no longer references them after Task 7.)

- [ ] **Step 2: Remove the variants in `LibrarySection.kt`**

Delete the two `data object` lines and their `sectionFilter` branches:

```kotlin
sealed interface LibrarySection {
    data class Console(val system: System) : LibrarySection
    data class SearchAll(val query: String) : LibrarySection
}
```

```kotlin
fun sectionFilter(section: LibrarySection): SectionFilter = when (section) {
    is LibrarySection.Console -> SectionFilter("", section.system, false, false)
    is LibrarySection.SearchAll -> SectionFilter(section.query, null, false, false)
}
```

- [ ] **Step 3: Remove the branches in `SectionVisuals.kt` and `sectionTitle`**

In `SectionVisuals.kt`, delete the `Recommended`/`Favorites` branches from `sectionVisual`. In `LibraryScreen.kt`'s `sectionTitle`, delete the `Recommended`/`Favorites` branches. Both `when`s stay total over the remaining `Console` + `SearchAll`.

- [ ] **Step 4: Update the tests**

In `SectionVisualsTest.kt`, delete the `recommendedAndFavoritesMapToStarAndHeart` test and any `LibrarySection.Favorites` reference (adjust `nesAccentDiffersFromFavoritesRed` to compare NES vs SNES instead). In `SectionFilterTest.kt`, remove assertions referencing the deleted variants.

- [ ] **Step 5: Build + unit tests**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests PASS. If the compiler flags a remaining reference, remove it (it was a dead path).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(library): remove orphaned Recommended/Favorites sections"
```

---

## Task 10: Final verification

- [ ] **Step 1: Full unit-test run**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Instrumented tests (if a device/emulator is available)**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS (includes `GameDaoHomeQueriesTest`). If no device, record that it was deferred.

- [ ] **Step 3: On-device smoke test**

Re-run the Task 7 Step 7 checklist end-to-end on the ONN / Android TV device, plus:
- In a console Library, the ★ Recommended and ♥ Favorites chips filter the list; toggling back to All restores it.

- [ ] **Step 4: Update memory / release notes**

Per the project's release habit, note the home redesign for the next beta (e.g. a `Gobe home redesign` memory and/or CHANGELOG entry). Not a code step — a reminder.

---

## Notes for the implementer

- **Focus on TV is finicky.** The carousel intercepts D-pad Left/Right in the hero Card's `onPreviewKeyEvent` and consumes them so Compose's focus search doesn't move focus off the hero. If Left/Right ever "escapes" the hero on-device, verify the handler returns `true` for those keys. Down is only consumed-then-delegated when a Continue strip exists.
- **Blend rendering** (`ConsoleHero`): if `BlendMode.Multiply` doesn't render as expected on the device's GPU, the fallback is a near-white plate with a plain `Image` (the photos are on near-white backgrounds). Confirm on-device before investing in the glow polish.
- **YAGNI:** no carousel wrap-around, no transparent-float art finish, no global cross-console Favorites view — all recorded as follow-ups in the spec, intentionally out of scope.
