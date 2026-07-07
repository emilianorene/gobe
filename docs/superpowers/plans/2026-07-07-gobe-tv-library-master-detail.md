# TV Library Master-Detail — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework `LibraryScreen` (level-2 library + search results) from a cover-tile grid into a master–detail layout: a vertical poster rail on the left and a live detail panel on the right that updates as focus moves.

**Architecture:** One screen changes (`LibraryScreen`), which already serves all four `LibrarySection`s. The detail panel is extracted as a shared `GameDetailPanel` reused by the existing full-screen `DetailScreen`. Cover rendering is unified in a shared `GameCover` composable. Two pure helpers (`LibraryKeyMap`, `rowSubtitle`) are TDD-tested in JVM; composables are verified by build + on-device.

**Tech Stack:** Kotlin, Jetpack Compose, `androidx.tv.material3`, Coil (`SubcomposeAsyncImage`), JUnit4 (pure JVM unit tests). Build/test: `./gradlew`.

**Spec:** `docs/superpowers/specs/2026-07-07-gobe-tv-library-master-detail-design.md`

---

## Pre-flight

- [ ] **Create a feature branch** (currently on `main`)

```bash
git checkout -b feat/tv-library-master-detail
```

## File Structure

- **Create** `app/src/main/java/com/gobe/tv/ui/library/LibraryKeyMap.kt` — pure L1/R1 → page action.
- **Create** `app/src/test/java/com/gobe/tv/ui/library/LibraryKeyMapTest.kt` — test for the above.
- **Create** `app/src/main/java/com/gobe/tv/ui/library/GameRow.kt` — rail row composable + pure `rowSubtitle` helper.
- **Create** `app/src/test/java/com/gobe/tv/ui/library/RowSubtitleTest.kt` — test for `rowSubtitle`.
- **Create** `app/src/main/java/com/gobe/tv/ui/art/GameCover.kt` — shared cover composable (url + branded fallback).
- **Create** `app/src/main/java/com/gobe/tv/ui/detail/GameDetailPanel.kt` — shared detail panel (meta + actions).
- **Modify** `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt` — use shared `GameCover`.
- **Modify** `app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt` — compose `GameDetailPanel`.
- **Modify** `app/src/main/java/com/gobe/tv/ui/library/LibraryScreen.kt` — rewrite to master-detail.
- **Modify** `app/src/main/java/com/gobe/tv/ui/GobeNavHost.kt` — drop now-unused `onOpenGame` from Library.
- **Modify** `app/src/main/res/values/strings.xml` (+ locale variants) — library legend + page-jump strings.

---

## Task 1: LibraryKeyMap (pure, TDD)

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/library/LibraryKeyMap.kt`
- Test: `app/src/test/java/com/gobe/tv/ui/library/LibraryKeyMapTest.kt`

Mirrors `ui/home/HomeKeyMap.kt`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.ui.library

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryKeyMapTest {
    @Test fun l1MapsToPagePrev() =
        assertEquals(LibraryKeyAction.PagePrev, keyToLibraryAction(KeyEvent.KEYCODE_BUTTON_L1))

    @Test fun r1MapsToPageNext() =
        assertEquals(LibraryKeyAction.PageNext, keyToLibraryAction(KeyEvent.KEYCODE_BUTTON_R1))

    @Test fun otherKeysMapToNull() {
        assertNull(keyToLibraryAction(KeyEvent.KEYCODE_BUTTON_A))
        assertNull(keyToLibraryAction(KeyEvent.KEYCODE_DPAD_DOWN))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.gobe.tv.ui.library.LibraryKeyMapTest"`
