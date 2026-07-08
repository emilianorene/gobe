package com.gobe.tv.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.theme.*

/** Home-carousel art for a console: a photo (NES/SNES/N64) or the vector placeholder (Arcade). */
data class ConsoleArt(@DrawableRes val art: Int, val isPhoto: Boolean, val accent: Color)

/** Pure map from a [System] to its carousel art + accent. Single source of truth. */
fun consoleArt(system: System): ConsoleArt = when (system) {
    System.NES    -> ConsoleArt(R.drawable.console_nes, isPhoto = true, accent = GobeAccentNes)
    System.SNES   -> ConsoleArt(R.drawable.console_snes, isPhoto = true, accent = GobeAccentSnes)
    System.N64    -> ConsoleArt(R.drawable.console_n64, isPhoto = true, accent = GobeAccentN64)
    System.ARCADE -> ConsoleArt(R.drawable.ic_controller_arcade, isPhoto = false, accent = GobeAccentArcade)
}
