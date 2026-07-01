# Home Gamepad Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add gamepad shortcuts (L1→Search, R1→Settings) and a subtle on-screen button legend to the Home screen so navigation is faster and discoverable.

**Architecture:** A Compose-free pure helper `keyToHomeAction(keyCode)` (unit-tested) maps L1/R1 to actions; `HomeScreen` intercepts keys via `Modifier.onPreviewKeyEvent` on its root and runs the action (focus the search field / open Settings). A fixed low-opacity `HomeControlLegend` row sits at the bottom of Home below a weighted grid.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`onPreviewKeyEvent`, `FocusRequester`, `SoftwareKeyboardController`), `android.view.KeyEvent` key codes, JUnit (JVM) + on-device verification over wireless adb.

**Spec:** `docs/superpowers/specs/2026-06-30-gobe-gamepad-nav-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch `feat/gamepad-nav` already exists (spec committed there). Stay on it.
- **Environment gotchas** (subagents MUST know): `java`/`adb` are NOT on PATH.
  - JDK: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"` before any `./gradlew`.
  - adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Device `192.168.1.219:5555` (ONN). Wake with `adb shell input keyevent KEYCODE_WAKEUP` before screenshots. Re-grant `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow` after reinstall. Send a shoulder button with `adb shell input keyevent 102` (L1) / `103` (R1).
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## File map

- `app/src/main/java/com/gobe/tv/ui/home/HomeKeyMap.kt` — **Create**: Compose-free `enum HomeKeyAction { Search, Settings }` + pure `keyToHomeAction(keyCode: Int): HomeKeyAction?`. Kept Compose-free so its JVM test drags in no Android/Compose deps (per spec reviewer note).
- `app/src/test/java/com/gobe/tv/ui/home/HomeKeyMapTest.kt` — **Create**: JVM unit test.
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — **Modify**: root `onPreviewKeyEvent`; `searchFocus` FocusRequester wired into `SearchField`; grid gets `weight(1f)`; add `HomeControlLegend()` at the bottom.
- `app/src/main/res/values/strings.xml` + `app/src/main/res/values-es/strings.xml` — **Modify**: legend label strings (EN + ES).

Reference (current `HomeScreen` facts): it is a `Column(Modifier.fillMaxSize().padding(40.dp))`; `SearchField(value, onValueChange, modifier)` wraps a `BasicTextField`; the Settings `Button` already uses `settingsFocus`; `onOpenSettings: () -> Unit` is a `HomeScreen` param; content is a `when {}` whose `else` branch holds the `LazyVerticalGrid`.

---

### Task 1: `keyToHomeAction` pure helper (TDD)

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/home/HomeKeyMap.kt`
- Create: `app/src/test/java/com/gobe/tv/ui/home/HomeKeyMapTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/gobe/tv/ui/home/HomeKeyMapTest.kt`:
```kotlin
package com.gobe.tv.ui.home

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeKeyMapTest {
    @Test fun l1MapsToSearch() =
        assertEquals(HomeKeyAction.Search, keyToHomeAction(KeyEvent.KEYCODE_BUTTON_L1))

    @Test fun r1MapsToSettings() =
        assertEquals(HomeKeyAction.Settings, keyToHomeAction(KeyEvent.KEYCODE_BUTTON_R1))

    @Test fun otherKeysMapToNull() {
        assertNull(keyToHomeAction(KeyEvent.KEYCODE_BUTTON_A))
        assertNull(keyToHomeAction(KeyEvent.KEYCODE_DPAD_CENTER))
    }
}
```
Note: `android.view.KeyEvent` key-code constants are plain ints available to JVM unit tests in
this project (Android SDK is on the unit-test classpath; other unit tests here already use
Android types). If the constants are not resolvable at JVM test time, replace them with their
literal values `KEYCODE_BUTTON_L1 = 102`, `KEYCODE_BUTTON_R1 = 103`, `KEYCODE_BUTTON_A = 96`,
`KEYCODE_DPAD_CENTER = 23`.

- [ ] **Step 2: Run the test, verify it fails**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.ui.home.HomeKeyMapTest"
```
Expected: FAIL — `HomeKeyAction` / `keyToHomeAction` unresolved.

