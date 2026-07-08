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
class GameDaoHomeQueriesTest {
    private lateinit var db: GobeDatabase
    private lateinit var dao: GameDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), GobeDatabase::class.java
        ).build()
        dao = db.gameDao()
    }
    @After fun close() = db.close()

    private fun game(path: String, system: System) = GameEntity(
        path = path, system = system, displayName = path, fileName = path, sizeBytes = 1, dateAdded = 1L,
    )

    @Test fun countsBySystemGroupsAndCounts() = runBlocking {
        dao.insertAll(listOf(
            game("/a.nes", System.NES), game("/b.nes", System.NES),
            game("/c.sfc", System.SNES),
        ))
        val counts = dao.observeCountsBySystem().first().associate { it.system to it.count }
        assertEquals(2, counts[System.NES])
        assertEquals(1, counts[System.SNES])
        assertEquals(null, counts[System.N64])
    }

    @Test fun recentBySystemFiltersOrdersAndLimits() = runBlocking {
        dao.insertAll(listOf(
            game("/a.nes", System.NES), game("/b.nes", System.NES), game("/z.sfc", System.SNES),
        ))
        val all = dao.getAll()
        dao.touchLastPlayed(all.first { it.path == "/a.nes" }.id, 1000L)
        dao.touchLastPlayed(all.first { it.path == "/b.nes" }.id, 2000L)
        dao.touchLastPlayed(all.first { it.path == "/z.sfc" }.id, 3000L)

        val nes = dao.observeContinuePlayingBySystem(System.NES.name, 10).first()
        assertEquals(listOf("/b.nes", "/a.nes"), nes.map { it.path })

        val nesLimited = dao.observeContinuePlayingBySystem(System.NES.name, 1).first()
        assertEquals(listOf("/b.nes"), nesLimited.map { it.path })
    }
}
