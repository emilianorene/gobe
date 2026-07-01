package com.gobe.tv.data

import com.gobe.tv.data.db.GameDao
import com.gobe.tv.data.db.GameEntity
import com.gobe.tv.data.db.RomFolderDao
import com.gobe.tv.data.db.RomFolderEntity
import com.gobe.tv.data.metadata.GameMatcher
import com.gobe.tv.data.metadata.MetadataIndex
import com.gobe.tv.data.scan.RomScanner
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.RomFolder
import com.gobe.tv.domain.System
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepository(
    private val gameDao: GameDao,
    private val folderDao: RomFolderDao,
    private val scanner: RomScanner,
    private val now: () -> Long = { java.lang.System.currentTimeMillis() },
    private val matcher: GameMatcher? = null,
    private val indexProvider: ((System) -> MetadataIndex)? = null,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { it() },
) {
    fun observeGames(): Flow<List<Game>> = gameDao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    fun observeFolders(): Flow<List<RomFolder>> = folderDao.observeAll().map { list ->
        list.map { RomFolder(it.id, it.path, it.enabled) }
    }

    suspend fun ensureDefaultFolder(defaultPath: String) {
        if (folderDao.count() == 0) folderDao.insert(RomFolderEntity(path = defaultPath))
    }

    suspend fun addFolder(path: String) = folderDao.insert(RomFolderEntity(path = path))
    suspend fun setFolderEnabled(id: Long, enabled: Boolean) = folderDao.setEnabled(id, enabled)
    suspend fun removeFolder(id: Long) = folderDao.delete(id)

    suspend fun getGame(id: Long): Game? = gameDao.getById(id)?.toDomain()

    suspend fun updateLastPlayed(id: Long, now: Long = java.lang.System.currentTimeMillis()) =
        gameDao.touchLastPlayed(id, now)

    fun observeContinuePlaying(limit: Int = 12): Flow<List<Game>> =
        gameDao.observeContinuePlaying(limit).map { list -> list.map { it.toDomain() } }

    /** Scans enabled folders and reconciles the DB. Returns number of games after scan. */
    suspend fun rescan(): Int {
        val paths = folderDao.getEnabled().map { it.path }
        val scanned = scanner.scan(paths)
        val existing = gameDao.getAll().map { it.path }.toSet()
        val diff = LibraryDiff.compute(scanned, existing)
        if (diff.toDeletePaths.isNotEmpty()) gameDao.deleteByPaths(diff.toDeletePaths)
        if (diff.toInsert.isNotEmpty()) {
            val ts = now()
            gameDao.insertAll(diff.toInsert.map {
                GameEntity(
                    path = it.path, system = it.system, displayName = it.displayName,
                    fileName = it.fileName, sizeBytes = it.sizeBytes, dateAdded = ts,
                )
            })
        }

        // Populate players/boxartName for games that don't have it yet.
        val m = matcher
        val provider = indexProvider
        if (m != null && provider != null) {
            val candidates = gameDao.getAll().filter { it.players == null && it.boxartName == null }
            val updates = candidates.mapNotNull { e ->
                val meta = m.match(e.displayName, provider(e.system)) ?: return@mapNotNull null
                MetaUpdate(e.id, meta.players, meta.boxart, meta.genre, meta.year)
            }
            if (updates.isNotEmpty()) {
                runInTransaction {
                    updates.forEach { u -> gameDao.updateMeta(u.id, u.players, u.boxart, u.genre, u.year) }
                }
            }
        }

        return gameDao.getAll().size
    }

    fun genres(): Flow<List<String>> = gameDao.distinctGenres()

    fun searchGames(query: String, system: System?, genre: String? = null): Flow<List<Game>> =
        gameDao.searchGames(query, system?.name, genre).map { list -> list.map { it.toDomain() } }

    private fun GameEntity.toDomain() = Game(
        id, path, system, displayName, fileName, sizeBytes, lastPlayed, dateAdded,
        players = players, boxartName = boxartName, genre = genre, year = year,
    )

    private data class MetaUpdate(
        val id: Long,
        val players: Int?,
        val boxart: String?,
        val genre: String?,
        val year: Int?,
    )
}
