# Controllers — Button Remap by Capture (sub-project C2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Full per-controller button remap by capture ("press the button for A"), persisted, applied in the emulator with precedence over the Swap presets.

**Architecture:** A pure `ButtonRemap` (physical→target keycode map) + `applyMapping(code, remap, swaps) = remap[code] ?: remapCode(code, swaps)` (unit-tested). Persisted per descriptor in `ControllerPrefs`. A capture UI in the controller detail; `ControllersActivity` runs the capture state machine in `dispatchKeyEvent`; `EmulatorActivity` applies the mapping.

**Tech Stack:** Kotlin, Compose for TV, Android `KeyEvent`/`InputDevice`, SharedPreferences, LibretroDroid, JUnit (JVM) + on-device.

**Spec:** `docs/superpowers/specs/2026-07-01-gobe-controllers-remap-design.md`

---

## Pre-flight

- Branch `feat/controllers-remap` exists (spec committed). Stay on it.
- `java`/`adb` NOT on PATH: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"`; adb `/opt/homebrew/share/android-commandlinetools/platform-tools/adb` (ONN `192.168.1.219:5555`; if offline, `adb connect`; a Pro Controller connects via BT but **sleeps on idle** — the user wakes it). Wake screen with `KEYCODE_WAKEUP`; re-grant `appops ... MANAGE_EXTERNAL_STORAGE allow` after reinstall.
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## Existing anchors

- `PadButton` enum + `keyCodeToPadButton(code): PadButton?` (`emulation/input/`).
- `ButtonSwaps` + `remapCode(code, swaps)` (`emulation/input/`).
- `ControllerPrefs` (object, `gobe.settings`): per-key `ctrl.port.<desc>` Int + `ctrl.swapab/swapxy.<desc>` bool; has `load`/`save`/`loadSwaps`/`saveSwaps` and a private `prefs(context)`.
- `EmulatorActivity`: forwards `sendKeyEvent(action, remapCode(code, swapsForInput(deviceId)), portForInput(deviceId))` (two identical sites); loads `assignments`/`swapsByDescriptor` in `onCreate`; helpers `portForInput`, `swapsForInput`.
- `ControllerDetailScreen(name, assignedPort, swaps, activeButtons, leftStick, rightStick, lastInput, onAssign, onToggleSwapAB, onToggleSwapXY)` — player selector + swap toggles + `ControllerTestPanel`.
- `ControllersActivity`: state `assignments`/`swaps`/`rows`/`selected` + test state; `dispatchKeyEvent` (Back→list/leave; else drive the test panel for the selected device, D-pad not consumed); `isSelectedDevice`, `deviceName`, `backToList`, `refreshDevices`.

## File map

- `app/src/main/java/com/gobe/tv/emulation/input/ButtonRemap.kt` — **Create**: pure remap + apply + (de)serialize.
- `app/src/test/java/com/gobe/tv/emulation/input/ButtonRemapTest.kt` — **Create**: JVM test.
- `app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt` — **Modify**: remap load/save.
- `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt` — **Modify**: apply `applyMapping`.
- `app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt` — **Modify**: Remap section in detail.
- `app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt` — **Modify**: capture state machine.
- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — **Modify**: remap strings.

---

### Task 1: `ButtonRemap` + `applyMapping` + (de)serialize (pure, TDD)

**Files:** Create `ButtonRemap.kt` + `ButtonRemapTest.kt`.

