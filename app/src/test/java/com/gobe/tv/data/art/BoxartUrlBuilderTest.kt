package com.gobe.tv.data.art

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoxartUrlBuilderTest {
    private val b = BoxartUrlBuilder()

    @Test fun snesUrl() = assertEquals(
        "https://thumbnails.libretro.com/Nintendo%20-%20Super%20Nintendo%20Entertainment%20System/Named_Boxarts/Super%20Mario%20World.png",
        b.url(System.SNES, "Super Mario World"),
    )
    @Test fun sanitizesIllegalChars() = assertEquals(
        "https://thumbnails.libretro.com/Nintendo%20-%20Super%20Nintendo%20Entertainment%20System/Named_Boxarts/Tom%20_%20Jerry.png",
        b.url(System.SNES, "Tom & Jerry"),
    )
    @Test fun nullWhenNoName() = assertNull(b.url(System.SNES, null))
    @Test fun nesFolder() = assertEquals(
        "https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%20Entertainment%20System/Named_Boxarts/Contra.png",
        b.url(System.NES, "Contra"),
    )
}
