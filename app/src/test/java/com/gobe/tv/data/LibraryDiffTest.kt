package com.gobe.tv.data

import com.gobe.tv.data.scan.ScannedRom
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryDiffTest {
    private fun rom(p: String, s: System = System.NES) =
        ScannedRom(p, s, "name", p.substringAfterLast('/'), 10)

    @Test fun computesInsertsAndDeletes() {
        val scanned = listOf(rom("/r/a.nes"), rom("/r/b.nes"))
        val existing = setOf("/r/b.nes", "/r/c.nes")
        val diff = LibraryDiff.compute(scanned, existing)
        assertEquals(listOf("/r/a.nes"), diff.toInsert.map { it.path })
        assertEquals(listOf("/r/c.nes"), diff.toDeletePaths)
    }

    @Test fun noChange() {
        val scanned = listOf(rom("/r/a.nes"))
        val diff = LibraryDiff.compute(scanned, setOf("/r/a.nes"))
        assertEquals(0, diff.toInsert.size)
        assertEquals(0, diff.toDeletePaths.size)
    }
}
