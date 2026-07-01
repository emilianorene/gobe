package com.gobe.tv.emulation.input

import org.junit.Assert.assertEquals
import org.junit.Test

class DeadzoneTest {
    @Test fun belowThresholdIsZero() { assertEquals(0f, applyDeadzone(0.05f, 0.10f), 1e-6f) }
    @Test fun offIsPassthrough() { assertEquals(0.03f, applyDeadzone(0.03f, 0f), 1e-6f) }
    @Test fun atEdgeIsZero() { assertEquals(0f, applyDeadzone(0.10f, 0.10f), 1e-6f) }
    @Test fun fullTravelStaysFull() { assertEquals(1f, applyDeadzone(1f, 0.10f), 1e-6f) }
    // 0.55 with thr 0.10 -> (0.55-0.10)/(1-0.10) = 0.45/0.90 = 0.5
    @Test fun rescalesMidRange() { assertEquals(0.5f, applyDeadzone(0.55f, 0.10f), 1e-6f) }
    @Test fun preservesSign() { assertEquals(-0.5f, applyDeadzone(-0.55f, 0.10f), 1e-6f) }
    @Test fun fromNameDefaultsToLow() { assertEquals(DeadzoneLevel.LOW, deadzoneFromName(null)) }
    @Test fun fromNameParsesKnown() { assertEquals(DeadzoneLevel.HIGH, deadzoneFromName("HIGH")) }
    @Test fun fromNameUnknownFallsBack() { assertEquals(DeadzoneLevel.LOW, deadzoneFromName("bogus")) }
}
