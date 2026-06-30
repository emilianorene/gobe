package com.gobe.tv.data.system

import com.gobe.tv.domain.System

class SystemDetector {
    fun detect(path: String): System? {
        val name = path.substringAfterLast('/')
        if (!name.contains('.')) return null
        val ext = name.substringAfterLast('.').lowercase()

        // A .zip is ambiguous: it may be an arcade romset OR a packed console game
        // (e.g. SNES collections). Prefer the parent folder name when it clearly names
        // a system; otherwise treat it as an (unverified) arcade romset.
        if (ext == "zip") {
            val parentFolder = path.substringBeforeLast('/', "").substringAfterLast('/')
            return folderToSystem(parentFolder) ?: System.ARCADE
        }

        return System.fromExtension(ext)
    }

    /** Maps a folder name to a system when it clearly identifies one, else null. */
    private fun folderToSystem(folder: String): System? {
        val f = folder.lowercase()
        return when {
            f.contains("snes") || f.contains("super nintendo") || f.contains("sfc") -> System.SNES
            f.contains("n64") || f.contains("nintendo 64") -> System.N64
            f.contains("famicom") || f.contains("fds") ||
                f == "nes" || f.contains("nintendo entertainment") -> System.NES
            f.contains("arcade") || f.contains("mame") || f.contains("neogeo") ||
                f.contains("neo geo") || f.contains("fbneo") || f.contains("cps") -> System.ARCADE
            else -> null
        }
    }
}
