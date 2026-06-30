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
    @Test fun articlesLeadingAndTrailingMatch() {
        // No-Intro moves articles to the end; our cleaned names keep them in front.
        assertEquals("bugslife", n.normalize("A Bug's Life"))
        assertEquals("bugslife", n.normalize("Bug's Life, A (USA)"))
        assertEquals("legendofzelda", n.normalize("The Legend of Zelda"))
        assertEquals("legendofzelda", n.normalize("Legend of Zelda, The (USA)"))
    }
}
