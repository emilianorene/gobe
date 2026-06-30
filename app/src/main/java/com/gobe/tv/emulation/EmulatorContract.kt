package com.gobe.tv.emulation

import android.content.Intent
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

        fun fromIntent(intent: Intent): EmulatorArgs = EmulatorArgs(
            gameId = intent.getLongExtra(KEY_GAME_ID, -1L),
            romPath = intent.getStringExtra(KEY_ROM_PATH) ?: error("EmulatorArgs: missing romPath"),
            system = System.valueOf(intent.getStringExtra(KEY_SYSTEM) ?: System.SNES.name),
            loadState = intent.getBooleanExtra(KEY_LOAD_STATE, false),
        )
    }
}

/** Puts this EmulatorArgs onto an Intent (used by the detail screen to launch the emulator). */
fun Intent.putEmulatorArgs(args: EmulatorArgs): Intent {
    putExtra(EmulatorArgs.KEY_GAME_ID, args.gameId)
    putExtra(EmulatorArgs.KEY_ROM_PATH, args.romPath)
    putExtra(EmulatorArgs.KEY_SYSTEM, args.system.name)
    putExtra(EmulatorArgs.KEY_LOAD_STATE, args.loadState)
    return this
}
