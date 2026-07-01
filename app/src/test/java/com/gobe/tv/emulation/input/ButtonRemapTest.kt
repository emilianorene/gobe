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
