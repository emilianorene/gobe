package com.gobe.tv.emulation

import com.gobe.tv.domain.System

/** Resolves the bundled Libretro core .so path for a system. SNES (snes9x), Arcade (FBNeo),
 *  NES/FDS (FCEUmm) and N64 (Mupen64Plus-Next, GLideN64/GLES3) are wired. */
class CoreManager(private val nativeLibDir: String) {
    fun corePath(system: System): String? {
        val lib = when (system) {
            System.SNES -> "libsnes9x_libretro_android.so"
            System.ARCADE -> "libfbneo_libretro_android.so"
            System.NES -> "libfceumm_libretro_android.so"
            System.N64 -> "libmupen64plus_next_gles3_libretro_android.so"
            else -> return null
        }
        return "$nativeLibDir/$lib"
    }
}
