# Controllers — Detection + Test (sub-project A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A "Controllers" screen (launched from Settings) that lists connected USB/Bluetooth gamepads live and a test panel that highlights each button/stick as it's pressed.

**Architecture:** A pure `keyCodeToPadButton` mapper (unit-tested) + a dedicated `ControllersActivity` that enumerates gamepads via `InputManager`, live-updates via `InputDeviceListener`, and captures input by overriding `dispatchKeyEvent`/`onGenericMotionEvent` (mirroring `EmulatorActivity`), feeding a Compose `ControllersScreen`. No persistence (ports/remap are sub-projects B/C).

**Tech Stack:** Kotlin, Jetpack Compose for TV, Android `InputManager`/`InputDevice`, JUnit (JVM) + on-device verification over wireless adb.

**Spec:** `docs/superpowers/specs/2026-07-01-gobe-controllers-detect-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch `feat/controllers-detect` already exists (spec committed there). Stay on it.
- **Environment gotchas** (subagents MUST know): `java`/`adb` NOT on PATH.
  - JDK: `export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"` before any `./gradlew`.
  - adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`. Device `192.168.1.219:5555` (ONN). Wake with `adb shell input keyevent KEYCODE_WAKEUP`. Re-grant `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow` after reinstall.
- Repo root: `/Users/emilianogonzalez/Documents/Claude Projects/gobe-games`.

## File map

- `app/src/main/java/com/gobe/tv/emulation/input/PadButton.kt` — **Create**: `PadButton` enum + pure `keyCodeToPadButton`. Compose-free (clean JVM test).
- `app/src/test/java/com/gobe/tv/emulation/input/PadButtonTest.kt` — **Create**: JVM unit test.
- `app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt` — **Create**: Compose UI (device list + highlight panel + sticks + last-event + Back).
- `app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt` — **Create**: input capture + enumeration + listener; renders the screen.
- `app/src/main/AndroidManifest.xml` — **Modify**: register `ControllersActivity` (exported=false).
- `app/src/main/java/com/gobe/tv/ui/settings/SettingsScreen.kt` — **Modify**: a "Controllers" button that starts the Activity.
- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — **Modify**: screen strings.

Existing facts: `SettingsScreen(onOpenFolders, onBack)` is a `Column` of Buttons; `val context = LocalContext.current` is available; other activities use `attachBaseContext(LocaleManager.wrap(newBase))`; `EmulatorActivity` reads `AXIS_HAT_X/Y` (D-pad), `AXIS_X/Y` (left stick), `AXIS_Z/RZ` (right stick).

---

### Task 1: `PadButton` enum + `keyCodeToPadButton` (TDD)

**Files:**
- Create: `app/src/test/java/com/gobe/tv/emulation/input/PadButtonTest.kt`
- Create: `app/src/main/java/com/gobe/tv/emulation/input/PadButton.kt`

- [ ] **Step 1: Write the failing test**

`PadButtonTest.kt`:
```kotlin
package com.gobe.tv.emulation.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PadButtonTest {
    @Test fun mapsFaceButtons() {
        assertEquals(PadButton.A, keyCodeToPadButton(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(PadButton.B, keyCodeToPadButton(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(PadButton.START, keyCodeToPadButton(KeyEvent.KEYCODE_BUTTON_START))
    }
    @Test fun mapsDpadKeys() {
        assertEquals(PadButton.DPAD_UP, keyCodeToPadButton(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(PadButton.DPAD_RIGHT, keyCodeToPadButton(KeyEvent.KEYCODE_DPAD_RIGHT))
    }
    @Test fun nonGamepadKeysAreNull() {
        assertNull(keyCodeToPadButton(KeyEvent.KEYCODE_A))
        assertNull(keyCodeToPadButton(KeyEvent.KEYCODE_BACK))
    }
}
```

- [ ] **Step 2: Run the test, verify it fails**

```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
cd "/Users/emilianogonzalez/Documents/Claude Projects/gobe-games"
./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.emulation.input.PadButtonTest"
```
Expected: FAIL — `PadButton`/`keyCodeToPadButton` unresolved.

- [ ] **Step 3: Create the enum + mapper**

`PadButton.kt`:
```kotlin
package com.gobe.tv.emulation.input

import android.view.KeyEvent

/** A logical gamepad button, for the controller test UI. */
enum class PadButton {
    A, B, X, Y, L1, R1, L2, R2, L3, R3, SELECT, START,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
}

/** Android gamepad key code -> PadButton, or null for non-gamepad keys. Pure. */
fun keyCodeToPadButton(keyCode: Int): PadButton? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_A -> PadButton.A
    KeyEvent.KEYCODE_BUTTON_B -> PadButton.B
    KeyEvent.KEYCODE_BUTTON_X -> PadButton.X
    KeyEvent.KEYCODE_BUTTON_Y -> PadButton.Y
    KeyEvent.KEYCODE_BUTTON_L1 -> PadButton.L1
    KeyEvent.KEYCODE_BUTTON_R1 -> PadButton.R1
    KeyEvent.KEYCODE_BUTTON_L2 -> PadButton.L2
    KeyEvent.KEYCODE_BUTTON_R2 -> PadButton.R2
    KeyEvent.KEYCODE_BUTTON_THUMBL -> PadButton.L3
    KeyEvent.KEYCODE_BUTTON_THUMBR -> PadButton.R3
    KeyEvent.KEYCODE_BUTTON_SELECT -> PadButton.SELECT
    KeyEvent.KEYCODE_BUTTON_START -> PadButton.START
    KeyEvent.KEYCODE_DPAD_UP -> PadButton.DPAD_UP
    KeyEvent.KEYCODE_DPAD_DOWN -> PadButton.DPAD_DOWN
    KeyEvent.KEYCODE_DPAD_LEFT -> PadButton.DPAD_LEFT
    KeyEvent.KEYCODE_DPAD_RIGHT -> PadButton.DPAD_RIGHT
    else -> null
}
```

- [ ] **Step 4: Run the test, verify it passes**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/input/PadButton.kt app/src/test/java/com/gobe/tv/emulation/input/PadButtonTest.kt
git commit -m "feat(controllers): PadButton enum + keyCodeToPadButton mapper (TDD)"
```

---

### Task 2: Strings (EN + ES)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Add strings**

`values/strings.xml` (near the `settings_*` block):
```xml
    <string name="settings_controllers">Controllers</string>
    <string name="controllers_title">Controllers</string>
    <string name="controllers_none">No controllers detected — connect a USB or Bluetooth gamepad.</string>
    <string name="controllers_connected">Connected</string>
    <string name="controllers_test">Press buttons to test</string>
    <string name="controllers_last">Last input</string>
    <string name="controllers_sticks">Sticks</string>
    <string name="controllers_back">Back</string>
```
`values-es/strings.xml`:
```xml
    <string name="settings_controllers">Controles</string>
    <string name="controllers_title">Controles</string>
    <string name="controllers_none">No se detectan controles — conectá un gamepad USB o Bluetooth.</string>
    <string name="controllers_connected">Conectados</string>
    <string name="controllers_test">Apretá botones para probar</string>
    <string name="controllers_last">Última entrada</string>
    <string name="controllers_sticks">Sticks</string>
    <string name="controllers_back">Volver</string>
```

- [ ] **Step 2: Build to verify resources compile**

Run `./gradlew :app:assembleDebug` (with JAVA_HOME). Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(controllers): screen strings (EN/ES)"
```

---

### Task 3: `ControllersScreen` (Compose UI)

**Files:**
- Create: `app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt`

Pure UI: given state, render it. No Android input logic here (that's Task 4). Verified on-device.

- [ ] **Step 1: Create the composable**

```kotlin
package com.gobe.tv.ui.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gobe.tv.R
import com.gobe.tv.emulation.input.PadButton

/** Stateless controller detection + test UI. The Activity supplies the live state. */
@Composable
fun ControllersScreen(
    devices: List<String>,          // display names of connected gamepads
    activeButtons: Set<PadButton>,  // currently-pressed buttons (keys + axis-derived)
    leftStick: Pair<Float, Float>,
    rightStick: Pair<Float, Float>,
    lastInput: String,              // e.g. "8BitDo Pro 2 · A"
) {
    Column(Modifier.fillMaxSize().padding(48.dp)) {
        Text(stringResource(R.string.controllers_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Connected list / empty state.
        if (devices.isEmpty()) {
            Text(stringResource(R.string.controllers_none), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.controllers_connected) + ":", style = MaterialTheme.typography.titleMedium)
            devices.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.controllers_test), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        // Button panel: rows of labels that light up when active.
        val rows = listOf(
            listOf(PadButton.A, PadButton.B, PadButton.X, PadButton.Y),
            listOf(PadButton.L1, PadButton.R1, PadButton.L2, PadButton.R2),
            listOf(PadButton.SELECT, PadButton.START, PadButton.L3, PadButton.R3),
            listOf(PadButton.DPAD_UP, PadButton.DPAD_DOWN, PadButton.DPAD_LEFT, PadButton.DPAD_RIGHT),
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b -> PadChip(b.name, b in activeButtons) }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.controllers_sticks) + ":  L (%.2f, %.2f)   R (%.2f, %.2f)"
                .format(leftStick.first, leftStick.second, rightStick.first, rightStick.second),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.controllers_last) + ": " + lastInput, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PadChip(label: String, active: Boolean) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color(0xFF2A2F3A)
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else Color.White
    Text(
        label,
        color = fg,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(vertical = 10.dp),
    )
}
```
(Note the `.format(...)` is applied to the string-resource template — simpler: build the sticks
string with `String.format` around `stringResource`. If the inline `.format` on a resource is
awkward, compute the numbers into a plain `"L (%.2f, %.2f) …".format(...)` and concatenate. Keep it
compiling.)

- [ ] **Step 2: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL. Fix imports/format as needed.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/controllers/ControllersScreen.kt
git commit -m "feat(controllers): ControllersScreen UI (device list + highlight panel + sticks)"
```

---

### Task 4: `ControllersActivity` (input capture + enumeration) + manifest

**Files:**
- Create: `app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

Reviewer-driven details baked in: **D-pad arrives as keys OR as the `AXIS_HAT_X/Y` axis** (both feed
DPAD_*, and the HAT returning to 0 must clear them); **triggers L2/R2 may be axes** (`AXIS_LTRIGGER`/
`AXIS_RTRIGGER`, fallback `AXIS_BRAKE`/`AXIS_GAS`) not keycodes; **sticks need a small deadzone** so a
resting stick isn't shown as active. Two state sets are unioned: key-held buttons + axis-derived
buttons.

- [ ] **Step 1: Create the Activity**

```kotlin
package com.gobe.tv.controllers

import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import com.gobe.tv.i18n.LocaleManager
import com.gobe.tv.emulation.input.PadButton
import com.gobe.tv.emulation.input.keyCodeToPadButton
import com.gobe.tv.ui.controllers.ControllersScreen
import com.gobe.tv.ui.theme.GobeTheme

class ControllersActivity : ComponentActivity() {

    private var devices by mutableStateOf<List<String>>(emptyList())
    private var keyButtons by mutableStateOf<Set<PadButton>>(emptySet())
    private var axisButtons by mutableStateOf<Set<PadButton>>(emptySet())
    private var leftStick by mutableStateOf(0f to 0f)
    private var rightStick by mutableStateOf(0f to 0f)
    private var lastInput by mutableStateOf("—")

    private val inputManager get() = getSystemService(Context.INPUT_SERVICE) as InputManager

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) { refreshDevices() }
        override fun onInputDeviceRemoved(deviceId: Int) { refreshDevices() }
        override fun onInputDeviceChanged(deviceId: Int) { refreshDevices() }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshDevices()
        setContent {
            GobeTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ControllersScreen(
                        devices = devices,
                        activeButtons = keyButtons + axisButtons,
                        leftStick = leftStick,
                        rightStick = rightStick,
                        lastInput = lastInput,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inputManager.registerInputDeviceListener(deviceListener, null)
        refreshDevices()
    }

    override fun onPause() {
        inputManager.unregisterInputDeviceListener(deviceListener)
        super.onPause()
    }

    private fun isGamepad(dev: InputDevice?): Boolean {
        val s = dev?.sources ?: return false
        return (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
            (s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    private fun refreshDevices() {
        devices = InputDevice.getDeviceIds()
            .mapNotNull { InputDevice.getDevice(it) }
            .filter { isGamepad(it) }
            .map { it.name }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event) // leave
        val pad = keyCodeToPadButton(event.keyCode) ?: return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            keyButtons = keyButtons + pad
            lastInput = deviceName(event.deviceId) + " · " + pad.name
        } else if (event.action == KeyEvent.ACTION_UP) {
            keyButtons = keyButtons - pad
        }
        // Let D-pad keys still move focus (so the on-screen UI stays navigable); consume the rest.
        val isDpad = pad == PadButton.DPAD_UP || pad == PadButton.DPAD_DOWN ||
            pad == PadButton.DPAD_LEFT || pad == PadButton.DPAD_RIGHT
        return if (isDpad) super.dispatchKeyEvent(event) else true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val dz = 0.2f
        fun ax(a: Int) = event.getAxisValue(a)
        // D-pad as HAT axis (release -> 0 clears).
        val hatX = ax(MotionEvent.AXIS_HAT_X); val hatY = ax(MotionEvent.AXIS_HAT_Y)
        // Triggers as axes (fallback to brake/gas).
        val lt = maxOf(ax(MotionEvent.AXIS_LTRIGGER), ax(MotionEvent.AXIS_BRAKE))
        val rt = maxOf(ax(MotionEvent.AXIS_RTRIGGER), ax(MotionEvent.AXIS_GAS))
        val set = mutableSetOf<PadButton>()
        if (hatX < -0.5f) set += PadButton.DPAD_LEFT; if (hatX > 0.5f) set += PadButton.DPAD_RIGHT
        if (hatY < -0.5f) set += PadButton.DPAD_UP;   if (hatY > 0.5f) set += PadButton.DPAD_DOWN
        if (lt > 0.5f) set += PadButton.L2; if (rt > 0.5f) set += PadButton.R2
        axisButtons = set
        val lx = ax(MotionEvent.AXIS_X); val ly = ax(MotionEvent.AXIS_Y)
        val rx = ax(MotionEvent.AXIS_Z); val ry = ax(MotionEvent.AXIS_RZ)
        leftStick = (if (kotlin.math.abs(lx) < dz) 0f else lx) to (if (kotlin.math.abs(ly) < dz) 0f else ly)
        rightStick = (if (kotlin.math.abs(rx) < dz) 0f else rx) to (if (kotlin.math.abs(ry) < dz) 0f else ry)
        return true
    }

    private fun deviceName(deviceId: Int): String =
        InputDevice.getDevice(deviceId)?.name ?: "device $deviceId"
}
```
Notes: `mutableStateOf` + `getValue`/`setValue` imports are included. If any `InputDevice.getDevice`
overload is deprecated on the target SDK, the current call is fine for minSdk 30. If the compiler
flags `AXIS_BRAKE`/`AXIS_GAS`, they exist on `MotionEvent`; keep them.

- [ ] **Step 2: Register the Activity in the manifest**

In `AndroidManifest.xml`, beside `EmulatorActivity`, add:
```xml
        <activity
            android:name=".controllers.ControllersActivity"
            android:exported="false"
            android:screenOrientation="landscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout|uiMode" />
```

- [ ] **Step 3: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/controllers/ControllersActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat(controllers): ControllersActivity — live device list + input test capture"
```

---

### Task 5: Settings entry to launch it

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Add a "Controllers" button**

After the existing ROM-folders button (line ~55), add:
```kotlin
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            context.startActivity(android.content.Intent(context, com.gobe.tv.controllers.ControllersActivity::class.java))
        }) { Text(stringResource(R.string.settings_controllers)) }
```
(`context` is already `LocalContext.current` in `SettingsScreen`.)

- [ ] **Step 2: Build to verify it compiles**

Run `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/settings/SettingsScreen.kt
git commit -m "feat(controllers): launch Controllers screen from Settings"
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
Expected: `PadButtonTest` green; app opens.

- [ ] **Step 2: Acceptance checklist (screenshot each; wake first)**

  - Settings → **Controllers** opens the screen.
  - With no gamepad: the **empty state** shows. Connect a USB/Bluetooth gamepad → it **appears in the
    list** (and disappears on disconnect). (The ONN remote is correctly NOT listed as a gamepad.)
  - In the test panel, pressing **A/B/X/Y, L1/R1, Select/Start** highlights the matching chip; the
    **D-pad** highlights (whether the pad sends keys or a HAT axis); **L2/R2** highlight (whether
    keycodes or trigger axes); moving the **sticks** updates the readout and a resting stick reads
    ~0 (deadzone); the **last-input** line shows the device name + button.
  - **Back** (remote Back) returns to Settings.
  - Note (soften if needed): on some pads a face button may report a non-standard code — it then
    won't light a chip but the raw device/last-input still updates; that's acceptable for A.

  If you don't have a physical gamepad on hand, drive what you can with the ONN remote (D-pad/Back)
  and note the buttons that need a real pad in RESULTS.

- [ ] **Step 3: Write + commit RESULTS**

Create `docs/superpowers/plans/2026-07-01-gobe-controllers-detect-RESULTS.md`: what shipped, which
controller(s) tested + how they appeared, any keycode/axis quirks observed, screenshots referenced.
Commit.

- [ ] **Step 4: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, merge
`feat/controllers-detect` → `main`, push.

---

## Notes for the implementer

- **Compose-free mapper:** keep `PadButton.kt` free of Compose imports so its JVM test stays clean.
- **Two state sets** (`keyButtons` ∪ `axisButtons`) cleanly handle D-pad-as-key-or-HAT and
  L2/R2-as-key-or-axis; the union is what the UI highlights.
- **Don't consume Back** — it's the way out. D-pad keys are NOT consumed (so focus/nav still works);
  face/shoulder keys are consumed so they don't leak elsewhere.
- **Scope is A only** — no port assignment, no remap, no persistence (those are B/C).
