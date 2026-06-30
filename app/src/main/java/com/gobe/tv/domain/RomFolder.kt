package com.gobe.tv.domain

data class RomFolder(
    val id: Long = 0,
    val path: String,
    val enabled: Boolean = true,
)
