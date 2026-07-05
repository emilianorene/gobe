package com.gobe.tv

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.data.db.GobeDatabase
import com.gobe.tv.data.metadata.GameMatcher
import com.gobe.tv.data.metadata.MetadataIndex
import com.gobe.tv.data.metadata.NameNormalizer
import com.gobe.tv.data.scan.RomScanner
import com.gobe.tv.data.system.NameCleaner
import com.gobe.tv.data.system.SystemDetector
import com.gobe.tv.domain.System

class GobeApp : Application() {
    lateinit var repository: LibraryRepository
        private set

    // Default ROM folder on the ONN (confirmed via adb). Internal storage is
    // case-insensitive, so this matches Download/roms regardless of casing.
    val defaultRomPath: String = "/storage/emulated/0/Download/ROMs"

    // Set when launching the emulator; consumed by GobeNavHost on the next ON_RESUME to
    // route back to the library grid (Home) after exiting a game.
    var returnToHomeOnResume: Boolean = false

    val gameMatcher = GameMatcher(NameNormalizer())

    private val indexCache = mutableMapOf<System, MetadataIndex>()

    fun metadataIndex(system: System): MetadataIndex = indexCache.getOrPut(system) {
        runCatching {
            assets.open("metadata/${system.name.lowercase()}.json").bufferedReader().use { it.readText() }
        }.mapCatching { MetadataIndex.parse(it) }
            .getOrDefault(MetadataIndex(emptyMap()))
    }

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, GobeDatabase::class.java, "gobe.db")
            .addMigrations(GobeDatabase.MIGRATION_1_2, GobeDatabase.MIGRATION_2_3, GobeDatabase.MIGRATION_3_4, GobeDatabase.MIGRATION_4_5)
            .build()
        val scanner = RomScanner(SystemDetector(), NameCleaner())
        repository = LibraryRepository(
            db.gameDao(), db.romFolderDao(), scanner,
            matcher = gameMatcher,
            indexProvider = { system -> metadataIndex(system) },
            runInTransaction = { block -> db.withTransaction { block() } },
        )
    }
}
