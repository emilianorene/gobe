# Controllers — List, Select & Player Assignment (sub-project B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure Controllers into a list → select → detail flow, let the user assign each controller to a player (P1–P4) persisted by device descriptor, and route emulator input to the assigned port (unassigned → P1).

**Architecture:** A pure `ControllerAssignments` (descriptor→port, unique-port) + `portForDevice` (unit-tested), a thin `ControllerPrefs` (per-key SharedPreferences), a refactored Controllers UI (list + detail with a reused test panel), and `EmulatorActivity` routing input by `deviceId → descriptor → port`.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Android `InputManager`/`InputDevice`, SharedPreferences (`gobe.settings`), LibretroDroid `sendKeyEvent`/`sendMotionEvent(port)`, JUnit (JVM) + on-device verification.

**Spec:** `docs/superpowers/specs/2026-07-01-gobe-controllers-players-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch `feat/controllers-players` already exists (spec committed there). Stay on it.
- **Environment gotchas:** `java`/`adb` NOT on PATH.
  - JDK: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"` before any `./gradlew`.
  - adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Device `192.168.1.219:5555` (ONN). A **Nintendo Switch Pro Controller** is connected. Wake with `adb shell input keyevent KEYCODE_WAKEUP`. Re-grant `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow` after reinstall.
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## What exists from sub-project A

- `emulation/input/PadButton.kt`: `PadButton` enum + `keyCodeToPadButton`.
- `ui/controllers/ControllersScreen.kt`: currently ONE screen = device-name list + test panel (button highlight + sticks + last-input). This task splits it.
- `controllers/ControllersActivity.kt`: enumerates gamepads (both `SOURCE_GAMEPAD` and `SOURCE_JOYSTICK`), `InputDeviceListener` hotplug, overrides `dispatchKeyEvent`/`onGenericMotionEvent` → `keyButtons`/`axisButtons`/`leftStick`/`rightStick`/`lastInput` state, renders `ControllersScreen`. Provides `LocalContentColor` for legible text.
- `EmulatorActivity`: forwards input with hardcoded port `0` (`sendKeyEvent(action, code, 0)` twice; three `sendMotionEvent(source, x, y, 0)`).

## File map

- `app/src/main/java/com/gobe/tv/emulation/input/ControllerAssignments.kt` — **Create**: pure map + `portForDevice`.
- `app/src/test/java/com/gobe/tv/emulation/input/ControllerAssignmentsTest.kt` — **Create**: JVM test.
- `app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt` — **Create**: SharedPreferences load/save.
- `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt` — **Modify**: route by port.
- `app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt` — **Modify**: split into list + detail + extracted `ControllerTestPanel`.
- `app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt` — **Modify**: list/detail nav, per-device test filter, load/save assignments.
- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — **Modify**: player-selector strings.

---

### Task 1: `ControllerAssignments` + `portForDevice` (pure, TDD)

**Files:**
- Create: `app/src/test/java/com/gobe/tv/emulation/input/ControllerAssignmentsTest.kt`
- Create: `app/src/main/java/com/gobe/tv/emulation/input/ControllerAssignments.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gobe.tv.emulation.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControllerAssignmentsTest {
    @Test fun assignSetsPort() {
        val a = ControllerAssignments().assign("dev-A", 1)
        assertEquals(1, a.portFor("dev-A"))
    }
    @Test fun assignEnforcesUniquePort() {
        val a = ControllerAssignments().assign("dev-A", 0).assign("dev-B", 0)
        assertNull(a.portFor("dev-A"))       // A bumped off port 0
        assertEquals(0, a.portFor("dev-B"))
    }
    @Test fun reassigningSameDeviceMovesIt() {
        val a = ControllerAssignments().assign("dev-A", 0).assign("dev-A", 2)
        assertEquals(2, a.portFor("dev-A"))
    }
    @Test fun clearRemoves() {
        val a = ControllerAssignments().assign("dev-A", 2).clear("dev-A")
        assertNull(a.portFor("dev-A"))
    }
    @Test fun portForDeviceDefaultsToP1() {
        val a = ControllerAssignments().assign("dev-A", 3)
        assertEquals(3, portForDevice("dev-A", a))
        assertEquals(0, portForDevice("dev-Z", a))  // unknown -> P1
        assertEquals(0, portForDevice(null, a))      // null (unresolvable deviceId) -> P1
    }
}
```

