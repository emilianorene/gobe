package com.gobe.tv.ui.home

import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleArtTest {
    @Test fun photoConsolesUsePhotosAndAccents() {
        assertEquals(R.drawable.console_nes, consoleArt(System.NES).art)
        assertTrue(consoleArt(System.NES).isPhoto)
        assertEquals(GobeAccentNes, consoleArt(System.NES).accent)
        assertEquals(R.drawable.console_snes, consoleArt(System.SNES).art)
        assertEquals(GobeAccentSnes, consoleArt(System.SNES).accent)
        assertEquals(R.drawable.console_n64, consoleArt(System.N64).art)
        assertEquals(GobeAccentN64, consoleArt(System.N64).accent)
    }
    @Test fun arcadeFallsBackToVectorIcon() {
        val arcade = consoleArt(System.ARCADE)
        assertEquals(R.drawable.ic_controller_arcade, arcade.art)
        assertFalse(arcade.isPhoto)
        assertEquals(GobeAccentArcade, arcade.accent)
    }
}
