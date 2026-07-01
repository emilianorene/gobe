# In-Game Controls Help — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the in-game menu discoverable: a brief auto-hiding launch hint ("Menu: Select+Start / Back") plus a control legend inside the pause overlay.

**Architecture:** Add a `showControlsHint` Compose state to `EmulatorActivity` (true at launch, auto-hidden after ~5 s and permanently once the menu opens) and render a new `ControlsHint` pill in the existing overlay `ComposeView`. Add a subtle legend line to `PauseOverlay`. New strings in EN/ES.

**Tech Stack:** Kotlin, Jetpack Compose for TV, `LaunchedEffect` + `kotlinx.coroutines.delay`, tv-material3, i18n string resources; on-device verification over wireless adb.

**Spec:** `docs/superpowers/specs/2026-07-01-gobe-ingame-controls-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch `feat/ingame-controls` already exists (spec committed there). Stay on it.
- **Environment gotchas** (subagents MUST know): `java`/`adb` are NOT on PATH.
  - JDK: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"` before any `./gradlew`.
  - adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Device `192.168.1.219:5555` (ONN). Wake with `adb shell input keyevent KEYCODE_WAKEUP`. Re-grant `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow` after reinstall.
  - Launch a game for testing: an SNES or Arcade game boots (FDS needs a BIOS the user hasn't added — don't use FDS to test).
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## File map

- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — **Modify**: `controls_hint_menu`, `pause_legend_select`, `pause_legend_close`.
- `app/src/main/java/com/gobe/tv/emulation/ui/ControlsHint.kt` — **Create**: the launch-hint pill composable.
- `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt` — **Modify**: `showControlsHint` state, hide-on-pause, render `ControlsHint` in the overlay.
- `app/src/main/java/com/gobe/tv/emulation/ui/PauseOverlay.kt` — **Modify**: add the legend line.

Current facts:
- `EmulatorActivity` has `private var paused by mutableStateOf(false)` and a `pause()` that sets
  `paused = true`. Its overlay `ComposeView.setContent { GobeTheme { if (paused) PauseOverlay(...) } }`.
- `PauseOverlay` is a `Column(... background Color(0xCC000000) ...)` of tv-material3 `Button`s; it
  already imports `Text`, `Color`, `MaterialTheme`, `stringResource`, `R`, layout bits.

---

### Task 1: Strings (EN + ES)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Add strings**

`values/strings.xml` (near the other `pause_`/`emu_` strings, ~line 50):
```xml
    <string name="controls_hint_menu">Menu: Select + Start · or Back</string>
    <string name="pause_legend_select">Select</string>
    <string name="pause_legend_close">Close</string>
```
`values-es/strings.xml` (matching keys):
```xml
    <string name="controls_hint_menu">Menú: Select + Start · o Atrás</string>
    <string name="pause_legend_select">Elegir</string>
    <string name="pause_legend_close">Cerrar</string>
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
git commit -m "feat(emu): strings for in-game controls hint + pause legend (EN/ES)"
```

---

### Task 2: `ControlsHint` composable + wire into `EmulatorActivity`

**Files:**
- Create: `app/src/main/java/com/gobe/tv/emulation/ui/ControlsHint.kt`
- Modify: `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt`

- [ ] **Step 1: Create the `ControlsHint` composable**

`app/src/main/java/com/gobe/tv/emulation/ui/ControlsHint.kt`:
```kotlin
package com.gobe.tv.emulation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R

/** Brief, non-intrusive pill shown at the bottom when a game starts, teaching how to open the menu. */
@Composable
fun ControlsHint() {
    Box(Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
        Text(
            stringResource(R.string.controls_hint_menu),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
```

- [ ] **Step 2: Add hint state + hide-on-pause to `EmulatorActivity`**

