package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class MetadataIndexTest {
    private val json = """{"supermarioworld":{"boxart":"Super Mario World","players":2,"recommended":true},
                          "contra":{"boxart":"Contra","players":2}}"""
    @Test fun looksUpNormalized() {
        val idx = MetadataIndex.parse(json)
        assertEquals("Super Mario World", idx["supermarioworld"]?.boxart)
        assertEquals(2, idx["contra"]?.players)
        assertNull(idx["unknown"])
    }
    @Test fun readsRecommendedFlag() {
        val idx = MetadataIndex.parse(json)
        assertTrue(idx["supermarioworld"]?.recommended == true)   // present -> true
        assertFalse(idx["contra"]?.recommended == true)           // absent  -> false
    }
}
