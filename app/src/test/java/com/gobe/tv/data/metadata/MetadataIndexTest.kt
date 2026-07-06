package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class MetadataIndexTest {
    private val json = """{"supermarioworld":{"boxart":"Super Mario World","players":2,"recommended":true,"description":"A platformer.","igdbCover":"co123"},
                          "contra":{"boxart":"Contra","players":2,"igdbCover":"co456"}}"""
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
    @Test fun readsRichMetadata() {
        val idx = MetadataIndex.parse(json)
        assertEquals("A platformer.", idx["supermarioworld"]?.description)
        assertEquals("co123", idx["supermarioworld"]?.igdbCover)
        assertNull(idx["contra"]?.description)          // non-recommended: no description
        assertEquals("co456", idx["contra"]?.igdbCover)
    }
}