- [ ] **Step 2: Run the test, verify it fails**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.emulation.input.ControllerAssignmentsTest"
```
Expected: FAIL — unresolved references.

- [ ] **Step 3: Create the pure code**

`ControllerAssignments.kt`:
```kotlin
package com.gobe.tv.emulation.input

/** Which controller (by stable device descriptor) drives which 0-based player port. Pure. */
data class ControllerAssignments(val byDescriptor: Map<String, Int> = emptyMap()) {
    /** Assign a descriptor to a port, removing any OTHER descriptor currently on that port. */
    fun assign(descriptor: String, port: Int): ControllerAssignments {
        val m = byDescriptor.filterValues { it != port }.toMutableMap()
        m[descriptor] = port
        return ControllerAssignments(m)
    }
    fun clear(descriptor: String): ControllerAssignments =
        ControllerAssignments(byDescriptor - descriptor)
    fun portFor(descriptor: String): Int? = byDescriptor[descriptor]
}

/** The port a device's input should go to: its assignment, or P1 (0) by default.
 *  descriptor is null when a runtime deviceId can't be resolved — that maps to P1. */
fun portForDevice(descriptor: String?, assignments: ControllerAssignments): Int =
    descriptor?.let { assignments.portFor(it) } ?: 0
```

- [ ] **Step 4: Run the test, verify it passes**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/input/ControllerAssignments.kt app/src/test/java/com/gobe/tv/emulation/input/ControllerAssignmentsTest.kt
git commit -m "feat(controllers): ControllerAssignments + portForDevice (pure, TDD)"
```

---

### Task 2: `ControllerPrefs` (SharedPreferences)

**Files:**
- Create: `app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt`

Per-key storage (descriptors are opaque vendor strings; per-key avoids delimiter issues).

- [ ] **Step 1: Create the store**

```kotlin
package com.gobe.tv.controllers

import android.content.Context
import com.gobe.tv.emulation.input.ControllerAssignments

/** Persists ControllerAssignments in the shared gobe.settings prefs, one key per descriptor. */
object ControllerPrefs {
    private const val PREFS = "gobe.settings"
    private const val PREFIX = "ctrl.port."

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): ControllerAssignments {
        val map = prefs(context).all
            .filterKeys { it.startsWith(PREFIX) }
            .mapNotNull { (k, v) -> (v as? Int)?.let { k.removePrefix(PREFIX) to it } }
            .toMap()
        return ControllerAssignments(map)
    }

    fun save(context: Context, a: ControllerAssignments) {
        val e = prefs(context).edit()
        prefs(context).all.keys.filter { it.startsWith(PREFIX) }.forEach { e.remove(it) }
        a.byDescriptor.forEach { (desc, port) -> e.putInt(PREFIX + desc, port) }
        e.apply()
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/controllers/ControllerPrefs.kt
git commit -m "feat(controllers): ControllerPrefs — persist assignments by descriptor"
```

---

### Task 3: Route emulator input by assigned port

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt`

This is safe to ship before the UI: with no assignments, every device resolves to P1 (port 0) —
identical to today's behavior.

- [ ] **Step 1: Load assignments + add a port helper**

Add imports: `android.view.InputDevice`, `com.gobe.tv.controllers.ControllerPrefs`,
`com.gobe.tv.emulation.input.ControllerAssignments`, `com.gobe.tv.emulation.input.portForDevice`.
Add a field and load it in `onCreate` (after `args` is set):
```kotlin
    private var assignments = ControllerAssignments()
```
In `onCreate` (e.g. right after `args = EmulatorArgs.fromIntent(intent)`):
```kotlin
        assignments = ControllerPrefs.load(this)
```
Add a helper:
```kotlin
    private fun portForInput(deviceId: Int): Int =
        portForDevice(InputDevice.getDevice(deviceId)?.descriptor, assignments)
```

- [ ] **Step 2: Replace the hardcoded ports**

In `dispatchKeyEvent`, both `retroView?.sendKeyEvent(event.action, code, 0)` calls become:
```kotlin
            retroView?.sendKeyEvent(event.action, code, portForInput(event.deviceId))
```
In `onGenericMotionEvent`, the three `sendMotionEvent(source, x, y, 0)` calls take
`portForInput(event.deviceId)` as the last arg instead of `0`.

- [ ] **Step 3: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt
git commit -m "feat(controllers): route emulator input to each controller's assigned port"
```

