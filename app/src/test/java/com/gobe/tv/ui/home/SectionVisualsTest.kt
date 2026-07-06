package com.gobe.tv.ui.home

import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.library.LibrarySection
import com.gobe.tv.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SectionVisualsTest {
    @Test fun consolesMapToTheirControllerAndAccent() {
        assertEquals(R.drawable.ic_controller_nes, sectionVisual(LibrarySection.Console(System.NES)).iconRes)
        assertEquals(GobeAccentNes, sectionVisual(LibrarySection.Console(System.NES)).accent)
        assertEquals(R.drawable.ic_controller_snes, sectionVisual(LibrarySection.Console(System.SNES)).iconRes)
        assertEquals(R.drawable.ic_controller_n64, sectionVisual(LibrarySection.Console(System.N64)).iconRes)
        assertEquals(R.drawable.ic_controller_arcade, sectionVisual(LibrarySection.Console(System.ARCADE)).iconRes)
    }
    @Test fun recommendedAndFavoritesMapToStarAndHeart() {
        assertEquals(R.drawable.ic_section_recommended, sectionVisual(LibrarySection.Recommended).iconRes)
        assertEquals(GobeAccentRecommended, sectionVisual(LibrarySection.Recommended).accent)
        assertEquals(R.drawable.ic_section_favorites, sectionVisual(LibrarySection.Favorites).iconRes)
        assertEquals(GobeAccentFavorites, sectionVisual(LibrarySection.Favorites).accent)
    }
    @Test fun nesAccentDiffersFromFavoritesRed() {
        assertNotEquals(sectionVisual(LibrarySection.Console(System.NES)).accent,
                        sectionVisual(LibrarySection.Favorites).accent)
    }
}
