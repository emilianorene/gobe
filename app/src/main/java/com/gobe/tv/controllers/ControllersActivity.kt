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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.gobe.tv.emulation.input.ButtonRemap
import com.gobe.tv.emulation.input.ButtonSwaps
import com.gobe.tv.emulation.input.ControllerAssignments
import com.gobe.tv.emulation.input.PadButton
import com.gobe.tv.emulation.input.keyCodeToPadButton
import com.gobe.tv.i18n.LocaleManager
import com.gobe.tv.ui.controllers.ControllerDetailScreen
import com.gobe.tv.ui.controllers.ControllerRow
import com.gobe.tv.ui.controllers.ControllersListScreen
import com.gobe.tv.ui.theme.GobeTheme

/**
 * Lists connected gamepads (USB/Bluetooth), lets the user assign each to a player port (P1–P4,
 * persisted by device descriptor), and test its inputs. Selecting a controller opens its detail;
 * the test panel there reacts only to that controller.
 */
class ControllersActivity : ComponentActivity() {

    private var assignments by mutableStateOf(ControllerAssignments())
    private var swaps by mutableStateOf<Map<String, ButtonSwaps>>(emptyMap())
    private var remaps by mutableStateOf<Map<String, ButtonRemap>>(emptyMap())
    private var capturingTarget by mutableStateOf<Int?>(null) // target keycode being bound; null = idle
    private var rows by mutableStateOf<List<ControllerRow>>(emptyList())
    private var selected by mutableStateOf<String?>(null) // selected descriptor; null = list view

    private var keyButtons by mutableStateOf<Set<PadButton>>(emptySet())
    private var axisButtons by mutableStateOf<Set<PadButton>>(emptySet())
    private var leftStick by mutableStateOf(0f to 0f)
    private var rightStick by mutableStateOf(0f to 0f)
    private var lastInput by mutableStateOf("—")