Expected: FAIL (unresolved reference `LibraryKeyAction` / `keyToLibraryAction`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.gobe.tv.ui.library

import android.view.KeyEvent

/** Library gamepad shortcut actions (page-jump within the poster rail). */
enum class LibraryKeyAction { PagePrev, PageNext }

/** Maps a gamepad key code to a library page-jump action (L1→prev, R1→next), or null. Pure.
 *  Note: inside the library, L1/R1 are repurposed from their Home meaning (Search/Settings). */
fun keyToLibraryAction(keyCode: Int): LibraryKeyAction? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L1 -> LibraryKeyAction.PagePrev
    KeyEvent.KEYCODE_BUTTON_R1 -> LibraryKeyAction.PageNext
    else -> null
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.gobe.tv.ui.library.LibraryKeyMapTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/library/LibraryKeyMap.kt app/src/test/java/com/gobe/tv/ui/library/LibraryKeyMapTest.kt
git commit -m "feat(library): LibraryKeyMap — L1/R1 page-jump (pure)"
```

---

## Task 2: rowSubtitle helper (pure, TDD)

The rail row shows a `year · genre` subtitle. Extract the formatting as a pure, testable function
(mirrors `tileCaption` in `GameTile.kt`). It lives in `GameRow.kt` (created here; the composable is
added in Task 5).

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/library/GameRow.kt` (helper only for now)
- Test: `app/src/test/java/com/gobe/tv/ui/library/RowSubtitleTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class RowSubtitleTest {
    @Test fun yearAndGenre() =
        assertEquals("1988 · Platformer", rowSubtitle(1988, "Platformer"))

    @Test fun yearOnly() =
        assertEquals("1988", rowSubtitle(1988, null))

    @Test fun genreOnly() =
        assertEquals("Platformer", rowSubtitle(null, "Platformer"))

    @Test fun neither() =
        assertEquals("", rowSubtitle(null, null))

    @Test fun nonPositiveYearOmitted() =
        assertEquals("Action", rowSubtitle(0, "Action"))

    @Test fun blankGenreOmitted() =
        assertEquals("1990", rowSubtitle(1990, "  "))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.gobe.tv.ui.library.RowSubtitleTest"`
Expected: FAIL (unresolved reference `rowSubtitle`).

- [ ] **Step 3: Write minimal implementation** (create `GameRow.kt` with just the helper)

```kotlin
package com.gobe.tv.ui.library

/** Rail-row subtitle: "year · genre", omitting each part when absent. Pure for unit testing. */
fun rowSubtitle(year: Int?, genre: String?): String {
    val y = if (year != null && year > 0) year.toString() else null
    val g = genre?.trim()?.takeIf { it.isNotBlank() }
    return listOfNotNull(y, g).joinToString(" · ")
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.gobe.tv.ui.library.RowSubtitleTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/library/GameRow.kt app/src/test/java/com/gobe/tv/ui/library/RowSubtitleTest.kt
git commit -m "feat(library): rowSubtitle helper — 'year · genre' (pure)"
```

---

## Task 3: Shared GameCover composable

`GameTile.DefaultCover` (branded fallback) and `DetailScreen.CoverArt` (Coil load) are both `private`
and duplicated. Extract one shared composable that loads `coverUrl(game)` and falls back to the
branded default, then refactor `GameTile` to use it. (No unit test — Compose UI; verified by build.)

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/art/GameCover.kt`
- Modify: `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt` (replace inner cover with `GameCover`)

- [ ] **Step 1: Create `GameCover.kt`**

Move the branded fallback (the current `DefaultCover` in `GameTile.kt`, lines ~150-190) here as a
private `DefaultCover`, and expose a public `GameCover(game, modifier, contentScale)` that mirrors
`GameTile`'s Coil block: `SubcomposeAsyncImage(model = coverUrl(game), contentScale = …)` showing the
image on `Success` and `DefaultCover(game)` otherwise (and when `coverUrl` is null). Default
`contentScale = ContentScale.Fit`. Keep the caller responsible for size/clip/background via `modifier`.

```kotlin
package com.gobe.tv.ui.art

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.gobe.tv.R
import com.gobe.tv.data.art.coverUrl
import com.gobe.tv.domain.Game

/** Cover art for a game: loads coverUrl(), falling back to a branded placeholder. Fills its box. */
@Composable
fun GameCover(game: Game, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val url = coverUrl(game)
    Box(modifier) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = game.displayName,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent()
                else DefaultCover(game)
            }
        } else {
            DefaultCover(game)
        }
    }
}

/** Branded placeholder cover for games without box art (Gobe logo watermark + title/system). */
@Composable
private fun DefaultCover(game: Game) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF232A3D), Color(0xFF0E1119)))),
    ) {
        Image(
            painter = painterResource(R.drawable.gobe_logo),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.62f).alpha(0.16f),
        )
        Text(
            game.system.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        Text(
            game.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
        )
    }
}
```
(Add the missing `androidx.compose.ui.unit.dp` import.)

- [ ] **Step 2: Refactor `GameTile.kt` to use `GameCover`**

Inside the `Card`'s `Box`, replace the inline `SubcomposeAsyncImage`/`DefaultCover` block with
`GameCover(game, Modifier.fillMaxSize())`. Keep the badge overlays (`👥`, `★`, `♥`) as they are.
Delete the now-duplicated `private fun DefaultCover` from `GameTile.kt`. Keep `tileCaption` untouched.

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/art/GameCover.kt app/src/main/java/com/gobe/tv/ui/home/GameTile.kt
git commit -m "refactor(ui): shared GameCover composable (unify tile/detail cover art)"
```

---

## Task 4: Shared GameDetailPanel composable

