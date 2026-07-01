# Controllers — Button Swap Presets (sub-project C, first cut) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Per-controller **Swap A/B** and **Swap X/Y** toggles (in the controller detail) that the emulator applies as a keycode substitution, persisted per controller.

**Architecture:** A pure `ButtonSwaps` + `remapCode` (unit-tested), swap flags persisted per descriptor in `ControllerPrefs`, toggles in `ControllerDetailScreen`, and `EmulatorActivity` remapping the forwarded keycode (alongside the existing port routing; pause combo stays on raw codes; motion/sticks untouched).

**Tech Stack:** Kotlin, Compose for TV, Android `KeyEvent`/`InputDevice`, SharedPreferences (`gobe.settings`), LibretroDroid `sendKeyEvent`, JUnit (JVM) + on-device.

**Spec:** `docs/superpowers/specs/2026-07-01-gobe-controllers-swap-design.md`

---

## Pre-flight

- Branch `feat/controllers-swap` exists (spec committed). Stay on it.
- `java`/`adb` NOT on PATH: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"`; adb at `/opt/homebrew/share/android-commandlinetools/platform-tools/adb` (ONN `192.168.1.219:5555`, a Nintendo Switch Pro Controller connects but **sleeps on idle** — the user presses a button to wake it). Wake screen with `adb shell input keyevent KEYCODE_WAKEUP`; re-grant `appops ... MANAGE_EXTERNAL_STORAGE allow` after reinstall.
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## Existing anchors (from A/B)

- `EmulatorActivity.dispatchKeyEvent` forwards with `retroView?.sendKeyEvent(event.action, code, portForInput(event.deviceId))` (two identical call sites: the Select/Start branch and the general branch). Select+Start pause combo detects on the raw `code`. Motion via `sendMotionEvent(..., port)` (no remap needed).
- `ControllerPrefs` (object) persists per-descriptor Int port keys (`ctrl.port.<desc>`) in `gobe.settings`.
- `ControllerDetailScreen(name, assignedPort, activeButtons, leftStick, rightStick, lastInput, onAssign)` has the P1–P4 selector + `ControllerTestPanel`.
- `ControllersActivity` owns `assignments`, `rows`, `selected`, and the test state; renders list/detail.

## File map

- `app/src/main/java/com/gobe/tv/emulation/input/ButtonSwaps.kt` — **Create**: pure model + `remapCode`.
- `app/src/test/java/com/gobe/tv/emulation/input/ButtonSwapsTest.kt` — **Create**: JVM test.
- `app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt` — **Modify**: swap load/save.
- `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt` — **Modify**: apply `remapCode`.
- `app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt` — **Modify**: swap toggles in detail.
- `app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt` — **Modify**: swaps state + wire.
- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — **Modify**: swap labels.

---

### Task 1: `ButtonSwaps` + `remapCode` (pure, TDD)

**Files:** Create `ButtonSwaps.kt` + `ButtonSwapsTest.kt`.

- [ ] **Step 1: Failing test** — `app/src/test/java/com/gobe/tv/emulation/input/ButtonSwapsTest.kt`:
```kotlin
package com.gobe.tv.emulation.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ButtonSwapsTest {
    @Test fun swapABonly() {
        val s = ButtonSwaps(swapAB = true)
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, remapCode(KeyEvent.KEYCODE_BUTTON_A, s))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, remapCode(KeyEvent.KEYCODE_BUTTON_B, s))
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, remapCode(KeyEvent.KEYCODE_BUTTON_X, s)) // untouched
    }
    @Test fun swapXYonly() {
        val s = ButtonSwaps(swapXY = true)
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, remapCode(KeyEvent.KEYCODE_BUTTON_X, s))
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, remapCode(KeyEvent.KEYCODE_BUTTON_Y, s))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, remapCode(KeyEvent.KEYCODE_BUTTON_A, s)) // untouched
    }
    @Test fun noSwapPassesThrough() {
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, remapCode(KeyEvent.KEYCODE_BUTTON_A, ButtonSwaps()))
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, remapCode(KeyEvent.KEYCODE_DPAD_UP, ButtonSwaps(true, true)))
    }
    @Test fun bothSwaps() {
        val s = ButtonSwaps(swapAB = true, swapXY = true)
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, remapCode(KeyEvent.KEYCODE_BUTTON_A, s))
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, remapCode(KeyEvent.KEYCODE_BUTTON_X, s))
    }
}
```

