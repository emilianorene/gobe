package com.gobe.tv.emulation.input

import com.gobe.tv.domain.System

/**
 * True when a game is a Famicom Disk System title. fceumm does NOT expose FDS disk sides through the
 * libretro disk-control interface (so `getAvailableDisks()` is always 0); instead it maps FDS disk
 * operations to RetroPad buttons — **L** = change disk side, **R** = insert/eject. The emulator
 * therefore drives a "change disk" by sending those buttons to the core, but only for FDS games.
 * Pure/testable.
 */
fun isFdsGame(system: System, romPath: String): Boolean =
    system == System.NES && romPath.endsWith(".fds", ignoreCase = true)
