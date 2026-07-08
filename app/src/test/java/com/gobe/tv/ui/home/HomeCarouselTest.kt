package com.gobe.tv.ui.home

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeCarouselTest {
    @Test fun visibleKeepsOnlyNonEmptyInSystemOrder() {
        val counts = mapOf(System.SNES to 3, System.NES to 5, System.ARCADE to 0)
        assertEquals(listOf(System.NES, System.SNES), visibleConsoles(counts).map { it.system })
    }
    @Test fun visibleCarriesCounts() {
        assertEquals(5, visibleConsoles(mapOf(System.NES to 5)).first().count)
    }
    @Test fun defaultFocusIsFirstVisibleOrNull() {
        assertEquals(System.NES, defaultFocus(visibleConsoles(mapOf(System.NES to 1, System.N64 to 2))))
        assertNull(defaultFocus(emptyList()))
    }
    @Test fun moveClampsAtEndsNoWrap() {
        val v = visibleConsoles(mapOf(System.NES to 1, System.SNES to 1, System.N64 to 1))
        assertEquals(System.SNES, moveFocus(v, System.NES, +1))
        assertEquals(System.NES, moveFocus(v, System.NES, -1))   // clamp at left
        assertEquals(System.N64, moveFocus(v, System.N64, +1))   // clamp at right
    }
    @Test fun moveFromUnknownStartsAtFirst() {
        val v = visibleConsoles(mapOf(System.NES to 1, System.SNES to 1))
        assertEquals(System.SNES, moveFocus(v, null, +1))
        assertNull(moveFocus(emptyList(), null, +1))
    }
}
