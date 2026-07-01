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