- [ ] **Step 2: Run, verify FAIL** — `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.emulation.input.ButtonSwapsTest"` (with JAVA_HOME). Expected: unresolved.

- [ ] **Step 3: Implement** — `app/src/main/java/com/gobe/tv/emulation/input/ButtonSwaps.kt`:
```kotlin
package com.gobe.tv.emulation.input

import android.view.KeyEvent

/** Per-controller face-button swap preset. */
data class ButtonSwaps(val swapAB: Boolean = false, val swapXY: Boolean = false)

/** Substitutes the Android keycode: A<->B if swapAB, X<->Y if swapXY, else unchanged. Pure. */
fun remapCode(keyCode: Int, s: ButtonSwaps): Int = when {
    s.swapAB && keyCode == KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
    s.swapAB && keyCode == KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
    s.swapXY && keyCode == KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
    s.swapXY && keyCode == KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
    else -> keyCode
}
```

- [ ] **Step 4: Run, verify PASS.** Same command.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/gobe/tv/emulation/input/ButtonSwaps.kt app/src/test/java/com/gobe/tv/emulation/input/ButtonSwapsTest.kt
git commit -m "feat(controllers): ButtonSwaps + remapCode (A/B, X/Y swap; pure, TDD)"
```

---

### Task 2: Persist swaps in `ControllerPrefs`

**Files:** Modify `ControllerPrefs.kt`.

- [ ] **Step 1: Add swap load/save** — add to the `ControllerPrefs` object (import `com.gobe.tv.emulation.input.ButtonSwaps`):
```kotlin
    private const val SWAP_AB = "ctrl.swapab."
    private const val SWAP_XY = "ctrl.swapxy."

    fun loadSwaps(context: Context): Map<String, ButtonSwaps> {
        val p = prefs(context)
        val descriptors = p.all.keys
            .filter { it.startsWith(SWAP_AB) || it.startsWith(SWAP_XY) }
            .map { it.removePrefix(SWAP_AB).removePrefix(SWAP_XY) }
            .toSet()
        return descriptors.associateWith { d ->
            ButtonSwaps(p.getBoolean(SWAP_AB + d, false), p.getBoolean(SWAP_XY + d, false))
        }
    }

    fun saveSwaps(context: Context, descriptor: String, s: ButtonSwaps) {
        prefs(context).edit()
            .putBoolean(SWAP_AB + descriptor, s.swapAB)
            .putBoolean(SWAP_XY + descriptor, s.swapXY)
            .apply()
    }
```

- [ ] **Step 2: Build** — `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt
git commit -m "feat(controllers): persist per-controller button swaps"
```

---

### Task 3: Apply the swap in `EmulatorActivity`

**Files:** Modify `EmulatorActivity.kt`.

Swaps are read once at emulator launch (`onCreate`); changing them mid-session requires relaunching
the game (acceptable — settings are edited outside gameplay). Motion/`sendMotionEvent` is unaffected
(analog untouched). The Select+Start pause combo continues to use the RAW `code`.

- [ ] **Step 1: Load swaps + add a helper**

Add imports: `com.gobe.tv.emulation.input.ButtonSwaps`, `com.gobe.tv.emulation.input.remapCode`.
Add a field near `assignments`:
```kotlin
    private var swapsByDescriptor: Map<String, ButtonSwaps> = emptyMap()
```
In `onCreate` (right after `assignments = ControllerPrefs.load(this)`):
```kotlin
        swapsByDescriptor = ControllerPrefs.loadSwaps(this)
```
Add a helper near `portForInput`:
```kotlin
    private fun swapsForInput(deviceId: Int): ButtonSwaps =
        swapsByDescriptor[InputDevice.getDevice(deviceId)?.descriptor] ?: ButtonSwaps()
```

- [ ] **Step 2: Remap the forwarded keycode**

Both forward call sites are identical:
`retroView?.sendKeyEvent(event.action, code, portForInput(event.deviceId))`. Replace BOTH with:
```kotlin
            retroView?.sendKeyEvent(event.action, remapCode(code, swapsForInput(event.deviceId)), portForInput(event.deviceId))
