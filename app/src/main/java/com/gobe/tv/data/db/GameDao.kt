package com.gobe.tv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games")
    suspend fun getAll(): List<GameEntity>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(games: List<GameEntity>)

    @Query("DELETE FROM games WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("UPDATE games SET lastPlayed = :ts WHERE id = :id")
    suspend fun touchLastPlayed(id: Long, ts: Long)

    @Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeContinuePlaying(limit: Int): Flow<List<GameEntity>>
}
