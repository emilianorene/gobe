package com.gobe.tv.emulation

import java.io.File

/** Stores emulator save states under <filesDir>/states/<gameId>.state (atomic writes). */
class SaveStateStore(filesDir: File) {
    private val dir = File(filesDir, "states").apply { mkdirs() }

    private fun stateFile(gameId: Long) = File(dir, "$gameId.state")

    fun hasState(gameId: Long): Boolean = stateFile(gameId).exists()

    fun readState(gameId: Long): ByteArray? =
        stateFile(gameId).takeIf { it.exists() }?.readBytes()

    /** temp-file-then-rename so a crash mid-write never leaves a truncated .state. */
    fun writeState(gameId: Long, bytes: ByteArray) {
        val tmp = File(dir, "$gameId.state.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(stateFile(gameId))) {
            stateFile(gameId).writeBytes(bytes) // fallback
            tmp.delete()
        }
    }
}
