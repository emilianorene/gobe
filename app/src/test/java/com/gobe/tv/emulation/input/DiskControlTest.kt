package com.gobe.tv.emulation.input

import org.junit.Assert.assertEquals
import org.junit.Test

class DiskControlTest {
    @Test fun wrapsToZeroAtEnd() { assertEquals(0, nextDisk(1, 2)) }
    @Test fun incrementsMidRange() { assertEquals(2, nextDisk(1, 4)) }
    @Test fun singleDiskIsNoOp() { assertEquals(0, nextDisk(0, 1)) }
    @Test fun zeroCountIsNoOp() { assertEquals(3, nextDisk(3, 0)) }
    @Test fun negativeCurrentStaysInRange() { assertEquals(0, nextDisk(-1, 3)) }
}