```
(Leave the Select/Start combo detection — which reads raw `code` — unchanged. `remapCode` leaves
Select/Start untouched anyway.)

- [ ] **Step 3: Build** — `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt
git commit -m "feat(controllers): apply per-controller A/B, X/Y swap to emulator input"
```

---

### Task 4: Swap toggles in the controller detail (UI + wiring)

**Files:** Modify `ControllersScreen.kt`, `ControllersActivity.kt`, strings (EN/ES).

- [ ] **Step 1: Strings**

`values/strings.xml`:
```xml
    <string name="controllers_buttons">Buttons</string>
    <string name="controllers_swap_ab">Swap A/B</string>
    <string name="controllers_swap_xy">Swap X/Y</string>
```
`values-es/strings.xml`:
```xml
    <string name="controllers_buttons">Botones</string>
    <string name="controllers_swap_ab">Cambiar A/B</string>
    <string name="controllers_swap_xy">Cambiar X/Y</string>
```

- [ ] **Step 2: Add swap toggles to `ControllerDetailScreen`**

Extend its signature and render a "Buttons" row after the player selector (before the test panel):
```kotlin
    swaps: com.gobe.tv.emulation.input.ButtonSwaps,
    onToggleSwapAB: () -> Unit,
    onToggleSwapXY: () -> Unit,
```
```kotlin
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.controllers_buttons) + ":", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onToggleSwapAB) {
                Text((if (swaps.swapAB) "● " else "") + stringResource(R.string.controllers_swap_ab))
            }
            Button(onClick = onToggleSwapXY) {
                Text((if (swaps.swapXY) "● " else "") + stringResource(R.string.controllers_swap_xy))
            }
        }
```

- [ ] **Step 3: Wire in `ControllersActivity`**

- Add state + load in `onCreate`:
```kotlin
    private var swaps by mutableStateOf<Map<String, ButtonSwaps>>(emptyMap())
```
```kotlin
        swaps = ControllerPrefs.loadSwaps(this)   // in onCreate, near assignments load
```
- In the detail branch of `setContent`, pass the selected controller's swaps + toggle callbacks:
```kotlin
                                swaps = swaps[sel] ?: ButtonSwaps(),
                                onToggleSwapAB = {
                                    val cur = swaps[sel] ?: ButtonSwaps()
                                    val next = cur.copy(swapAB = !cur.swapAB)
                                    ControllerPrefs.saveSwaps(this@ControllersActivity, sel, next)
                                    swaps = swaps + (sel to next)
                                },
                                onToggleSwapXY = {
                                    val cur = swaps[sel] ?: ButtonSwaps()
                                    val next = cur.copy(swapXY = !cur.swapXY)
                                    ControllerPrefs.saveSwaps(this@ControllersActivity, sel, next)
                                    swaps = swaps + (sel to next)
                                },
```
(Import `com.gobe.tv.emulation.input.ButtonSwaps`.)

- [ ] **Step 4: Build** — `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL. Resolve imports.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(controllers): Swap A/B and Swap X/Y toggles in controller detail"
```

---

### Task 5: On-device acceptance + RESULTS + merge

- [ ] **Step 1: Unit tests + install** (`testDebugUnitTest`, `installDebug`, grant perm, wake).

- [ ] **Step 2: Acceptance (Pro Controller awake)**
  - Controller detail shows **Swap A/B** and **Swap X/Y** toggles (off by default for a controller
    with no saved swaps — confirms the `loadSwaps` map-miss → `ButtonSwaps()` default).
  - Toggle **Swap A/B** on (shows "● Swap A/B"); it persists across reopen.
  - Launch a game and confirm **A and B act swapped** (the confirm/cancel roles flip); toggle off and
    relaunch restores normal. The **Select+Start menu still opens** regardless of swap.
  - Regression: a controller with no swaps plays normally.
  - (If the controller is asleep, ask the user to wake it; the swap logic is unit-tested and the
    emulator-apply is a one-line keycode substitution.)

- [ ] **Step 3: RESULTS** — `docs/superpowers/plans/2026-07-01-gobe-controllers-swap-RESULTS.md`; commit.

- [ ] **Step 4: Finish** — superpowers:finishing-a-development-branch: tests pass, merge → `main`, push.

---

## Notes for the implementer

- **Pure + tested:** `remapCode` is the whole mechanic; the rest is persistence + one-line apply + toggles.
- **Raw pause combo:** Select+Start detection stays on raw `code` so the menu is unaffected by swaps.
- **Read-at-launch:** the emulator loads swaps in `onCreate`; mid-session changes need a relaunch (by design).
- **Scope:** only A/B and X/Y face-button swaps; no capture-remap, no analog, global per controller.
