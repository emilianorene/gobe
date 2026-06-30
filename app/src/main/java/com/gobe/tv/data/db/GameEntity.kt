package com.gobe.tv.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gobe.tv.domain.System

@Entity(tableName = "games", indices = [Index(value = ["path"], unique = true)])
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val system: System,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val lastPlayed: Long? = null,
    val dateAdded: Long,
)
