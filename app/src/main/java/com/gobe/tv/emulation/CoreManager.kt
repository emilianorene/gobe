package com.gobe.tv.emulation

import com.gobe.tv.domain.System

/** Resolves the bundled Libretro core .so path for a system. Only SNES is wired in Fase 2. */
class CoreManager(private val nativeLibDir: String) {
    fun corePath(system: System): String? {
        val lib = when (system) {
            System.SNES -> "libsnes9x_libretro_android.so"
            else -> return null
        }
        return "$nativeLibDir/$lib"
    }
}
