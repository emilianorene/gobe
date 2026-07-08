package com.gobe.tv.ui.library

import com.gobe.tv.domain.System

/** A level-1 Home entry: a console, a special collection, or a search. Opens a LibraryScreen. */
sealed interface LibrarySection {
    data class Console(val system: System) : LibrarySection
    data class SearchAll(val query: String) : LibrarySection
}

/** The fixed base DB filter a section maps to (genre + sort are chosen live in the library). Pure. */
data class SectionFilter(
    val query: String,
    val system: System?,
    val recommendedOnly: Boolean,
    val favoritesOnly: Boolean,
)

fun sectionFilter(section: LibrarySection): SectionFilter = when (section) {
    is LibrarySection.Console -> SectionFilter("", section.system, false, false)
    is LibrarySection.SearchAll -> SectionFilter(section.query, null, false, false)
}
