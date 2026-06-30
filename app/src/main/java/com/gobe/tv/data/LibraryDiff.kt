package com.gobe.tv.data

import com.gobe.tv.data.scan.ScannedRom

data class LibraryDiffResult(
    val toInsert: List<ScannedRom>,
    val toDeletePaths: List<String>,
)

object LibraryDiff {
    fun compute(scanned: List<ScannedRom>, existingPaths: Set<String>): LibraryDiffResult {
        val scannedPaths = scanned.map { it.path }.toSet()
        val toInsert = scanned.filter { it.path !in existingPaths }
        val toDelete = existingPaths.filter { it !in scannedPaths }
        return LibraryDiffResult(toInsert, toDelete)
    }
}