---

### Task 4: Split Controllers UI into list + detail (+ extracted test panel)

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt`
- Modify: `app/src/main/res/values/strings.xml` + `values-es/strings.xml`

- [ ] **Step 1: Add player-selector strings**

`values/strings.xml`:
```xml
    <string name="controllers_player">Player</string>
    <string name="controllers_unassigned">Not assigned</string>
    <string name="controllers_select_hint">Select a controller</string>
```
`values-es/strings.xml`:
```xml
    <string name="controllers_player">Jugador</string>
    <string name="controllers_unassigned">Sin asignar</string>
    <string name="controllers_select_hint">Seleccioná un control</string>
```

- [ ] **Step 2: Refactor `ControllersScreen.kt`**

Replace the file's single `ControllersScreen` with three composables (keep `PadButton` import and
the existing `PadChip`):

1. `ControllerRow` model + **list screen**:
```kotlin
data class ControllerRow(val descriptor: String, val name: String, val port: Int?)

@Composable
fun ControllersListScreen(rows: List<ControllerRow>, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(stringResource(R.string.controllers_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (rows.isEmpty()) {
            Text(stringResource(R.string.controllers_none), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.controllers_select_hint), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            rows.forEachIndexed { i, r ->
                val label = r.name + "   " +
                    (r.port?.let { "P${it + 1}" } ?: stringResource(R.string.controllers_unassigned))
                Button(
                    onClick = { onSelect(r.descriptor) },
                    modifier = if (i == 0) Modifier.focusRequester(remember { FocusRequester() }.also {
                        // request focus on the first row so a gamepad can navigate immediately
                    }) else Modifier,
                ) { Text(label) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
```
(Use `androidx.tv.material3.Button`; add imports for `Button`, `FocusRequester`, `focusRequester`,
`remember`. Initial-focus can be simplified: give the first row a `FocusRequester` and
`LaunchedEffect(Unit){ requestFocus() }` like other screens — keep it compiling and focusable.)

2. **Extract the test panel** (from the old `ControllersScreen` body) into:
```kotlin
@Composable
fun ControllerTestPanel(
    activeButtons: Set<PadButton>,
    leftStick: Pair<Float, Float>,
    rightStick: Pair<Float, Float>,
    lastInput: String,
) { /* the button-rows grid + sticks + last-input from the old ControllersScreen */ }
```

3. **Detail screen** = name + player selector + test panel:
```kotlin
@Composable
fun ControllerDetailScreen(
    name: String,
    assignedPort: Int?,
    activeButtons: Set<PadButton>,
    leftStick: Pair<Float, Float>,
    rightStick: Pair<Float, Float>,
    lastInput: String,
    onAssign: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.controllers_player) + ":", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0..3).forEach { p ->
                val selected = assignedPort == p
                Button(onClick = { onAssign(p) }) { Text(if (selected) "● P${p + 1}" else "P${p + 1}") }
            }
        }
        Spacer(Modifier.height(24.dp))
        ControllerTestPanel(activeButtons, leftStick, rightStick, lastInput)
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL. Resolve imports.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(controllers): split UI into controller list + detail (assign + test panel)"
```

---

### Task 5: `ControllersActivity` — list/detail nav, assignment, per-device test

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt`

- [ ] **Step 1: Add assignment state + descriptors to the enumeration**

- Load assignments: `private var assignments by mutableStateOf(ControllerPrefs.load(this))` (set in
  `onCreate`, or `remember` equivalent — a plain field works since it drives Compose state).
  Simplest: `private var assignments by mutableStateOf(ControllerAssignments())` and
  `assignments = ControllerPrefs.load(this)` in `onCreate`.
- Change `refreshDevices()` to build `List<ControllerRow>` (descriptor + name + assigned port):
```kotlin
    private var rows by mutableStateOf<List<ControllerRow>>(emptyList())
    private fun refreshDevices() {
        val list = mutableListOf<ControllerRow>()
        for (id in InputDevice.getDeviceIds()) {
            val dev = InputDevice.getDevice(id) ?: continue
            if (!isGamepad(dev)) continue
            val desc = dev.descriptor
            list.add(ControllerRow(desc, dev.name, assignments.portFor(desc)))
        }
        rows = list
    }
```
- Track selection: `private var selected by mutableStateOf<String?>(null)` (the selected descriptor,
  null = list view).

- [ ] **Step 2: Filter the test state to the selected controller**

In `dispatchKeyEvent`/`onGenericMotionEvent`, only update the test state
(`keyButtons`/`axisButtons`/sticks/`lastInput`) when `selected != null` AND the event's device
descriptor equals `selected`:
```kotlin
    private fun isSelectedDevice(deviceId: Int): Boolean =
        selected != null && InputDevice.getDevice(deviceId)?.descriptor == selected
```
Guard the state updates with `if (!isSelectedDevice(event.deviceId)) return super.dispatchKeyEvent(event)`
(after the Back check) — i.e. only capture for the controller being tested. In list view (`selected
== null`), don't capture (nothing to highlight).

- [ ] **Step 3: Render list vs detail; wire assign/back**

```kotlin
        setContent {
            GobeTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                        val sel = selected
                        val row = rows.firstOrNull { it.descriptor == sel }
                        if (sel == null || row == null) {
                            ControllersListScreen(rows = rows, onSelect = { selected = it })
                        } else {
                            ControllerDetailScreen(
                                name = row.name,
                                assignedPort = assignments.portFor(sel),
                                activeButtons = keyButtons + axisButtons,
                                leftStick = leftStick, rightStick = rightStick, lastInput = lastInput,
                                onAssign = { port ->
                                    assignments = assignments.assign(sel, port)
                                    ControllerPrefs.save(this@ControllersActivity, assignments)
                                    refreshDevices()
                                },
                            )
                        }
                    }
                }
            }
        }
```
- **Back handling** in `dispatchKeyEvent`: if `KEYCODE_BACK` and `selected != null`, clear
  `selected` (return to list) and consume; else `super` (leave the Activity). Add at the top of
  `dispatchKeyEvent`:
```kotlin
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && selected != null) { selected = null; return true }
            return super.dispatchKeyEvent(event)
        }
```
- **Disconnect while in detail:** if `row == null` (selected descriptor no longer present), the code
  above falls back to the list automatically. Good.
- Clear stale test highlights when leaving detail: when `selected` becomes null, also reset
  `keyButtons`/`axisButtons` to empty (so a re-entry starts clean).

- [ ] **Step 4: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL. Resolve imports
(`CompositionLocalProvider`, `ControllerRow`, `ControllersListScreen`, `ControllerDetailScreen`,
`ControllerPrefs`, `ControllerAssignments`).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt
git commit -m "feat(controllers): list->detail nav, player assignment, per-device test"
```

---

### Task 6: On-device acceptance + RESULTS + merge

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
Expected: `ControllerAssignmentsTest` + existing tests green; app opens.

- [ ] **Step 2: Acceptance checklist (screenshot each; wake first)**

  - Settings → Controllers shows the **list** with the connected controller + its player label
    ("Not assigned" initially). Selecting it opens the **detail** (name + P1–P4 selector + test panel).
  - Assigning **P2** shows "● P2"; going Back to the list shows "P2" on the row; **reopening the app
    and the screen still shows P2** (persistence).
  - In the detail, the **test panel highlights that controller's buttons** (from A).
  - Launch a game: the controller still works (assigned or default P1). With two controllers,
    each drives its assigned port in a 2-player game (best-effort — if only one pad is available,
    verify the single pad works whether assigned to P1 or unassigned, and note in RESULTS).
  - Back from detail → list; Back from list → Settings.

- [ ] **Step 3: Write + commit RESULTS**

Create `docs/superpowers/plans/2026-07-01-gobe-controllers-players-RESULTS.md`: what shipped, the
controller(s) tested, persistence confirmed, routing notes, screenshots referenced. Commit.

- [ ] **Step 4: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, merge
`feat/controllers-players` → `main`, push.

---

## Notes for the implementer

- **Pure vs Android split:** `ControllerAssignments`/`portForDevice` are pure + tested; `ControllerPrefs`
  is thin serialization (verified on-device via the persistence check).
- **Safe routing:** unassigned → P1 (port 0) keeps single-player identical to today.
- **Per-device test filter:** only the selected controller drives the highlight panel (no cross-talk).
- **Descriptor caveats** (empty/duplicate for identical pads) are a documented limitation; the P1
  default keeps input working regardless.
- **Scope is B** — no button remapping (that's C), no per-game assignments, no rumble/deadzone/swap.
