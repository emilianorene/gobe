package com.gobe.tv.emulation

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveStateStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun store() = SaveStateStore(tmp.root)

    @Test fun noStateInitially() = assertFalse(store().hasState(42))

    @Test fun writeThenReadRoundTrips() {
        val s = store()
        val bytes = byteArrayOf(1, 2, 3, 4)
        s.writeState(42, bytes)
        assertTrue(s.hasState(42))
        assertArrayEquals(bytes, s.readState(42))
    }

    @Test fun writeIsAtomicNoTmpLeftBehind() {
        val s = store()
        s.writeState(7, byteArrayOf(9))
        val states = java.io.File(tmp.root, "states").list()?.toList() ?: emptyList()
        assertTrue(states.contains("7.state"))
        assertFalse(states.any { it.endsWith(".tmp") })
    }
}
