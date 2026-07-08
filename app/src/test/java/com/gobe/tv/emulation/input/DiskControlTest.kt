package com.gobe.tv.emulation.input

import com.gobe.tv.domain.System
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiskControlTest {
    @Test fun fdsRomOnNesIsFds() {
        assertTrue(isFdsGame(System.NES, "/roms/Zelda no Densetsu.fds"))
    }
    @Test fun fdsExtensionIsCaseInsensitive() {
        assertTrue(isFdsGame(System.NES, "/roms/Game.FDS"))
    }
    @Test fun plainNesRomIsNotFds() {
        assertFalse(isFdsGame(System.NES, "/roms/Circus Charlie.nes"))
    }
    @Test fun fdsExtensionOnNonNesSystemIsNotFds() {
        assertFalse(isFdsGame(System.SNES, "/roms/weird.fds"))
    }
}
