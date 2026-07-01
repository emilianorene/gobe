# Home Grid Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework the Home grid into square, captioned tiles in a single scroll so more games are visible, nothing is pinned/cut, and every tile shows `Name (Year)`.

**Architecture:** `GameTile` becomes a `Column` = square cover (`ContentScale.Fit`) + a caption line derived by a pure, unit-tested `tileCaption(name, year)` helper. `HomeScreen` collapses the pinned "Continue playing" row and the separate game grid into ONE `LazyVerticalGrid`, where "Continue playing" is a full-span header item (`GridItemSpan(maxLineSpan)`) that scrolls with the grid.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv:tv-material3`), `LazyVerticalGrid`/`LazyRow`, Coil (`SubcomposeAsyncImage`), JUnit (JVM) + on-device verification over wireless adb.

**Spec:** `docs/superpowers/specs/2026-06-30-gobe-home-grid-redesign-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch `feat/home-grid-redesign` already exists (spec committed there). Stay on it.
- **Environment gotchas** (subagents MUST know): `java`/`adb` are NOT on PATH.
  - JDK: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"` before any `./gradlew`.
  - adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Device: `192.168.1.219:5555` (ONN, `armeabi-v7a`). Wake with `adb shell input keyevent KEYCODE_WAKEUP` before screenshots. Re-grant `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow` after any reinstall.
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## File map

- `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt` — **Modify**: add pure `tileCaption(name, year)`; restructure the composable to square cover + caption `Column`; keep players badge + `DefaultCover` adapted to square.
- `app/src/test/java/com/gobe/tv/ui/home/TileCaptionTest.kt` — **Create**: JVM unit test for `tileCaption`.
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — **Modify**: unify continue-row + grid into one `LazyVerticalGrid` with a full-span "Continue playing" header; preserve focus (UP→Settings, initial focus).

Reference (current `Game` fields used): `Game(id, displayName, system, players, genre, year, boxartName, path, sizeBytes, …)`; `year: Int?`, `players: Int?`.

---

### Task 1: `tileCaption` pure helper (TDD)

**Files:**
- Test: `app/src/test/java/com/gobe/tv/ui/home/TileCaptionTest.kt` (create)
- Modify: `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/gobe/tv/ui/home/TileCaptionTest.kt`:
```kotlin
package com.gobe.tv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TileCaptionTest {
    @Test fun nameWithYear() =
        assertEquals("Super Mario World (1991)", tileCaption("Super Mario World", 1991))

    @Test fun nameWithoutYear() =
        assertEquals("Super Mario World", tileCaption("Super Mario World", null))

    @Test fun nonPositiveYearIsOmitted() =
        assertEquals("Final Fight", tileCaption("Final Fight", 0))
}
```

- [ ] **Step 2: Run the test, verify it fails**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.TileCaptionTest"
```
Expected: FAIL — `tileCaption` is unresolved / does not exist.

- [ ] **Step 3: Add the helper in `GameTile.kt`**

At the top level of `GameTile.kt` (e.g. just under the `private val boxartUrlBuilder = BoxartUrlBuilder()` line), add:
```kotlin
/** Tile caption: game name, plus " (year)" when a real year is known. Pure for unit testing. */
fun tileCaption(name: String, year: Int?): String =
    if (year != null && year > 0) "$name ($year)" else name
```

- [ ] **Step 4: Run the test, verify it passes**

Same command as Step 2. Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/GameTile.kt app/src/test/java/com/gobe/tv/ui/home/TileCaptionTest.kt
git commit -m "feat(home): tileCaption helper — 'Name (Year)' with year optional"
```

---

