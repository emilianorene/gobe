package com.gobe.tv.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.gobe.tv.R
import com.gobe.tv.domain.System
import com.gobe.tv.ui.library.LibrarySection
import com.gobe.tv.ui.theme.*

/** Icon (controller / star / heart) + accent color for a Home level-1 section tile. */
data class SectionVisual(@DrawableRes val iconRes: Int, val accent: Color)

/**
 * Pure map from a level-1 [LibrarySection] to its Hero-tile art + accent. Single source of the
 * console -> (controller drawable, color) table. `SearchAll` is never rendered as a section tile;
 * it returns a neutral fallback only to keep this `when` total.
 */
fun sectionVisual(section: LibrarySection): SectionVisual = when (section) {
    is LibrarySection.Console -> when (section.system) {
        System.NES    -> SectionVisual(R.drawable.ic_controller_nes, GobeAccentNes)
        System.SNES   -> SectionVisual(R.drawable.ic_controller_snes, GobeAccentSnes)
        System.N64    -> SectionVisual(R.drawable.ic_controller_n64, GobeAccentN64)
        System.ARCADE -> SectionVisual(R.drawable.ic_controller_arcade, GobeAccentArcade)
    }
    LibrarySection.Recommended -> SectionVisual(R.drawable.ic_section_recommended, GobeAccentRecommended)
    LibrarySection.Favorites   -> SectionVisual(R.drawable.ic_section_favorites, GobeAccentFavorites)
    is LibrarySection.SearchAll -> SectionVisual(R.drawable.ic_section_recommended, GobeAccent) // never rendered
}
