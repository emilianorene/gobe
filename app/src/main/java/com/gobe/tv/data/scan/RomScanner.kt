package com.gobe.tv.data.scan

import com.gobe.tv.data.system.NameCleaner
import com.gobe.tv.data.system.SystemDetector
import com.gobe.tv.domain.System
import java.io.File
import java.nio.file.Files

data class ScannedRom(
    val path: String,
    val system: System,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
)

class RomScanner(
    private val detector: SystemDetector,
    private val nameCleaner: NameCleaner,
    private val maxDepth: Int = 12,
) {
    fun scan(folderPaths: List<String>): List<ScannedRom> {
        val out = mutableListOf<ScannedRom>()
        for (p in folderPaths) {
            val root = File(p)
            if (!root.isDirectory) continue
            walk(root, 0, out)
        }
        return out
    }

    private fun walk(dir: File, depth: Int, out: MutableList<ScannedRom>) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            // Do not follow symlinks (avoid cyclic/pathological trees).
            if (isSymlink(f)) continue
            if (f.isDirectory) {
                walk(f, depth + 1, out)
            } else {
                val system = detector.detect(f.absolutePath) ?: continue
                out += ScannedRom(
                    path = f.absolutePath,
                    system = system,
                    displayName = nameCleaner.clean(f.name),
                    fileName = f.name,
                    sizeBytes = f.length(),
                )
            }
        }
    }

    private fun isSymlink(f: File): Boolean =
        try { Files.isSymbolicLink(f.toPath()) } catch (e: Exception) { true }
}
