package com.gobe.tv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GameEntity::class, RomFolderEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GobeDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun romFolderDao(): RomFolderDao

    companion object {
        val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE games ADD COLUMN players INTEGER")
            db.execSQL("ALTER TABLE games ADD COLUMN boxartName TEXT")
        }
    }
}
