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
