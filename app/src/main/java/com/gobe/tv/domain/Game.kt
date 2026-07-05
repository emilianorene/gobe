package com.gobe.tv.domain

data class Game(
    val id: Long = 0,
    val path: String,
    val system: System,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val lastPlayed: Long? = null,
    val dateAdded: Long,
    val players: Int? = null,
    val boxartName: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val recommended: Boolean = false,
    val favorite: Boolean = false,
)
