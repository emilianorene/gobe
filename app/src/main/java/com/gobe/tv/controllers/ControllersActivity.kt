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
import com.gobe.tv.emulation.input.PadButton
import com.gobe.tv.emulation.input.keyCodeToPadButton
import com.gobe.tv.i18n.LocaleManager
import com.gobe.tv.ui.controllers.ControllersScreen
import com.gobe.tv.ui.theme.GobeTheme

/**
 * Detects connected gamepads (USB/Bluetooth) via InputManager and lets the user test their inputs:
 * pressing buttons/moving sticks highlights the on-screen panel. No persistence — port assignment
 * and remapping are separate sub-projects.
 */
class ControllersActivity : ComponentActivity() {

    private var devices by mutableStateOf<List<String>>(emptyList())
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
        // Let D-pad keys still move focus (so the screen stays navigable); consume the rest.
        val isDpad = pad == PadButton.DPAD_UP || pad == PadButton.DPAD_DOWN ||
            pad == PadButton.DPAD_LEFT || pad == PadButton.DPAD_RIGHT
        return if (isDpad) super.dispatchKeyEvent(event) else true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val dz = 0.2f
        fun ax(a: Int) = event.getAxisValue(a)
        // D-pad as HAT axis (release -> 0 clears these).
        val hatX = ax(MotionEvent.AXIS_HAT_X); val hatY = ax(MotionEvent.AXIS_HAT_Y)
        // Triggers may be axes rather than keycodes (fallback brake/gas).
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
