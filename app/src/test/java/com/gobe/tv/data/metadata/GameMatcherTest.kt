package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class GameMatcherTest {
    private val idx = MetadataIndex.parse("""{"supermarioworld":{"boxart":"Super Mario World","players":2}}""")
    private val m = GameMatcher(NameNormalizer())
    @Test fun matchesByNormalizedName() {
        val meta = m.match("Super Mario World (USA) [!]", idx)
        assertEquals("Super Mario World", meta?.boxart); assertEquals(2, meta?.players)
    }
    @Test fun nullWhenNoMatch() = assertNull(m.match("Nonexistent Game", idx))
}
