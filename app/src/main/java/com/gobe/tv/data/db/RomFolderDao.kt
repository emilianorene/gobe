package com.gobe.tv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RomFolderDao {
    @Query("SELECT * FROM rom_folders ORDER BY path ASC")
    fun observeAll(): Flow<List<RomFolderEntity>>

    @Query("SELECT * FROM rom_folders WHERE enabled = 1")
    suspend fun getEnabled(): List<RomFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: RomFolderEntity)

    @Query("UPDATE rom_folders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM rom_folders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM rom_folders")
    suspend fun count(): Int
}