    private val inputManager get() = getSystemService(Context.INPUT_SERVICE) as InputManager

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()
        override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()
        override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assignments = ControllerPrefs.load(this)
        swaps = ControllerPrefs.loadSwaps(this)
        remaps = ControllerPrefs.loadRemaps(this)
        refreshDevices()
        setContent {
            GobeTheme {
                // tv-material3 Text defaults to black outside a Surface; provide onBackground.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                    ) {
                        val sel = selected
                        val row = rows.firstOrNull { it.descriptor == sel }
                        if (sel == null || row == null) {
                            ControllersListScreen(rows = rows, onSelect = { selected = it })
                        } else {
                            ControllerDetailScreen(
                                name = row.name,
                                assignedPort = assignments.portFor(sel),
                                swaps = swaps[sel] ?: ButtonSwaps(),
                                activeButtons = keyButtons + axisButtons,
                                leftStick = leftStick,
                                rightStick = rightStick,
                                lastInput = lastInput,
                                remap = remaps[sel] ?: ButtonRemap(),
                                capturingTarget = capturingTarget,
                                onAssign = { port ->
                                    assignments = assignments.assign(sel, port)
                                    ControllerPrefs.save(this@ControllersActivity, assignments)
                                    refreshDevices()
                                },
                                onToggleSwapAB = {
                                    val next = (swaps[sel] ?: ButtonSwaps()).let { it.copy(swapAB = !it.swapAB) }
                                    ControllerPrefs.saveSwaps(this@ControllersActivity, sel, next)
                                    swaps = swaps + (sel to next)
                                },
                                onToggleSwapXY = {
                                    val next = (swaps[sel] ?: ButtonSwaps()).let { it.copy(swapXY = !it.swapXY) }
                                    ControllerPrefs.saveSwaps(this@ControllersActivity, sel, next)
                                    swaps = swaps + (sel to next)
                                },
                                onCaptureStart = { target -> capturingTarget = target },
                                onResetRemap = {
                                    val cleared = ButtonRemap()
                                    ControllerPrefs.saveRemap(this@ControllersActivity, sel, cleared)
                                    remaps = remaps + (sel to cleared)
                                },
                            )
                        }
                    }
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
        // Require BOTH gamepad buttons AND joystick axes — isolates real controllers from the TV
        // remote (joystick-only) and virtual input devices (gamepad-only) seen on Android TV.
        return (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) &&
            (s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

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

    /** Only the controller currently open in the detail view drives the test panel. */
    private fun isSelectedDevice(deviceId: Int): Boolean =
        selected != null && InputDevice.getDevice(deviceId)?.descriptor == selected

    private fun backToList() {
        selected = null
        capturingTarget = null
        keyButtons = emptySet()
        axisButtons = emptySet()
        leftStick = 0f to 0f
        rightStick = 0f to 0f
    }

    private fun isDpadButton(p: PadButton) = p == PadButton.DPAD_UP || p == PadButton.DPAD_DOWN ||
        p == PadButton.DPAD_LEFT || p == PadButton.DPAD_RIGHT

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Capture mode takes priority (DOWN binds, UP-Back cancels — the asymmetry is intentional).
        val cap = capturingTarget
        if (cap != null) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) capturingTarget = null
                return true // consume Back = cancel; never leave or bind
            }
            val sel = selected
            val capPad = keyCodeToPadButton(event.keyCode)
            val eligible = capPad != null && !isDpadButton(capPad)
            if (event.action == KeyEvent.ACTION_DOWN && eligible && sel != null &&
                InputDevice.getDevice(event.deviceId)?.descriptor == sel
            ) {
                val next = (remaps[sel] ?: ButtonRemap()).bind(event.keyCode, cap)
                ControllerPrefs.saveRemap(this, sel, next)
                remaps = remaps + (sel to next)
                capturingTarget = null
                return true // consume the bound press
            }
            // Ineligible (D-pad/unknown) during capture: don't bind; let it navigate the UI.
            return super.dispatchKeyEvent(event)
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && selected != null) { backToList(); return true }
            return super.dispatchKeyEvent(event) // list view: leave the Activity
        }
        val pad = keyCodeToPadButton(event.keyCode) ?: return super.dispatchKeyEvent(event)
        if (!isSelectedDevice(event.deviceId)) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            keyButtons = keyButtons + pad
            lastInput = deviceName(event.deviceId) + " · " + pad.name
        } else if (event.action == KeyEvent.ACTION_UP) {
            keyButtons = keyButtons - pad
        }
        // Let D-pad keys still move focus; consume the rest.
        val isDpad = pad == PadButton.DPAD_UP || pad == PadButton.DPAD_DOWN ||
            pad == PadButton.DPAD_LEFT || pad == PadButton.DPAD_RIGHT
        return if (isDpad) super.dispatchKeyEvent(event) else true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!isSelectedDevice(event.deviceId)) return super.onGenericMotionEvent(event)
        val dz = 0.2f
        fun ax(a: Int) = event.getAxisValue(a)
        val hatX = ax(MotionEvent.AXIS_HAT_X); val hatY = ax(MotionEvent.AXIS_HAT_Y)
        val lt = maxOf(ax(MotionEvent.AXIS_LTRIGGER), ax(MotionEvent.AXIS_BRAKE))
        val rt = maxOf(ax(MotionEvent.AXIS_RTRIGGER), ax(MotionEvent.AXIS_GAS))
        val set = mutableSetOf<PadButton>()
        if (hatX < -0.5f) set += PadButton.DPAD_LEFT
        if (hatX > 0.5f) set += PadButton.DPAD_RIGHT
        if (hatY < -0.5f) set += PadButton.DPAD_UP
        if (hatY > 0.5f) set += PadButton.DPAD_DOWN
        if (lt > 0.5f) set += PadButton.L2
        if (rt > 0.5f) set += PadButton.R2
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