Next to `private var paused by mutableStateOf(false)`, add:
```kotlin
    // Shown briefly at launch to teach the menu combo; hidden after a delay AND permanently once
    // the menu has been opened (so it never returns on resume).
    private var showControlsHint by mutableStateOf(true)
```
In `pause()`, set it false (so it's gone for good after the first menu open):
```kotlin
    private fun pause() {
        retroView?.frameSpeed = 0
        retroView?.audioEnabled = false
        paused = true
        showControlsHint = false
    }
```

- [ ] **Step 3: Render the hint in the overlay ComposeView**

In the overlay `setContent { GobeTheme { ... } }`, render the hint alongside the pause overlay:
```kotlin
            setContent {
                GobeTheme {
                    if (paused) {
                        PauseOverlay(
                            hasState = hasState,
                            onResume = ::resume,
                            onSave = ::saveState,
                            onLoad = ::loadState,
                            onExit = ::exitToMenu,
                        )
                    }
                    if (showControlsHint && !paused) {
                        ControlsHint()
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(5000)
                            showControlsHint = false
                        }
                    }
                }
            }
```
(Import `com.gobe.tv.emulation.ui.ControlsHint`, or reference it fully-qualified. The `LaunchedEffect`
runs once when the hint first composes; the `delay` is cancelled if the Activity finishes — fine.)

- [ ] **Step 4: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/ui/ControlsHint.kt app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt
git commit -m "feat(emu): auto-hiding launch controls hint (gone after menu opens)"
```

---

### Task 3: Pause-overlay control legend

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/emulation/ui/PauseOverlay.kt`

- [ ] **Step 1: Add the legend line at the bottom of the overlay Column**

After the last `Button` (Exit to Gobe) inside the `Column`, add:
```kotlin
        Spacer(Modifier.height(8.dp))
        Text(
            "Ⓐ " + stringResource(R.string.pause_legend_select) +
                "  ·  Ⓑ/Back " + stringResource(R.string.pause_legend_close),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
```
Add imports if missing: `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.height`.
(`Text`, `Color`, `MaterialTheme`, `stringResource`, `Modifier`, `dp` are already imported.)

- [ ] **Step 2: Build to verify it compiles**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/ui/PauseOverlay.kt
git commit -m "feat(emu): control legend line in the pause overlay"
```

---

### Task 4: On-device acceptance + RESULTS + merge

- [ ] **Step 1: Install + launch a game**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:installDebug
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
$ADB shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
$ADB shell input keyevent KEYCODE_WAKEUP
$ADB shell am start -n com.gobe.tv/.MainActivity
# navigate to an SNES or Arcade game → detail → Play (or launch via the grid)
```

- [ ] **Step 2: Acceptance checklist (screenshot each; wake first)**

  - On game launch, the **hint pill** appears bottom-center ("Menú: Select + Start · o Atrás") and
    **disappears after ~5 s** (screenshot right after launch, then again ~6 s later).
  - Open the menu (Back, or Select+Start on a gamepad) → the pause overlay shows the **legend line**
    `Ⓐ Elegir · Ⓑ/Back Cerrar`; the hint does **not** reappear after resuming.
  - Menu still works: A selects, B/Back closes/resumes, Exit to Gobe returns to the grid.
  - Spanish locale → hint + legend translated.

- [ ] **Step 3: Write + commit RESULTS**

Create `docs/superpowers/plans/2026-07-01-gobe-ingame-controls-RESULTS.md`: what shipped, hint
duration used, screenshots referenced, any tweaks. Then commit.

- [ ] **Step 4: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, merge
`feat/ingame-controls` → `main`, push.

---

## Notes for the implementer

- **"Gone after discovery":** `pause()` sets `showControlsHint = false`, so once the user opens the
  menu the hint never returns on resume (the `!paused` guard alone would let it re-show within the 5 s
  window — the assignment is what makes it permanent).
- **No new unit test** — negligible pure logic; verified on-device.
- **Don't change** button bindings, the pause flow, or anything outside these four files.
- Test with an **SNES/Arcade** game (FDS needs a BIOS not present).
