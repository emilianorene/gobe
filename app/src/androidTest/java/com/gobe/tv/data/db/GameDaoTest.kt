package com.gobe.tv.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gobe.tv.domain.System
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDaoTest {
    private lateinit var db: GobeDatabase
    private lateinit var dao: GameDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), GobeDatabase::class.java
        ).build()
        dao = db.gameDao()
    }
    @After fun close() = db.close()

    @Test fun insertAndQuery() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/r/a.nes", system = System.NES, displayName = "A",
                fileName = "a.nes", sizeBytes = 1, dateAdded = 1L),
        ))
        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(System.NES, all[0].system)
    }

    @Test fun ignoresDuplicatePath() = runBlocking {
        val g = GameEntity(path = "/r/a.nes", system = System.NES, displayName = "A",
            fileName = "a.nes", sizeBytes = 1, dateAdded = 1L)
        dao.insertAll(listOf(g)); dao.insertAll(listOf(g))
        assertEquals(1, dao.getAll().size)
    }

    @Test fun continuePlayingOrdersByLastPlayedDesc() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/r/a.sfc", system = System.SNES, displayName = "A", fileName = "a.sfc", sizeBytes = 1, dateAdded = 1L),
            GameEntity(path = "/r/b.sfc", system = System.SNES, displayName = "B", fileName = "b.sfc", sizeBytes = 1, dateAdded = 1L),
        ))
        val all = dao.getAll()
        val a = all.first { it.path == "/r/a.sfc" }
        val b = all.first { it.path == "/r/b.sfc" }
        dao.touchLastPlayed(a.id, 1000L)
        dao.touchLastPlayed(b.id, 2000L)
        val cp = dao.observeContinuePlaying(10).first()
        assertEquals(listOf("/r/b.sfc", "/r/a.sfc"), cp.map { it.path })  // b first (more recent)
    }

    @Test fun continuePlayingExcludesNeverPlayed() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/r/a.sfc", system = System.SNES, displayName = "A", fileName = "a.sfc", sizeBytes = 1, dateAdded = 1L),
        ))
        assertEquals(0, dao.observeContinuePlaying(10).first().size)
    }

    @Test fun searchAndUpdateMeta() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path="/r/Super Mario World.sfc", system=System.SNES, displayName="Super Mario World",
                fileName="x.sfc", sizeBytes=1, dateAdded=1L),
        ))
        val g = dao.getAll().first()
        dao.updateMeta(g.id, players = 2, boxartName = "Super Mario World", genre = "Platform", year = 1991)
        val hits = dao.searchGames("Mario", System.SNES.name, null, 0).first()
        assertEquals(1, hits.size)
        assertEquals(2, hits[0].players)
        assertEquals("Super Mario World", hits[0].boxartName)
        assertEquals("Platform", hits[0].genre)
        assertEquals(1991, hits[0].year)
        // filter by other system returns nothing
        assertEquals(0, dao.searchGames("Mario", System.NES.name, null, 0).first().size)
        // null system = all systems
        assertEquals(1, dao.searchGames("Mario", null, null, 0).first().size)
    }

    private fun game(name: String, system: System, genre: String?) = GameEntity(
        path = "/r/$name", system = system, displayName = name,
        fileName = "$name.x", sizeBytes = 1, dateAdded = 1L, genre = genre,
    )

    @Test fun distinctGenresSortedNonEmpty() = runBlocking {
        dao.insertAll(listOf(
            game("A", System.SNES, "Platform"),
            game("B", System.NES, "Action"),
            game("C", System.SNES, "Action"),
            game("D", System.SNES, null),
            game("E", System.SNES, ""),
        ))
        assertEquals(listOf("Action", "Platform"), dao.distinctGenres().first())
    }

    @Test fun searchFiltersByGenreAndSystem() = runBlocking {
        dao.insertAll(listOf(
            game("A", System.SNES, "Action"),
            game("B", System.NES, "Action"),
            game("C", System.SNES, "Platform"),
        ))
        // genre only
        assertEquals(listOf("A", "B"), dao.searchGames("", null, "Action", 0).first().map { it.displayName })
        // genre + system
        assertEquals(listOf("A"), dao.searchGames("", "SNES", "Action", 0).first().map { it.displayName })
        // no genre = unchanged (all)
        assertEquals(3, dao.searchGames("", null, null, 0).first().size)
    }

    @Test fun searchRecommendedOnlyAndSortsRecommendedFirst() = runBlocking {
        dao.insertAll(listOf(
            GameEntity(path = "/a", system = System.SNES, displayName = "Alpha",
                fileName = "a.sfc", sizeBytes = 1, dateAdded = 1L),   // recommended=false (default)
            GameEntity(path = "/z", system = System.SNES, displayName = "Zeta",
                fileName = "z.sfc", sizeBytes = 1, dateAdded = 1L),
        ))
        val zeta = dao.getAll().first { it.displayName == "Zeta" }
        dao.updateRecommended(zeta.id, true)
        // recommendedOnly = 1 -> only Zeta
        assertEquals(listOf("Zeta"), dao.searchGames("", null, null, 1).first().map { it.displayName })
        // recommendedOnly = 0 -> Zeta first (recommended DESC), then Alpha
        assertEquals(listOf("Zeta", "Alpha"), dao.searchGames("", null, null, 0).first().map { it.displayName })
    }
}
