package com.gobe.tv.data.metadata
import org.junit.Assert.assertEquals
import org.junit.Test
class NameNormalizerTest {
    private val n = NameNormalizer()
    @Test fun stripsTagsCaseAndPunctuation() {
        assertEquals("supermarioworld", n.normalize("Super Mario World (USA) [!]"))
        assertEquals("madams", n.normalize("The M.Adams"))
        assertEquals("finalfightguy", n.normalize("Final Fight Guy (NA)"))
    }
}
