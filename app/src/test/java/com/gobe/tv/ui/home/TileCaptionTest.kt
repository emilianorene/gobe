package com.gobe.tv.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TileCaptionTest {
    @Test fun nameWithYear() =
        assertEquals("Super Mario World (1991)", tileCaption("Super Mario World", 1991))

    @Test fun nameWithoutYear() =
        assertEquals("Super Mario World", tileCaption("Super Mario World", null))

    @Test fun nonPositiveYearIsOmitted() =
        assertEquals("Final Fight", tileCaption("Final Fight", 0))
}
