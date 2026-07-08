package com.gobe.tv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class RowSubtitleTest {
    @Test fun yearAndGenre() =
        assertEquals("1988 · Platformer", rowSubtitle(1988, "Platformer"))

    @Test fun yearOnly() =
        assertEquals("1988", rowSubtitle(1988, null))

    @Test fun genreOnly() =
        assertEquals("Platformer", rowSubtitle(null, "Platformer"))

    @Test fun neither() =
        assertEquals("", rowSubtitle(null, null))

    @Test fun nonPositiveYearOmitted() =
        assertEquals("Action", rowSubtitle(0, "Action"))

    @Test fun blankGenreOmitted() =
        assertEquals("1990", rowSubtitle(1990, "  "))
}
