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
