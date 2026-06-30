package com.gobe.tv.ui

sealed interface Route {
    data object Permission : Route
    data object Home : Route
    data object Settings : Route
    data object Folders : Route
    data class FolderBrowser(val startPath: String) : Route
    data class Detail(val gameId: Long) : Route
}
