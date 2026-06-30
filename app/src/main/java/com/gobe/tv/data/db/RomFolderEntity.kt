package com.gobe.tv.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rom_folders", indices = [Index(value = ["path"], unique = true)])
data class RomFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val enabled: Boolean = true,
)
