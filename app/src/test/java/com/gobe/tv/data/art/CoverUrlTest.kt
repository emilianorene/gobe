package com.gobe.tv.data.art

import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverUrlTest {
    private fun game(boxart: String?, cover: String?) = Game(
        id = 1, path = "/x", system = System.SNES, displayName = "X",
        fileName = "x", sizeBytes = 0, dateAdded = 0, boxartName = boxart, igdbCover = cover,
    )
    @Test fun prefersLibretroWhenBoxartPresent() {
        assertTrue(coverUrl(game("Super Mario World", "co123"))!!.contains("thumbnails.libretro.com"))
    }
    @Test fun fallsBackToIgdbWhenNoBoxart() {
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/co123.jpg", coverUrl(game(null, "co123")))
    }
    @Test fun nullWhenNeither() { assertNull(coverUrl(game(null, null))) }
    @Test fun igdbCoverUrlFormats() {
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/abc.jpg", igdbCoverUrl("abc"))
    }
    @Test fun igdbCoverUrlNullOrBlank() { assertNull(igdbCoverUrl(null)); assertNull(igdbCoverUrl("")) }
}
