package com.gobe.tv.emulation

import com.gobe.tv.domain.System

/** Typed payload for launching the emulator. Map form keeps it JVM-unit-testable; the
 *  Activity maps these keys to/from a real Intent's extras. */
data class EmulatorArgs(
    val gameId: Long,
    val romPath: String,
    val system: System,
    val loadState: Boolean = false,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        KEY_GAME_ID to gameId,
        KEY_ROM_PATH to romPath,
        KEY_SYSTEM to system.name,
        KEY_LOAD_STATE to loadState,
    )

    companion object {
        const val KEY_GAME_ID = "gobe.emu.gameId"
        const val KEY_ROM_PATH = "gobe.emu.romPath"
        const val KEY_SYSTEM = "gobe.emu.system"
        const val KEY_LOAD_STATE = "gobe.emu.loadState"

        fun fromMap(map: Map<String, Any?>): EmulatorArgs = EmulatorArgs(
            gameId = (map[KEY_GAME_ID] as Number).toLong(),
            romPath = map[KEY_ROM_PATH] as String,
            system = System.valueOf(map[KEY_SYSTEM] as String),
            loadState = (map[KEY_LOAD_STATE] as? Boolean) ?: false,
        )
    }
}
