package com.gobe.tv.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gobe.tv.domain.System
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
}
