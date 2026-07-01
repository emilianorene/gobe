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
