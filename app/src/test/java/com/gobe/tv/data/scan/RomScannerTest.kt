package com.gobe.tv.data.scan

import com.gobe.tv.data.system.NameCleaner
import com.gobe.tv.data.system.SystemDetector
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RomScannerTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scanner = RomScanner(SystemDetector(), NameCleaner())

    private fun write(rel: String, bytes: Int = 16): File {
        val f = File(tmp.root, rel)
        f.parentFile?.mkdirs()
        f.writeBytes(ByteArray(bytes))
        return f
    }

    @Test fun findsRecognizedRecursively() {
        write("Contra.nes")
        write("snes/Mario (USA).smc")
        write("docs/readme.txt")     // ignored
        val roms = scanner.scan(listOf(tmp.root.absolutePath))
        assertEquals(2, roms.size)
        val mario = roms.first { it.system == System.SNES }
        assertEquals("Mario", mario.displayName)
        assertTrue(mario.sizeBytes > 0)
    }

    @Test fun emptyForMissingFolder() {
        assertEquals(0, scanner.scan(listOf("/no/such/path")).size)
    }
}