- [ ] **Step 1: Failing test** — `app/src/test/java/com/gobe/tv/emulation/input/ButtonRemapTest.kt`:
```kotlin
package com.gobe.tv.emulation.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ButtonRemapTest {
    @Test fun bindSetsAndReverseLooks() {
        val r = ButtonRemap().bind(99, KeyEvent.KEYCODE_BUTTON_A)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, r.byPhysical[99])
        assertEquals(99, r.physicalFor(KeyEvent.KEYCODE_BUTTON_A))
    }
    @Test fun bindOnePhysicalPerTarget() {
        val r = ButtonRemap().bind(99, KeyEvent.KEYCODE_BUTTON_A).bind(100, KeyEvent.KEYCODE_BUTTON_A)
        assertNull(r.byPhysical[99])
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, r.byPhysical[100])
    }
    @Test fun applyMappingCustomWinsOverSwap() {
        val r = ButtonRemap().bind(KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_A)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A,
            applyMapping(KeyEvent.KEYCODE_BUTTON_X, r, ButtonSwaps(swapAB = true)))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A,  // unbound B falls through to swap
            applyMapping(KeyEvent.KEYCODE_BUTTON_B, r, ButtonSwaps(swapAB = true)))
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y,  // unbound, no swap
            applyMapping(KeyEvent.KEYCODE_BUTTON_Y, r, ButtonSwaps()))
    }
    @Test fun unboundPhysicalKeepsDefault() {
        val r = ButtonRemap().bind(99, KeyEvent.KEYCODE_BUTTON_A)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, applyMapping(KeyEvent.KEYCODE_BUTTON_A, r, ButtonSwaps()))
    }
    @Test fun serializeRoundTrip() {
        val r = ButtonRemap().bind(99, 96).bind(100, 97)
        assertEquals(r.byPhysical, parseRemap(serializeRemap(r)).byPhysical)
    }
    @Test fun parseHandlesEmptyAndMalformed() {
        assertEquals(emptyMap<Int, Int>(), parseRemap(null).byPhysical)
        assertEquals(emptyMap<Int, Int>(), parseRemap("").byPhysical)
        assertEquals(mapOf(1 to 2), parseRemap("1:2,bad,3:").byPhysical)
    }
    @Test fun resetClears() {
        assertEquals(emptyMap<Int, Int>(), ButtonRemap().bind(1, 2).reset().byPhysical)
    }
}
```

- [ ] **Step 2: Run, verify FAIL** — `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.emulation.input.ButtonRemapTest"`.

- [ ] **Step 3: Implement** — `app/src/main/java/com/gobe/tv/emulation/input/ButtonRemap.kt`:
```kotlin
package com.gobe.tv.emulation.input

/** Per-controller custom button remap: physical Android keycode -> target keycode. Pure. */
data class ButtonRemap(val byPhysical: Map<Int, Int> = emptyMap()) {
    /** Bind physical->target, enforcing one physical per target (remove any other physical on it). */
    fun bind(physical: Int, target: Int): ButtonRemap {
        val m = byPhysical.filterValues { it != target }.toMutableMap()
        m[physical] = target
        return ButtonRemap(m)
    }
    fun reset(): ButtonRemap = ButtonRemap(emptyMap())
    /** Which physical key currently triggers this target (for the UI), or null. */
    fun physicalFor(target: Int): Int? = byPhysical.entries.firstOrNull { it.value == target }?.key
}

/** Custom binding wins; else the Swap preset; else passthrough. */
fun applyMapping(code: Int, remap: ButtonRemap, swaps: ButtonSwaps): Int =
    remap.byPhysical[code] ?: remapCode(code, swaps)

fun serializeRemap(r: ButtonRemap): String =
    r.byPhysical.entries.joinToString(",") { "${it.key}:${it.value}" }

fun parseRemap(s: String?): ButtonRemap {
    if (s.isNullOrBlank()) return ButtonRemap()
    val m = mutableMapOf<Int, Int>()
    for (part in s.split(",")) {
        val kv = part.split(":")
        if (kv.size != 2) continue
        val p = kv[0].toIntOrNull() ?: continue
        val t = kv[1].toIntOrNull() ?: continue
        m[p] = t
    }
    return ButtonRemap(m)
}
```

- [ ] **Step 4: Run, verify PASS.**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/gobe/tv/emulation/input/ButtonRemap.kt app/src/test/java/com/gobe/tv/emulation/input/ButtonRemapTest.kt
git commit -m "feat(controllers): ButtonRemap + applyMapping + (de)serialize (pure, TDD)"
```

---

### Task 2: Persist remaps in `ControllerPrefs`

**Files:** Modify `ControllerPrefs.kt`.

- [ ] **Step 1:** Add (import `com.gobe.tv.emulation.input.ButtonRemap`, `serializeRemap`, `parseRemap`):
```kotlin
    private const val REMAP = "ctrl.remap."

    fun loadRemaps(context: Context): Map<String, ButtonRemap> {
        val p = prefs(context)
        return p.all.keys.filter { it.startsWith(REMAP) }.associate { k ->
            k.removePrefix(REMAP) to parseRemap(p.getString(k, null))
        }
    }

    fun saveRemap(context: Context, descriptor: String, r: ButtonRemap) {
        prefs(context).edit().putString(REMAP + descriptor, serializeRemap(r)).apply()
    }
