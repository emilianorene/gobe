package com.gobe.tv.ui

import com.gobe.tv.ui.library.LibrarySection

sealed interface Route {
    data object Permission : Route
    data object Home : Route
    data class Library(val section: LibrarySection) : Route
    data object Settings : Route
    data object Folders : Route
    data class FolderBrowser(val startPath: String) : Route
    data class Detail(val gameId: Long) : Route
}
