package com.gobe.tv.emulation

import com.gobe.tv.domain.System

/** Resolves the bundled Libretro core .so path for a system. SNES (snes9x), Arcade (FBNeo) and
 *  NES/FDS (FCEUmm) are wired; N64 not yet. */
class CoreManager(private val nativeLibDir: String) {
    fun corePath(system: System): String? {
        val lib = when (system) {
            System.SNES -> "libsnes9x_libretro_android.so"
            System.ARCADE -> "libfbneo_libretro_android.so"
            System.NES -> "libfceumm_libretro_android.so"
            else -> return null
        }
        return "$nativeLibDir/$lib"
    }
}
