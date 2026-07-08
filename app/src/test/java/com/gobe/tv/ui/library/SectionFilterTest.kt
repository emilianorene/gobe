package com.gobe.tv.ui.library

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionFilterTest {
    @Test fun consoleMapsToSystem() {
        val f = sectionFilter(LibrarySection.Console(System.SNES))
        assertEquals(System.SNES, f.system)
        assertTrue(!f.recommendedOnly && !f.favoritesOnly && f.query == "")
    }
    @Test fun searchAllCarriesQuery() {
        val f = sectionFilter(LibrarySection.SearchAll("zelda"))
        assertEquals("zelda", f.query)
        assertEquals(null, f.system)
    }
}
