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
}
