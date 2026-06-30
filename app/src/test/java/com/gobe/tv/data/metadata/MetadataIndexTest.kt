package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class MetadataIndexTest {
    private val json = """{"supermarioworld":{"boxart":"Super Mario World","players":2},
                          "contra":{"boxart":"Contra","players":2}}"""
    @Test fun looksUpNormalized() {
        val idx = MetadataIndex.parse(json)
        assertEquals("Super Mario World", idx["supermarioworld"]?.boxart)
        assertEquals(2, idx["contra"]?.players)
        assertNull(idx["unknown"])
    }
}
