# Gobe — Console Section Tiles (controller art Hero cards) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat text tiles on the console-first Home (Recommended, Favorites, one per console) with "Hero cards" — a per-console controller illustration (or ★/♥) centered on a per-section accent glow, name underneath — plus light Home-grid polish.

**Architecture:** A pure UI-layer mapping `sectionVisual(LibrarySection) → SectionVisual(iconRes, accent)` (the single console→art/color table, unit-tested) drives a rewritten `SectionTile` Hero-card composable. Art ships as 6 bundled `VectorDrawable`s (4 controllers baked multi-color + a gold star + a red heart), so every tile renders a uniform `painterResource(iconRes)` with no tint; the accent drives only the glow. `System` (domain) stays pure — no color/drawable there.

**Tech Stack:** Kotlin, Jetpack Compose **for TV** (`androidx.tv.material3`), Android VectorDrawable, JUnit4.

**Spec:** `docs/superpowers/specs/2026-07-06-gobe-console-tiles-design.md`

**Build/test env:**
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- Unit: `./gradlew :app:testDebugUnitTest`. Compile: `./gradlew :app:assembleDebug`.
- Gradle is slow (~1–10 min); run and wait, don't abort.
- On-device: **ask the user before installing** (they actively use the ONN).
- Do NOT bump app version / CHANGELOG (that's release packaging).
- Use TARGETED `git add <files>` (never `-A`/`-am`).

---

## File Structure

**Create:**
- `app/src/main/res/drawable/ic_controller_nes.xml`, `ic_controller_snes.xml`, `ic_controller_n64.xml`, `ic_controller_arcade.xml` — controller illustrations (paths only, own colors baked in).
- `app/src/main/res/drawable/ic_section_recommended.xml` (gold star), `ic_section_favorites.xml` (red heart).
- `app/src/main/java/com/gobe/tv/ui/home/SectionVisuals.kt` — `SectionVisual` + pure `sectionVisual(...)`.
- `app/src/test/java/com/gobe/tv/ui/home/SectionVisualsTest.kt` — unit test for the mapping.

**Modify:**
- `app/src/main/java/com/gobe/tv/ui/theme/Color.kt` — named per-section accent constants.
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — `SectionTile` rewrite + grid split/polish.

---

## Task 1: Per-section accent color constants

**Files:** Modify `app/src/main/java/com/gobe/tv/ui/theme/Color.kt`.

Palette resolves the spec's NES-vs-Favorites red collision (NES = deep orange, distinct from Favorites red). Six mutually-distinct hues on the dark theme.

- [ ] **Step 1: Add the constants.** Append to `Color.kt` (match the file's existing `val Name = Color(0xFF......)` style):
```kotlin
// Per-section accent colors for the Home Hero tiles (glow + baked star/heart art).
val GobeAccentRecommended = Color(0xFFFFC94D) // gold (matches the ★ badge family)
val GobeAccentFavorites   = Color(0xFFE53935) // red  (matches the ♥ badge)
val GobeAccentNes         = Color(0xFFFF7043) // deep orange (distinct from Favorites red)
val GobeAccentSnes        = Color(0xFFA88BFF) // lavender (distinct from brand primary #7C5CFF)
val GobeAccentN64         = Color(0xFF22B8A6) // teal
val GobeAccentArcade      = Color(0xFFF5A623) // amber
```
- [ ] **Step 2: Verify it compiles.** Run: `./gradlew :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: Commit.**
```bash
git add app/src/main/java/com/gobe/tv/ui/theme/Color.kt
git commit -m "feat(theme): per-section accent colors for Home Hero tiles"
```

---

## Task 2: Section art — 6 VectorDrawables

**Files:** Create the 6 `app/src/main/res/drawable/ic_*.xml` files listed above.

Requirements for ALL: valid Android `VectorDrawable` (paths ONLY — no `<rect>`/`<circle>`; convert shapes to `pathData`). Colors are **baked into the drawable** (do NOT rely on tint) so the tile renders them untinted. Star = gold `#FFC94D`, heart = red `#E53935` (match the accents). Controllers use their own realistic button colors. `viewportWidth/Height` per the reference; `android:width/height` in dp can be ~2× the viewport for crispness.

Reference SVGs live in `.superpowers/brainstorm/42098-1783354164/controllers-v2.html` (the `<defs>` block `#c-nes`, `#c-snes`, `#c-n64`, `#c-arcade`) — the visual target. Convert each to paths.

- [ ] **Step 1: Star** — `ic_section_recommended.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFC94D"
        android:pathData="M12,2 l2.9,6.26 6.85,0.62 -5.18,4.53 1.53,6.7 -6.1,-3.63 -6.1,3.63 1.53,-6.7 -5.18,-4.53 6.85,-0.62 z"/>
</vector>
```
- [ ] **Step 2: Heart** — `ic_section_favorites.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#E53935"
        android:pathData="M12,21 C4.5,15 2,11.5 2,8.2 A5,5 0 0,1 12,6 A5,5 0 0,1 22,8.2 C22,11.5 19.5,15 12,21 z"/>
</vector>
```
- [ ] **Step 3: NES controller** — `ic_controller_nes.xml` (full worked example of the shape→path conversion; use this pattern for the rest). Viewport 100×60. Body dark grey + light top strip, black D-pad, two dark-red buttons, two maroon select/start pills:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp" android:height="72dp" android:viewportWidth="100" android:viewportHeight="60">
    <!-- body -->
    <path android:fillColor="#3A3F4A" android:strokeColor="#20242C" android:strokeWidth="1.5"
        android:pathData="M11,12 H89 a5,5 0 0 1 5,5 V45 a5,5 0 0 1 -5,5 H11 a5,5 0 0 1 -5,-5 V17 a5,5 0 0 1 5,-5 z"/>
    <!-- top light strip -->
    <path android:fillColor="#C9CCD4"
        android:pathData="M11.5,14 H88.5 a2.5,2.5 0 0 1 2.5,2.5 V19.5 a2.5,2.5 0 0 1 -2.5,2.5 H11.5 a2.5,2.5 0 0 1 -2.5,-2.5 V16.5 a2.5,2.5 0 0 1 2.5,-2.5 z"/>
    <!-- D-pad (two crossed bars) -->
    <path android:fillColor="#15181E" android:pathData="M16,30 h18 v5.5 h-18 z"/>
    <path android:fillColor="#15181E" android:pathData="M22.25,24 h5.5 v18 h-5.5 z"/>
    <!-- select / start pills -->
    <path android:fillColor="#8A1F1F" android:pathData="M44,35 h4 a2,2 0 0 1 0,4 h-4 a2,2 0 0 1 0,-4 z"/>
    <path android:fillColor="#8A1F1F" android:pathData="M54,35 h4 a2,2 0 0 1 0,4 h-4 a2,2 0 0 1 0,-4 z"/>
    <!-- A / B buttons (circles as two half-arcs) -->
    <path android:fillColor="#C0392B" android:pathData="M66.5,35 a5.5,5.5 0 1 0 11,0 a5.5,5.5 0 1 0 -11,0 z"/>
    <path android:fillColor="#C0392B" android:pathData="M80.5,35 a5.5,5.5 0 1 0 11,0 a5.5,5.5 0 1 0 -11,0 z"/>
</vector>
```
- [ ] **Step 4: SNES controller** — `ic_controller_snes.xml`, viewport 100×56. Convert `#c-snes` from the reference: a light-grey rounded "dog-bone" body (`M19,14 H81 a15,15 0 0 1 0,30 H19 a15,15 0 0 1 0,-30 z`, fill `#D7D9E0` stroke `#B3B7C2`), black D-pad (two bars like NES), two small grey Select/Start pills, and the **four colored face buttons** as circles-via-arcs in a diamond: X top `#4C8FD6`, Y left `#4CAF50`, A right `#D84A3A`, B bottom `#E0C23A` (each r≈4). Keep the colors — they're the SNES signature.
- [ ] **Step 5: N64 controller** — `ic_controller_n64.xml`, viewport 100×64. Convert `#c-n64`: three light-grey rounded prongs fanning from a center hub (center bar + a left and right rounded bar rotated outward — a `<group android:rotation="19" android:pivotX="44" android:pivotY="33">` wrapping the left bar path, and `-19` for the right, is allowed in VectorDrawable), a dark analog stick (circle) at center-top, a small black D-pad on the left prong, a yellow C-button cluster (4 small circles `#E0C23A`) on the right prong, and a blue A button `#2E6FD6`.
- [ ] **Step 6: Arcade stick** — `ic_controller_arcade.xml`, viewport 100×62. Convert `#c-arcade`: a dark rounded panel `#2A2E36` (stroke `#15181E`), a joystick (dark base ellipse-as-path, grey shaft rect-path, red ball circle `#D84A3A`), and an arc of 5 colored buttons (`#E0C23A`, `#D84A3A`, `#4CAF50`, `#2E6FD6`, `#D84EA0`), each a circle-via-arcs r≈5.
- [ ] **Step 7: Verify resources compile.** Run: `./gradlew :app:assembleDebug`. Expected: `BUILD SUCCESSFUL` (an invalid `pathData` fails the build). Note: exact art fidelity is judged on-device in Task 6, not here.
- [ ] **Step 8: Commit.**
```bash
git add app/src/main/res/drawable/ic_controller_nes.xml app/src/main/res/drawable/ic_controller_snes.xml app/src/main/res/drawable/ic_controller_n64.xml app/src/main/res/drawable/ic_controller_arcade.xml app/src/main/res/drawable/ic_section_recommended.xml app/src/main/res/drawable/ic_section_favorites.xml
git commit -m "feat(art): controller + star/heart vector drawables for Home tiles"
```

---

## Task 3: `SectionVisuals.kt` — pure mapping (TDD)

**Files:** Create `app/src/main/java/com/gobe/tv/ui/home/SectionVisuals.kt`, test `app/src/test/java/com/gobe/tv/ui/home/SectionVisualsTest.kt`.

First inspect `app/src/main/java/com/gobe/tv/ui/library/LibrarySection.kt` to confirm the variant names/shape (`Console(system)`, `Recommended`, `Favorites`, `SearchAll(query)`).

- [ ] **Step 1: Write the failing test** `SectionVisualsTest.kt`:
```kotlin
package com.gobe.tv.ui.home

import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.library.LibrarySection
import com.gobe.tv.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SectionVisualsTest {
    @Test fun consolesMapToTheirControllerAndAccent() {
        assertEquals(R.drawable.ic_controller_nes, sectionVisual(LibrarySection.Console(System.NES)).iconRes)
        assertEquals(GobeAccentNes, sectionVisual(LibrarySection.Console(System.NES)).accent)
        assertEquals(R.drawable.ic_controller_snes, sectionVisual(LibrarySection.Console(System.SNES)).iconRes)
        assertEquals(R.drawable.ic_controller_n64, sectionVisual(LibrarySection.Console(System.N64)).iconRes)
        assertEquals(R.drawable.ic_controller_arcade, sectionVisual(LibrarySection.Console(System.ARCADE)).iconRes)
    }
    @Test fun recommendedAndFavoritesMapToStarAndHeart() {
        assertEquals(R.drawable.ic_section_recommended, sectionVisual(LibrarySection.Recommended).iconRes)
        assertEquals(GobeAccentRecommended, sectionVisual(LibrarySection.Recommended).accent)
        assertEquals(R.drawable.ic_section_favorites, sectionVisual(LibrarySection.Favorites).iconRes)
        assertEquals(GobeAccentFavorites, sectionVisual(LibrarySection.Favorites).accent)
    }
    @Test fun nesAccentDiffersFromFavoritesRed() { // guards the color collision
        assertNotEquals(sectionVisual(LibrarySection.Console(System.NES)).accent,
                        sectionVisual(LibrarySection.Favorites).accent)
    }
}
```
(If `LibrarySection.Console`/`Recommended`/`Favorites` are constructed differently than shown, adjust to the real API.)
- [ ] **Step 2: Run to verify fail.** Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.SectionVisualsTest"`. Expected: FAIL (`sectionVisual`/`SectionVisual` unresolved).
- [ ] **Step 3: Implement `SectionVisuals.kt`:**
```kotlin
package com.gobe.tv.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.library.LibrarySection
import com.gobe.tv.ui.theme.*

/** Icon (controller / star / heart) + accent color for a Home level-1 section tile. */
data class SectionVisual(@DrawableRes val iconRes: Int, val accent: Color)

/**
 * Pure map from a level-1 [LibrarySection] to its Hero-tile art + accent. Single source of the
 * console -> (controller drawable, color) table. `SearchAll` is never rendered as a section tile;
 * it returns a neutral fallback only to keep this `when` total.
 */
fun sectionVisual(section: LibrarySection): SectionVisual = when (section) {
    is LibrarySection.Console -> when (section.system) {
        System.NES    -> SectionVisual(R.drawable.ic_controller_nes, GobeAccentNes)
        System.SNES   -> SectionVisual(R.drawable.ic_controller_snes, GobeAccentSnes)
        System.N64    -> SectionVisual(R.drawable.ic_controller_n64, GobeAccentN64)
        System.ARCADE -> SectionVisual(R.drawable.ic_controller_arcade, GobeAccentArcade)
    }
    LibrarySection.Recommended -> SectionVisual(R.drawable.ic_section_recommended, GobeAccentRecommended)
    LibrarySection.Favorites   -> SectionVisual(R.drawable.ic_section_favorites, GobeAccentFavorites)
    is LibrarySection.SearchAll -> SectionVisual(R.drawable.ic_section_recommended, GobeAccent) // never rendered
}
```
(The inner `when (section.system)` has no `else`, so a future `System` value is a compile error. `GobeAccent` is the existing brand primary in `Color.kt` — confirm the name; use whatever the brand accent constant is called.)
- [ ] **Step 4: Run to verify pass.** Same command. Expected: PASS (3 tests).
- [ ] **Step 5: Commit.**
```bash
git add app/src/main/java/com/gobe/tv/ui/home/SectionVisuals.kt app/src/test/java/com/gobe/tv/ui/home/SectionVisualsTest.kt
git commit -m "feat(home): sectionVisual mapping (console -> controller art + accent)"
```

---

## Task 4: `SectionTile` Hero card rewrite

**Files:** Modify `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` (the `SectionTile` composable, ~lines 136–151).

Read the current `SectionTile` and its call site first. Keep the `onClick` + `requestInitialFocus` behavior; change only the tile's internals + signature (now also takes the `LibrarySection` so it can resolve `sectionVisual`). Match the file's existing imports (`androidx.tv.material3.*`).

- [ ] **Step 1: Rewrite the composable.** Replace `SectionTile` with:
```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTile(
    label: String,
    section: LibrarySection,
    onClick: () -> Unit,
    requestInitialFocus: Boolean,
) {
    val visual = sectionVisual(section)
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    if (requestInitialFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Card(
        onClick = onClick,
        modifier = (if (requestInitialFocus) Modifier.focusRequester(focus) else Modifier)
            .fillMaxWidth().aspectRatio(1.3f)
            .onFocusChanged { focused = it.isFocused },
        colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            // accent glow (brighter when focused)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(
                            visual.accent.copy(alpha = if (focused) 0.55f else 0.32f),
                            Color.Transparent,
                        ),
                        radius = 220f,
                    )
                )
            )
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(visual.iconRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.72f).weight(1f),
                )
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}
```
Add any missing imports: `androidx.compose.foundation.Image`, `androidx.compose.foundation.background`, `androidx.compose.foundation.layout.aspectRatio`, `androidx.compose.ui.draw` not needed, `androidx.compose.ui.graphics.Brush`, `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.layout.ContentScale`, `androidx.compose.ui.focus.onFocusChanged`, `androidx.compose.runtime.{mutableStateOf,remember,getValue,setValue}`, `androidx.compose.ui.res.painterResource`, `com.gobe.tv.ui.library.LibrarySection`.
- [ ] **Step 2: Update the call site** in `HomeScreen` where `SectionTile(...)` is invoked — pass the section. The sections list is `List<Pair<String, LibrarySection>>`, so pass `label = pair.first, section = pair.second`. (This may be adjusted further in Task 5's grid split; a minimal working call here is fine.)
- [ ] **Step 3: Verify compile.** Run: `./gradlew :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4: Commit.**
```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt
git commit -m "feat(home): SectionTile Hero card (controller art + accent glow + focus)"
```

---

## Task 5: Home grid split + polish

**Files:** Modify `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` (the `LazyVerticalGrid` content, ~lines 63–128).

Goal: (a) cell sizing so Hero cards breathe, (b) the "Consoles" header **between** the collections and the console tiles (today the single `itemsIndexed(sections)` pass renders them merged), (c) a clear header + spacing for the Continue-playing row (light touch).

- [ ] **Step 1: Split the sections list** where it's currently built (~lines 63–68). Instead of one merged `sections`, build two:
```kotlin
val collections: List<Pair<String, LibrarySection>> = buildList {
    add(stringResource(R.string.section_recommended) to LibrarySection.Recommended)
    add(stringResource(R.string.section_favorites) to LibrarySection.Favorites)
}
val consoles: List<Pair<String, LibrarySection>> =
    System.entries.map { it.displayName to LibrarySection.Console(it) }
```
- [ ] **Step 2: Render two groups with the header between.** In the `LazyVerticalGrid` content, render the Continue-playing row (if present) with its header, then the `collections` tiles, then a full-span "Consoles" header item, then the `consoles` tiles. Use `GridItemSpan(maxLineSpan)` for the header/row items. Sketch:
```kotlin
// Continue-playing (unchanged data), now with a clear header:
if (continueGames.isNotEmpty()) {
    item(span = { GridItemSpan(maxLineSpan) }) { RowHeader(stringResource(R.string.home_continue_playing)) }
    item(span = { GridItemSpan(maxLineSpan) }) { ContinuePlayingRow(continueGames, onGameClick) }
}
itemsIndexed(collections) { i, (label, section) ->
    SectionTile(label, section, onClick = { onSectionClick(section) },
        requestInitialFocus = continueGames.isEmpty() && i == 0)
}
item(span = { GridItemSpan(maxLineSpan) }) { RowHeader(stringResource(R.string.home_consoles)) }
itemsIndexed(consoles) { _, (label, section) ->
    SectionTile(label, section, onClick = { onSectionClick(section) }, requestInitialFocus = false)
}
```
Adapt names (`continueGames`, `onGameClick`, `onSectionClick`, the existing continue-playing composable) to the real code. Preserve the exact initial-focus rule that exists today (first collection tile focused only when there's no Continue-playing row).
- [ ] **Step 3: Add a small `RowHeader` helper** (if none exists) — a full-width uppercase section label:
```kotlin
@Composable
private fun RowHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp))
}
```
If a "Consoles" title composable already exists, reuse/rename it instead of duplicating (DRY). Ensure `R.string.home_continue_playing` exists (Task 3 reviewer confirmed it does); reuse `home_consoles`.
- [ ] **Step 4: Tune cell size/gaps** — bump `GridCells.Adaptive(160.dp)` to `Adaptive(180.dp)` and confirm the 16.dp gaps still look right (leave gaps unless cramped). Keep it minimal.
- [ ] **Step 5: Verify compile.** Run: `./gradlew :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 6: Commit.**
```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt
git commit -m "feat(home): split grid into collections/consoles with headers + spacing polish"
```

