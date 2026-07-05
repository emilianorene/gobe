package com.gobe.tv.domain

/** Grid sort order. dbValue is the int passed to the DAO's CASE-based ORDER BY. */
enum class SortMode(val dbValue: Int) { RECOMMENDED(0), TITLE(1), YEAR(2) }