### Task 2: Square `GameTile` with caption

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/GameTile.kt`

No JVM-unit-testable surface here (Compose UI); verified on-device in Task 4. Keep the existing
`tileCaption` from Task 1, the players-badge logic, and `DefaultCover`.

- [ ] **Step 1: Restructure the `GameTile` composable**

Replace the current `Card(...width(140.dp).height(190.dp)) { Box { cover + badge } }` body with a
`Column` of the cover Card (square) + caption. Concretely, change the composable body so it reads:

```kotlin
    Column(
        modifier = modifier.then(focusModifier).width(132.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f), // square cover
            colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (url != null) {
                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = game.displayName,
                        contentScale = ContentScale.Fit, // whole cover, letterboxed on the surface
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (painter.state is AsyncImagePainter.State.Success) {
                            SubcomposeAsyncImageContent()
                        } else {
                            DefaultCover(game)
                        }
                    }
                } else {
                    DefaultCover(game)
                }

                val p = game.players
                if (p != null && p >= 2) {
                    Text(
                        "👥$p",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tileCaption(game.displayName, game.year),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
```

Notes for the implementer:
- The clickable `onClick` moves onto the cover `Card` (the whole tile no longer needs to be one Card). Focus visuals come from the Card as before.
- Add the imports you now need and remove any that are now unused. Likely new: `androidx.compose.foundation.layout.Column`, `androidx.compose.foundation.layout.aspectRatio`, `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.height`, `androidx.compose.ui.text.style.TextOverflow`. (`height` on a Box was used before via the old fixed size; ensure the final import set compiles.)
- Leave `DefaultCover` as-is; it already `fillMaxSize()`s, so it fills the square.

- [ ] **Step 2: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. Fix any unused-import / missing-import errors and rebuild.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/GameTile.kt
git commit -m "feat(home): square GameTile with Name (Year) caption"
```

---

### Task 3: Unify Home into one scroll (continue-playing as a full-span header)

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt`

Goal: replace the `Column { continueColumn; LazyVerticalGrid }` (lines ~90-131) with a single
`LazyVerticalGrid` that contains a leading full-span "Continue playing" header (title + LazyRow)
when present, then the game tiles — so everything scrolls together and continue-playing scrolls
away. Preserve today's focus behavior.

- [ ] **Step 1: Replace the content branch**

In `HomeScreen`, the `else -> { Column(verticalArrangement = spacedBy(24.dp)) { ... } }` block is
the target. Replace its body with a single grid:

```kotlin
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(132.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (hasContinue) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            // First focusable content row routes UP to the top-bar Settings.
                            Column(Modifier.focusProperties { up = settingsFocus }) {
                                Text(
                                    stringResource(R.string.home_continue_playing),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(state.continuePlaying, key = { it.id }) { g ->
                                        GameTile(
                                            game = g,
                                            onClick = { onOpenGame(g.id) },
                                            requestInitialFocus = g == state.continuePlaying.first(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(state.games, key = { it.id }) { g ->
                        GameTile(
                            game = g,
                            onClick = { onOpenGame(g.id) },
                            requestInitialFocus = !hasContinue && g == state.games.first(),
                            modifier = if (!hasContinue && g == state.games.first())
                                Modifier.focusProperties { up = settingsFocus } else Modifier,
                        )
                    }
                }
            }
```

Notes for the implementer:
- Import `androidx.compose.foundation.lazy.grid.GridItemSpan`. `LazyRow`, `items` (both
  `foundation.lazy.items` for the row and `foundation.lazy.grid.items` for the grid) are already
  imported in this file — keep both.
- The old code applied `up = settingsFocus` to the grid via a `gridModifier`. In the no-continue
  case we now apply it to the first grid tile's modifier (as above) so UP still reaches Settings.
  `GameTile` already accepts a `modifier` param and applies it.
- Remove the now-unused `val gridModifier = ...` and the outer `Column(verticalArrangement …)` and
  the old separate continue `Column` — they're replaced by the single grid. Keep `val hasContinue`.
- Do NOT change the top bar, chips, `SearchField`, `FilterChip`, `EmptyState`, or the ViewModel.

- [ ] **Step 2: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. Resolve unused imports (`Arrangement` still used; the old two-scroll
leftovers removed) and rebuild.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt
git commit -m "feat(home): single-scroll grid with Continue playing as a full-span header"
```

---

### Task 4: On-device acceptance

Install and verify on the ONN. Wake the screen before each screenshot; capture to the scratchpad.

- [ ] **Step 1: Unit tests + install**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
$ADB shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
$ADB shell input keyevent KEYCODE_WAKEUP
$ADB shell am start -n com.gobe.tv/.MainActivity
```
Expected: unit tests BUILD SUCCESSFUL (TileCaptionTest green); app installs and opens.

- [ ] **Step 2: Acceptance checklist (screenshot each; wake screen first)**

Capture with:
```
$ADB exec-out screencap -p > /private/tmp/.../scratchpad/redesign-<n>.png
```
Verify:
  - Tiles are **square** with a **`Name (Year)`** caption underneath; covers are not cropped
    (portrait art letterboxes on the sides, wide art on top/bottom).
  - **More games per screen** than before; each row has more columns than the old 140dp layout.
  - **Continue playing scrolls away**: from the top it's visible, and scrolling DOWN moves it
    off-screen so the full catalog shows (it's no longer pinned/cutting the grid).
  - **Focus**: D-pad moves from the Continue-playing row down into the grid and back up; from the
    top row, UP reaches the Settings button. A game opens on select (SNES and an arcade game).
  - A **long title** truncates to ≤2 lines with an ellipsis (find one, or note none present).

- [ ] **Step 3: If a check fails**

Focus issue between the header LazyRow and the grid (Spec §7 risk 1): if D-pad can't move between
them, the fallback is to keep the continue row as a compact pinned row above a separate grid
(smaller tiles) — surface to the human before changing the approach. Column count wrong → tune the
`GridCells.Adaptive` min width. Caption too tall → reduce to `maxLines = 1` or smaller type.

---

### Task 5: RESULTS, finish & merge

**Files:**
- Create: `docs/superpowers/plans/2026-06-30-gobe-home-grid-redesign-RESULTS.md`

- [ ] **Step 1: Write RESULTS**

Document: final tile size/column count chosen; caption line count; how continue-playing scrolls;
any focus caveats found on-device; before/after notes; screenshots referenced.

- [ ] **Step 2: Commit RESULTS**

```bash
git add docs/superpowers/plans/2026-06-30-gobe-home-grid-redesign-RESULTS.md
git commit -m "docs(home): RESULTS — grid redesign (square tiles, single scroll, captions)"
```

- [ ] **Step 3: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, then merge
`feat/home-grid-redesign` → `main` and push (the established pattern for this project).

---

## Notes for the implementer

- **DRY:** the caption comes only from `tileCaption`; don't inline the `(year)` logic in Compose.
- **Don't touch** the box-art pipeline, detail screen, ViewModel, chips, or search — scope is the
  two Home files + one test.
- **Focus is the one real risk** — verify the header-row↔grid D-pad transition on-device (Task 4).
- Keep the players badge and `DefaultCover` behavior intact; only their container shape changed.
