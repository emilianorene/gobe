package com.gobe.tv.data.system

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemDetectorTest {
    private val d = SystemDetector()

    @Test fun nes() = assertEquals(System.NES, d.detect("/r/Contra.nes"))
    @Test fun snes_smc() = assertEquals(System.SNES, d.detect("/r/Mario.smc"))
    @Test fun snes_sfc() = assertEquals(System.SNES, d.detect("/r/Mario.sfc"))
    @Test fun n64() = assertEquals(System.N64, d.detect("/r/Zelda.z64"))
    @Test fun arcade_zip() = assertEquals(System.ARCADE, d.detect("/r/mslug.zip"))
    @Test fun caseInsensitive() = assertEquals(System.NES, d.detect("/r/GAME.NES"))
    @Test fun unknown() = assertNull(d.detect("/r/notes.txt"))
    @Test fun noExtension() = assertNull(d.detect("/r/README"))
}
