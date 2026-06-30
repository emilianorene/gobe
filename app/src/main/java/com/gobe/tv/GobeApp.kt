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

    // NOTE: placeholder default ROM path. To be confirmed via adb on the real ONN
    // (Task 1) and updated before the device-acceptance pass.
    val defaultRomPath: String = "/storage/emulated/0/Roms"

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, GobeDatabase::class.java, "gobe.db").build()
        val scanner = RomScanner(SystemDetector(), NameCleaner())
        repository = LibraryRepository(db.gameDao(), db.romFolderDao(), scanner)
    }
}