Extract the right-hand content of `DetailScreen` (name, meta, chips, description, Play/Resume/Favorite)
into a shared, presentational `GameDetailPanel` driven by callbacks. Then refactor `DetailScreen` to
compose it (behavior preserved). `LibraryScreen` will reuse it in Task 6.

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/detail/GameDetailPanel.kt`
- Modify: `app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt`

- [ ] **Step 1: Create `GameDetailPanel.kt`**

Signature — presentational, no launch/DB logic inside:

```kotlin
@Composable
fun GameDetailPanel(
    game: Game,
    playable: Boolean,
    hasState: Boolean,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    favorite: Boolean,
    playFocusRequester: FocusRequester? = null,   // caller may request initial focus on Play
    modifier: Modifier = Modifier,
)
```

Body: a `Row` — left `GameCover(game, Modifier.width(220.dp).height(300.dp).clip(RoundedCornerShape(12.dp)))`,
right a `Column` with `game.displayName`, system/year/players/genre meta, `★ recommended` line when
`game.recommended`, size + `💾 save-state` note when `hasState`, description (`maxLines = 8`, ellipsis)
when non-blank, then the action buttons: Play (via `playFocusRequester` when provided; disabled →
`detail_play_soon` when `!playable`), Resume (`detail_resume_save`, only when `hasState`), Favorite
(`♥/♡` + `detail_favorite`/`detail_unfavorite`). Port the exact markup/strings from the current
`DetailScreen` lines ~99-171.

- [ ] **Step 2: Refactor `DetailScreen.kt` to use it**

Keep all state/logic in `DetailScreen` (loading `game`, `favorite` mirror, `hasState`, `playable`,
`launch()`, `ON_RESUME` refresh, focus). Replace the inline right-hand `Column` with
`GameDetailPanel(game = g, playable = playable, hasState = hasState, onPlay = { launch(false) },
onResume = { launch(true) }, onToggleFavorite = { … existing toggle … }, favorite = favorite,
playFocusRequester = playFocus)`. Remove the now-unused private `CoverArt` (replaced by `GameCover`
inside the panel). Keep `BackHandler`, the loading text, and the Back button (Back can stay in
`DetailScreen` around the panel, or be included — keep DetailScreen's own Back button for its
full-screen use).

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/detail/GameDetailPanel.kt app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt
git commit -m "refactor(detail): extract shared GameDetailPanel from DetailScreen"
```

---

## Task 5: GameRow composable

Add the rail-row composable to `GameRow.kt` (which already holds `rowSubtitle`). A focusable `Card`/row:
medium cover + name + `rowSubtitle(year, genre)` + ★/♥ badge, with an orange (`GobeAccentNes`) selected
highlight. Reports focus via `onFocused` and launches via `onClick`.

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/library/GameRow.kt`

- [ ] **Step 1: Add the composable**

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GameRow(
    game: Game,
    onClick: () -> Unit,          // A = launch directly
    onFocused: () -> Unit,        // focus moved here → parent updates the panel
    requestInitialFocus: Boolean = false,
    modifier: Modifier = Modifier,
)
```

- Use a `Card(onClick = onClick, …)` sized `fillMaxWidth()`, with `.onFocusChanged { if (it.isFocused) onFocused() }`.
- Left: `GameCover(game, Modifier.width(46.dp).height(62.dp).clip(RoundedCornerShape(6.dp)))`.
- Middle (`weight(1f)`): `game.displayName` (single line, ellipsis) + `rowSubtitle(game.year, game.genre)` (labelSmall, muted). Omit the subtitle Text when the string is blank.
- Right: badge — `★` (`Color(0xFFFFD54F)`) when `game.recommended`, else `♥` (`GobeAccentFavorites`) when `game.favorite`, else nothing.
- Handle `requestInitialFocus` with a `FocusRequester` exactly like `GameTile` does (own the requester, `LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }`).

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/library/GameRow.kt
git commit -m "feat(library): GameRow — poster rail row (cover + name + subtitle + badge)"
```

---

## Task 6: Rewrite LibraryScreen to master-detail

Replace the `LazyVerticalGrid` with a two-pane `Row`: a `LazyColumn` poster rail of `GameRow` + a
`GameDetailPanel`. `LibraryScreen` owns the focused-game state, the `launch()` function, save-state /
favorite state for the focused game, the L1/R1 page-jump handler, and the per-screen legend.

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/gobe/tv/ui/GobeNavHost.kt`
- Modify: `app/src/main/res/values/strings.xml` (+ each `values-<locale>/strings.xml`)

- [ ] **Step 1: Add string resources**

In `app/src/main/res/values/strings.xml` add:

```xml
<string name="legend_play">Play</string>
<string name="legend_page">Page</string>
<string name="legend_actions">Actions</string>
```

Add matching translations in every `values-*/strings.xml` that exists (mirror the locales already
present for `legend_*`). Verify with: `ls app/src/main/res/values-*/strings.xml`.

- [ ] **Step 2: Rewrite `LibraryScreen`**

