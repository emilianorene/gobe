package com.gobe.tv.ui.library

/** Rail-row subtitle: "year · genre", omitting each part when absent. Pure for unit testing. */
fun rowSubtitle(year: Int?, genre: String?): String {
    val y = if (year != null && year > 0) year.toString() else null
    val g = genre?.trim()?.takeIf { it.isNotBlank() }
    return listOfNotNull(y, g).joinToString(" · ")
}