- [ ] **Step 3: Create the helper**

Create `app/src/main/java/com/gobe/tv/ui/home/HomeKeyMap.kt`:
```kotlin
package com.gobe.tv.ui.home

import android.view.KeyEvent

/** Home gamepad shortcut actions. */
enum class HomeKeyAction { Search, Settings }

/** Maps a gamepad key code to a Home shortcut action (L1→Search, R1→Settings), or null. Pure. */
fun keyToHomeAction(keyCode: Int): HomeKeyAction? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L1 -> HomeKeyAction.Search
    KeyEvent.KEYCODE_BUTTON_R1 -> HomeKeyAction.Settings
    else -> null
}
```

- [ ] **Step 4: Run the test, verify it passes**

Same command as Step 2. Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeKeyMap.kt app/src/test/java/com/gobe/tv/ui/home/HomeKeyMapTest.kt
git commit -m "feat(home): keyToHomeAction — L1→Search, R1→Settings (pure, TDD)"
```

---

### Task 2: Legend strings (EN + ES)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Add strings**

In `app/src/main/res/values/strings.xml` (near the other `home_*` strings), add:
```xml
    <string name="legend_select">Select</string>
    <string name="legend_back">Back</string>
    <string name="legend_search">Search</string>
    <string name="legend_settings">Settings</string>
```
In `app/src/main/res/values-es/strings.xml` (matching keys), add:
```xml
    <string name="legend_select">Seleccionar</string>
    <string name="legend_back">Volver</string>
    <string name="legend_search">Buscar</string>
    <string name="legend_settings">Ajustes</string>
```

- [ ] **Step 2: Build to verify resources compile**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(home): legend label strings (EN + ES)"
```

---

### Task 3: Wire shortcuts + legend into `HomeScreen`

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt`

No JVM-unit-testable surface (Compose UI + key handling); verified on-device in Task 4.

- [ ] **Step 1: Add a search FocusRequester and intercept keys on the root**

At the top of `HomeScreen` (beside `val settingsFocus = remember { FocusRequester() }`), add:
```kotlin
    val searchFocus = remember { FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
```
Change the root `Column(Modifier.fillMaxSize().padding(40.dp))` to also intercept keys:
```kotlin
    Column(
        Modifier
            .fillMaxSize()
            .padding(40.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyToHomeAction(event.key.nativeKeyCode)) {
                    HomeKeyAction.Search -> {
                        runCatching { searchFocus.requestFocus() }
                        keyboard?.show()
                        true
                    }
                    HomeKeyAction.Settings -> { onOpenSettings(); true }
                    null -> false
                }
            },
    ) {
```
Add imports:
- `androidx.compose.ui.input.key.onPreviewKeyEvent`
- `androidx.compose.ui.input.key.KeyEventType`
- `androidx.compose.ui.input.key.type`
- `androidx.compose.ui.input.key.key`
- `androidx.compose.ui.input.key.nativeKeyCode`
(`LocalSoftwareKeyboardController` is referenced fully-qualified above; you may add an import instead.)

- [ ] **Step 2: Pass the FocusRequester into `SearchField` and apply it**

Update the `SearchField(...)` call in the top bar to pass the requester:
```kotlin
            SearchField(
                value = query,
                onValueChange = vm::setQuery,
                focusRequester = searchFocus,
                modifier = Modifier.weight(1f),
            )
```
Update the `SearchField` composable signature + apply the requester to the `BasicTextField`:
```kotlin
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    ...
        BasicTextField(
            ...
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(focusRequester),
            ...
        )
    ...
}
```
(`focusRequester` import `androidx.compose.ui.focus.focusRequester` is already present in this file.)

- [ ] **Step 3: Give the content a weight and add the legend at the bottom**

The `when { … }` content block must not consume all height, so the legend stays pinned. Wrap the
`when` in a `Box(Modifier.weight(1f).fillMaxWidth())`, then add the legend after it, still inside
the root `Column`:
```kotlin
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> ...
                ... -> EmptyState(onOpenSettings)
                else -> { /* the LazyVerticalGrid (unchanged) */ }
            }
        }
        HomeControlLegend()
