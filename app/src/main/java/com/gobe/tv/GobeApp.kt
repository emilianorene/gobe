package com.gobe.tv

import android.app.Application
import androidx.room.Room
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.data.db.GobeDatabase
import com.gobe.tv.data.scan.RomScanner
import com.gobe.tv.data.system.NameCleaner
import com.gobe.tv.data.system.SystemDetector

class GobeApp : Application() {
    lateinit var repository: LibraryRepository
        private set

    // Default ROM folder on the ONN (confirmed via adb). Internal storage is
    // case-insensitive, so this matches Download/roms regardless of casing.
    val defaultRomPath: String = "/storage/emulated/0/Download/ROMs"

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, GobeDatabase::class.java, "gobe.db").build()
        val scanner = RomScanner(SystemDetector(), NameCleaner())
        repository = LibraryRepository(db.gameDao(), db.romFolderDao(), scanner)
    }
}