---

## Task 6: Full sweep + on-device

- [ ] **Step 1: Unit tests.** Run: `./gradlew :app:testDebugUnitTest`. Expected: `BUILD SUCCESSFUL` (incl. `SectionVisualsTest`, and the pre-existing Home tests `TileCaptionTest`/`HomeKeyMapTest` still green).
- [ ] **Step 2: Compile.** Run: `./gradlew :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3: On-device (ONLY after asking the user).** Install and check on the ONN: the Home shows controller Hero cards for NES/SNES/N64/Arcade + ★/♥ for Recommended/Favorites; the controllers are recognizable and legible at 10 ft; D-pad focus moves correctly and the accent glow intensifies on focus; the "Consoles" header sits between the collections and the console tiles; nothing is clipped. Iterate on the VectorDrawable art if any controller reads poorly (art is only verifiable here).

---

## Notes for the implementer
- Controllers are illustrations, not tinted glyphs — never apply a tint to their `Image`; colors are baked into the drawable.
- Compose **for TV**: use `androidx.tv.material3` (as the rest of `HomeScreen.kt` does), not `androidx.compose.material3`.
- Keep `System` (domain) pure — all color/drawable lives in the UI layer.
- Follow existing focus idioms in `HomeScreen.kt`; don't regress the initial-focus behavior.