Structure:
- Keep the VM wiring (`vm.games`, `genres`, `selectedGenre`, `sortMode`, `cycleSort`, `setGenre`).
- `val games by vm.games.collectAsState()`; hold `var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }`.
- Derive `val focused = games.firstOrNull { it.id == focusedId } ?: games.firstOrNull()`. When `games`
  changes and `focusedId` isn't in the list, reset it to the first game (`LaunchedEffect(games) { if (games.none { it.id == focusedId }) focusedId = games.firstOrNull()?.id }`).
- Focused-game side state (mirror `DetailScreen`): `hasState` via `SaveStateStore(context.filesDir).hasState(id)` recomputed on `focused` change and on `ON_RESUME`; `favorite` local mirror from `focused?.favorite`; `playable` via `CoreManager(...).corePath(system) != null`.
- `fun launch(loadState: Boolean)` — identical to `DetailScreen.launch`, using `focused`.
- Layout: `Column(padding(40.dp))` →
  - top bar `Row`: section title + `"· ${games.size}"` + Sort button (`vm.cycleSort()`, existing label) + genre chips `LazyRow` (existing logic).
  - `Row(weight(1f))`: LEFT `LazyColumn(Modifier.weight(0.4f), state = listState)` of `GameRow(game, onClick = { focusedId = game.id; launch(false) }, onFocused = { focusedId = game.id }, requestInitialFocus = index == 0)`; RIGHT `GameDetailPanel(game = focused, …, onPlay = { launch(false) }, onResume = { launch(true) }, onToggleFavorite = { toggle }, favorite = favorite, modifier = Modifier.weight(0.6f))` — render an empty-state Text when `focused == null`.
  - bottom: `LibraryControlLegend()` → `Ⓐ {legend_play} · Ⓑ {legend_back} · ▶ {legend_actions} · L1/R1 {legend_page}` via `stringResource`.
- `onPreviewKeyEvent` on the outer `Column`: on `KeyDown`, `keyToLibraryAction(event.key.nativeKeyCode)` → `PagePrev`/`PageNext` → `scope.launch { listState.scrollToItem((listState.firstVisibleItemIndex ± visibleCount).coerceIn(0, games.lastIndex)) }` where `visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)`. Return true only when an action matched.
- Keep `BackHandler { onBack() }`.
- Keep the `LibrarySection` → title mapping (`sectionTitle`) and `sortLabel`.

- [ ] **Step 3: Update `GobeNavHost.kt`**

`LibraryScreen` no longer navigates to Detail (the panel is inline). Drop the `onOpenGame` parameter
from `LibraryScreen`'s signature and remove `onOpenGame = { push(Route.Detail(it)) }` from the
`Route.Library` branch. Leave Home's `onOpenGame → Route.Detail` (continue-playing) intact.

- [ ] **Step 4: Build to verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run the full unit-test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (including the two new pure tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/library/LibraryScreen.kt app/src/main/java/com/gobe/tv/ui/GobeNavHost.kt app/src/main/res/values*/strings.xml
git commit -m "feat(library): master-detail layout (poster rail + live detail panel)"
```

---

## Task 7: Full build + on-device verification

- [ ] **Step 1: Assemble the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: On-device manual verification (ONN) — ASK THE USER FIRST before installing.**

Install and drive the flow on the ONN (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
Verify against the spec's acceptance:
- Open a console (e.g. NES) → poster rail on the left, detail panel on the right showing the first game.
- Moving focus Up/Down updates the panel live (0 presses to see info).
- **A** on a row launches the game directly.
- **Right** enters the panel; Play/Resume/Favorite reachable; **Left** returns to the rail.
- **L1/R1** page-jump the rail.
- ★/♥ badges show on rows; a game with a save state shows **Resume**; favorite toggle updates the row's ♥.
- Search from Home renders results in the same master-detail layout.
- Empty section/search shows the empty state without crashing.

- [ ] **Step 3: Finish the branch** — use `superpowers:finishing-a-development-branch` to decide merge/PR.
  (Repo push requires the `emilianorene` gh account — `gh auth switch --user emilianorene`.)

---

## Notes for the implementer

- **TDD applies to the pure helpers** (Tasks 1–2). Compose UI is verified by compile + on-device, per
  the spec — there is no Robolectric/Compose-test harness in this project (tests are pure JVM JUnit4).
- **Follow existing patterns**: focus handling mirrors `GameTile`/`SectionTile`; key handling mirrors
  `HomeScreen`'s `onPreviewKeyEvent`; the panel markup is a straight port of `DetailScreen`.
- **DRY**: after Task 3 there is exactly one branded-cover implementation; after Task 4 exactly one
  detail-panel implementation.
- **Do not touch Home** (console Hero tiles), the in-game pause overlay, or `LibraryViewModel` logic.
```
