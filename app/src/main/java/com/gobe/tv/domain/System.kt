package com.gobe.tv.domain

enum class System(val displayName: String, val extensions: Set<String>) {
    NES("NES", setOf("nes", "fds")),
    SNES("SNES", setOf("smc", "sfc")),
    N64("N64", setOf("z64", "n64", "v64")),
    ARCADE("Arcade", setOf("zip")); // unverified in Fase 1

    companion object {
        /** Lowercase extension (no dot) -> System, or null if unknown. */
        fun fromExtension(ext: String): System? {
            val e = ext.lowercase()
            return entries.firstOrNull { e in it.extensions }
        }
    }
}
