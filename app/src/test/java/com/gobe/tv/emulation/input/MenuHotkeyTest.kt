package com.gobe.tv.emulation.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuHotkeyTest {
    @Test fun matchesWhenAllHeld() {
        val held = setOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)
        assertTrue(isHotkeyCombo(held, MenuHotkey.L1_R1))
    }
    @Test fun falseWhenPartiallyHeld() {
        assertFalse(isHotkeyCombo(setOf(KeyEvent.KEYCODE_BUTTON_L1), MenuHotkey.L1_R1))
    }
    @Test fun falseWhenEmpty() {
        assertFalse(isHotkeyCombo(emptySet(), MenuHotkey.SELECT_START))
    }
    @Test fun doesNotMatchDifferentCombo() {
        val held = setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START)
        assertFalse(isHotkeyCombo(held, MenuHotkey.L1_R1))
    }
    @Test fun fromNameDefaultsToSelectStart() {
        assertEquals(MenuHotkey.SELECT_START, menuHotkeyFromName(null))
        assertEquals(MenuHotkey.SELECT_START, menuHotkeyFromName("bogus"))
    }
    @Test fun fromNameParsesKnown() {
        assertEquals(MenuHotkey.L1_R1, menuHotkeyFromName("L1_R1"))
    }
}
