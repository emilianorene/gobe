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

    @Query("SELECT DISTINCT genre FROM games WHERE genre IS NOT NULL AND genre != '' ORDER BY genre COLLATE NOCASE ASC")
    fun distinctGenres(): Flow<List<String>>

    @Query("SELECT * FROM games WHERE displayName LIKE '%' || :q || '%' AND (:system IS NULL OR system = :system) AND (:genre IS NULL OR genre = :genre) ORDER BY displayName COLLATE NOCASE ASC")
    fun searchGames(q: String, system: String?, genre: String?): Flow<List<GameEntity>>

    @Query("UPDATE games SET players = :players, boxartName = :boxartName, genre = :genre, year = :year WHERE id = :id")
    suspend fun updateMeta(id: Long, players: Int?, boxartName: String?, genre: String?, year: Int?)
}