```
(If the `else` grid used `Modifier.fillMaxSize()`, keep it — it now fills the weighted Box.)

- [ ] **Step 4: Add the `HomeControlLegend` composable**

Add at file scope (e.g. below `FilterChip`):
```kotlin
@Composable
private fun HomeControlLegend() {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        LegendItem("Ⓐ", stringResource(R.string.legend_select), color)
        LegendItem("Ⓑ", stringResource(R.string.legend_back), color)
        LegendItem("L1", stringResource(R.string.legend_search), color)
        LegendItem("R1", stringResource(R.string.legend_settings), color)
    }
}

@Composable
private fun LegendItem(glyph: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        "$glyph $label",
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}
```

- [ ] **Step 5: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. Resolve any missing/unused imports and rebuild.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt
git commit -m "feat(home): L1→Search / R1→Settings shortcuts + button legend"
```

---

### Task 4: On-device acceptance

Install and verify on the ONN. Wake the screen; capture to the scratchpad. A **gamepad** is
needed to press L1/R1 — if the controller isn't reachable, simulate with
`adb shell input keyevent 102` (L1) and `103` (R1) as a proxy.

- [ ] **Step 1: Unit tests + install**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
$ADB shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
$ADB shell input keyevent KEYCODE_WAKEUP
$ADB shell am force-stop com.gobe.tv
$ADB shell am start -n com.gobe.tv/.MainActivity
```
Expected: `HomeKeyMapTest` green; app opens to Home.

- [ ] **Step 2: Acceptance checklist (screenshot each; wake first)**

  - The **legend bar** is visible at the bottom of Home: `Ⓐ Select · Ⓑ Back · L1 Search · R1 Settings`
    (subtle, legible). Switch device language to Spanish and confirm `Ⓐ Seleccionar · Ⓑ Volver ·
    L1 Buscar · R1 Ajustes`.
  - **L1** (`adb shell input keyevent 102`, or the physical button) → the **search field focuses**
    (cursor in the box / IME appears). Screenshot before/after.
  - **R1** (`adb shell input keyevent 103`) → the **Settings screen opens**. Screenshot.
  - Normal D-pad + A(select)/B(back) navigation still works (open a game, go back).

- [ ] **Step 3: If a check fails**

If L1/R1 don't register via `onPreviewKeyEvent` (Spec §7 risk 1), implement the fallback: handle
the two key codes in `MainActivity.dispatchKeyEvent`, gated so it only acts on the Home route, and
route to the same actions. If the IME doesn't appear on L1, ensure `keyboard?.show()` runs after
`requestFocus()` (it may need to be posted). Glyphs illegible → swap `Ⓐ/Ⓑ` for `A`/`B` in circles
drawn with a `Box`, or plain `A`/`B`.

---

### Task 5: RESULTS, finish & merge

**Files:**
- Create: `docs/superpowers/plans/2026-06-30-gobe-gamepad-nav-RESULTS.md`

- [ ] **Step 1: Write RESULTS**

Document: final mapping + legend text; whether `onPreviewKeyEvent` sufficed or the
`dispatchKeyEvent` fallback was needed; whether `keyboard?.show()` was needed for the IME; any
glyph/legibility tweaks; on-device screenshots referenced.

- [ ] **Step 2: Commit RESULTS**

```bash
git add docs/superpowers/plans/2026-06-30-gobe-gamepad-nav-RESULTS.md
git commit -m "docs(home): RESULTS — gamepad nav shortcuts + button legend"
```

- [ ] **Step 3: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, then merge
`feat/gamepad-nav` → `main` and push (established pattern for this project). Then update
`docs/superpowers/BACKLOG.md` to mark the gamepad-nav item done (or remove it).

---

## Notes for the implementer

- **DRY:** the key→action mapping lives only in `keyToHomeAction`; `HomeScreen` calls it, doesn't
  re-check key codes inline.
- **Compose-free helper:** keep `HomeKeyMap.kt` free of Compose imports so its JVM test stays clean.
- **Don't change** the grid redesign, chips, ViewModel, detail, or emulator — scope is the two
  Home files + the helper + strings.
- **Focus/legend are the real risks** — verify L1/R1 delivery and that the legend stays pinned
  (grid must be weighted) on-device (Task 4).
