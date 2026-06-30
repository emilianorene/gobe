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

    // Famicom Disk System images are NES.
    @Test fun fds() = assertEquals(System.NES, d.detect("/r/Zanac (JP).fds"))
    @Test fun fdsCase() = assertEquals(System.NES, d.detect("/r/Game.FDS"))

    // A .zip is classified by its parent folder when that folder names a system,
    // otherwise it falls back to ARCADE.
    @Test fun zipInSnesFolder() =
        assertEquals(System.SNES, d.detect("/roms/Snes/Donkey Kong Country.zip"))
    @Test fun zipInArcadeFolder() =
        assertEquals(System.ARCADE, d.detect("/roms/Arcade/contra.zip"))
    @Test fun zipInUnknownFolderIsArcade() =
        assertEquals(System.ARCADE, d.detect("/roms/misc/mslug.zip"))
    @Test fun zipInN64Folder() =
        assertEquals(System.N64, d.detect("/roms/N64/Mario 64.zip"))
}