```

- [ ] **Step 2: Build** — `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt
git commit -m "feat(controllers): persist per-controller button remaps"
```

---

### Task 3: Apply the remap in `EmulatorActivity`

**Files:** Modify `EmulatorActivity.kt`.

- [ ] **Step 1:** Add imports (`ButtonRemap`, `applyMapping`). Add a field near `swapsByDescriptor`:
```kotlin
    private var remapsByDescriptor: Map<String, ButtonRemap> = emptyMap()
```
Load in `onCreate` (after `swapsByDescriptor = ...`):
```kotlin
        remapsByDescriptor = ControllerPrefs.loadRemaps(this)
```
Add a helper near `swapsForInput`:
```kotlin
    private fun remapForInput(deviceId: Int): ButtonRemap =
        remapsByDescriptor[InputDevice.getDevice(deviceId)?.descriptor] ?: ButtonRemap()
```

- [ ] **Step 2:** Replace BOTH forward sites
`retroView?.sendKeyEvent(event.action, remapCode(code, swapsForInput(event.deviceId)), portForInput(event.deviceId))`
with:
```kotlin
            retroView?.sendKeyEvent(event.action, applyMapping(code, remapForInput(event.deviceId), swapsForInput(event.deviceId)), portForInput(event.deviceId))
```
(`remapCode` import may become unused → remove it if the compiler flags it, since `applyMapping`
now wraps it.)

- [ ] **Step 3: Build.** Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt
git commit -m "feat(controllers): apply custom button remap (precedence over swaps) in emulator"
```

---

### Task 4: Remap UI in the controller detail

**Files:** Modify `ControllersScreen.kt`, strings (EN/ES).

- [ ] **Step 1: Strings**

`values/strings.xml`:
```xml
    <string name="controllers_remap">Remap buttons</string>
    <string name="controllers_remap_default">default</string>
    <string name="controllers_remap_press">Press a button on the controller…</string>
    <string name="controllers_remap_reset">Reset remap</string>
```
`values-es/strings.xml`:
```xml
    <string name="controllers_remap">Remapear botones</string>
    <string name="controllers_remap_default">default</string>
    <string name="controllers_remap_press">Apretá un botón del control…</string>
    <string name="controllers_remap_reset">Restablecer remapeo</string>
```

- [ ] **Step 2: Extend `ControllerDetailScreen`**

Add params:
```kotlin
    remap: com.gobe.tv.emulation.input.ButtonRemap,
    capturingTarget: Int?,
    onCaptureStart: (Int) -> Unit,
    onResetRemap: () -> Unit,
```
After the Swap toggles row, add a Remap section. Define the 10 targets (label + keycode) and render
a focusable row per target showing its current physical binding; a Reset button; a capturing banner:
```kotlin
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.controllers_remap) + ":", style = MaterialTheme.typography.titleMedium)
        if (capturingTarget != null) {
            Text(stringResource(R.string.controllers_remap_press), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        val targets = listOf(
            PadButton.A to android.view.KeyEvent.KEYCODE_BUTTON_A,
            PadButton.B to android.view.KeyEvent.KEYCODE_BUTTON_B,
            PadButton.X to android.view.KeyEvent.KEYCODE_BUTTON_X,
            PadButton.Y to android.view.KeyEvent.KEYCODE_BUTTON_Y,
            PadButton.L1 to android.view.KeyEvent.KEYCODE_BUTTON_L1,
            PadButton.R1 to android.view.KeyEvent.KEYCODE_BUTTON_R1,
            PadButton.L2 to android.view.KeyEvent.KEYCODE_BUTTON_L2,
            PadButton.R2 to android.view.KeyEvent.KEYCODE_BUTTON_R2,
            PadButton.SELECT to android.view.KeyEvent.KEYCODE_BUTTON_SELECT,
            PadButton.START to android.view.KeyEvent.KEYCODE_BUTTON_START,
        )
        // Two rows of five to keep it compact.
        targets.chunked(5).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEach { (label, targetCode) ->
                    val phys = remap.physicalFor(targetCode)
                    val bound = phys?.let { com.gobe.tv.emulation.input.keyCodeToPadButton(it)?.name ?: "#$it" }
                        ?: stringResource(R.string.controllers_remap_default)
                    Button(onClick = { onCaptureStart(targetCode) }) { Text("${label.name}: $bound") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = onResetRemap) { Text(stringResource(R.string.controllers_remap_reset)) }
```
(Imports already available: `Row`, `Arrangement`, `Button`, `Text`, `MaterialTheme`, `stringResource`,
`R`, `PadButton`, `Spacer`, `Modifier`, `dp`.)

