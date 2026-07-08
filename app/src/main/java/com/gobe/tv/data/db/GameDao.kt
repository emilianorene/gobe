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

    @Query("SELECT system AS system, COUNT(*) AS count FROM games GROUP BY system")
    fun observeCountsBySystem(): Flow<List<SystemCount>>

    @Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL AND system = :system ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeContinuePlayingBySystem(system: String, limit: Int): Flow<List<GameEntity>>

    @Query("SELECT DISTINCT genre FROM games WHERE genre IS NOT NULL AND genre != '' ORDER BY genre COLLATE NOCASE ASC")
    fun distinctGenres(): Flow<List<String>>

    @Query("SELECT * FROM games WHERE displayName LIKE '%' || :q || '%' AND (:system IS NULL OR system = :system) AND (:genre IS NULL OR genre = :genre) AND (:recommendedOnly = 0 OR recommended = 1) AND (:favoritesOnly = 0 OR favorite = 1) ORDER BY CASE WHEN :sortMode = 0 THEN recommended ELSE 0 END DESC, CASE WHEN :sortMode = 2 THEN year END DESC, displayName COLLATE NOCASE ASC")
    fun searchGames(q: String, system: String?, genre: String?, recommendedOnly: Int, favoritesOnly: Int, sortMode: Int): Flow<List<GameEntity>>

    @Query("UPDATE games SET players = :players, boxartName = :boxartName, genre = :genre, year = :year, description = :description, igdbCover = :igdbCover WHERE id = :id")
    suspend fun updateMeta(id: Long, players: Int?, boxartName: String?, genre: String?, year: Int?, description: String?, igdbCover: String?)

    @Query("UPDATE games SET recommended = :recommended, description = :description, igdbCover = :igdbCover WHERE id = :id")
    suspend fun updateIndexExtras(id: Long, recommended: Boolean, description: String?, igdbCover: String?)

    @Query("UPDATE games SET recommended = :recommended WHERE id = :id")
    suspend fun updateRecommended(id: Long, recommended: Boolean)

    @Query("UPDATE games SET favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)
}

/** Room projection for a per-system game count. `system` maps back via the enum type converter. */
data class SystemCount(val system: com.gobe.tv.domain.System, val count: Int)
