package com.gobe.tv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GameEntity::class, RomFolderEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GobeDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun romFolderDao(): RomFolderDao
}