- [ ] **Step 3: Build** — resolve imports. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(controllers): remap section (target rows + capture banner + reset) in detail"
```

---

### Task 5: Capture state machine in `ControllersActivity`

**Files:** Modify `ControllersActivity.kt`.

- [ ] **Step 1: State + load**

Add (import `ButtonRemap`):
```kotlin
    private var remaps by mutableStateOf<Map<String, ButtonRemap>>(emptyMap())
    private var capturingTarget by mutableStateOf<Int?>(null)
```
Load in `onCreate` (with the others): `remaps = ControllerPrefs.loadRemaps(this)`.
Reset `capturingTarget = null` inside `backToList()` (so leaving the detail cancels any capture).

- [ ] **Step 2: Capture branch at the TOP of `dispatchKeyEvent`**

Insert BEFORE the existing Back handling (capture takes priority). Note the DOWN-binds / UP-cancels
asymmetry is intentional (avoids a lingering Back-down):
```kotlin
        val cap = capturingTarget
        if (cap != null) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) capturingTarget = null
                return true // consume Back = cancel; never leave or bind
            }
            val sel = selected
            val pad = keyCodeToPadButton(event.keyCode)
            val eligible = pad != null && !isDpad(pad)
            if (event.action == KeyEvent.ACTION_DOWN && eligible && sel != null &&
                InputDevice.getDevice(event.deviceId)?.descriptor == sel) {
                val next = (remaps[sel] ?: ButtonRemap()).bind(event.keyCode, cap)
                ControllerPrefs.saveRemap(this, sel, next)
                remaps = remaps + (sel to next)
                capturingTarget = null
                return true // consume the bound press
            }
            // Ineligible (D-pad/unknown) during capture: don't bind; let it navigate the UI.
            return super.dispatchKeyEvent(event)
        }
```
Add a helper (used above and reuse in the existing test-panel D-pad check):
```kotlin
    private fun isDpad(p: PadButton) = p == PadButton.DPAD_UP || p == PadButton.DPAD_DOWN ||
        p == PadButton.DPAD_LEFT || p == PadButton.DPAD_RIGHT
```
(and optionally simplify the existing `isDpad` inline check in the test-panel branch to call it.)

- [ ] **Step 3: Render the detail with the remap props**

In the `ControllerDetailScreen(...)` call, add:
```kotlin
                                remap = remaps[sel] ?: ButtonRemap(),
                                capturingTarget = capturingTarget,
                                onCaptureStart = { target -> capturingTarget = target },
                                onResetRemap = {
                                    val cleared = ButtonRemap()
                                    ControllerPrefs.saveRemap(this@ControllersActivity, sel, cleared)
                                    remaps = remaps + (sel to cleared)
                                },
```

- [ ] **Step 4: Build** — resolve imports. Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt
git commit -m "feat(controllers): button-capture state machine (bind/cancel) in Controllers"
```

---

### Task 6: On-device acceptance + RESULTS + merge

- [ ] **Step 1: Unit tests + install** (`testDebugUnitTest`, `installDebug`, grant perm, wake; `adb connect` if offline).

- [ ] **Step 2: Acceptance (Pro Controller awake)**
  - Detail shows a **Remap buttons** section (10 target rows, each "default") + Reset.
  - Tap **A** → banner "Press a button…" → press a physical button → the A row shows that button;
    launch a game and confirm that physical button now acts as A.
  - **Cancel:** tap **B** → press **Back** → capture exits, B stays "default" (nothing bound), and
    Back did NOT leave the detail.
  - **Reset** clears all rows to default.
  - Unbound keys still honor the Swap presets; the Select+Start menu still opens.
  - (If the controller is asleep, ask the user to wake it; the mapping logic is unit-tested and the
    emulator apply is `applyMapping`.)

- [ ] **Step 3: RESULTS** — `docs/superpowers/plans/2026-07-01-gobe-controllers-remap-RESULTS.md`; commit.

- [ ] **Step 4: Finish** — superpowers:finishing-a-development-branch: tests pass, merge → `main`, push.

---

## Notes for the implementer

- **Capture priority:** the capture branch is first in `dispatchKeyEvent`; Back cancels (consumed,
  UP), an eligible face/shoulder/Select/Start DOWN binds, D-pad/unknown are ignored-for-binding but
  passed through so focus still moves.
- **Precedence:** `applyMapping` = custom binding wins, else swap, else passthrough (unit-tested).
- **Read-at-launch:** the emulator loads remaps in `onCreate`; mid-session changes need a relaunch.
- **Scope:** face/shoulder/Select/Start only; no D-pad/analog/per-game.
