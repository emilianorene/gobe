package com.gobe.tv.emulation

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class EmulatorContractTest {
    @Test fun roundTrip() {
        val args = EmulatorArgs(gameId = 5, romPath = "/r/x.sfc", system = System.SNES, loadState = true)
        val restored = EmulatorArgs.fromMap(args.toMap())
        assertEquals(args, restored)
    }

    @Test fun roundTripDefaults() {
        val args = EmulatorArgs(gameId = 1, romPath = "/r/y.sfc", system = System.SNES)
        assertEquals(false, args.loadState)
        assertEquals(args, EmulatorArgs.fromMap(args.toMap()))
    }
}
