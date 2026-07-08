package com.gobe.tv.ui.home

import com.gobe.tv.domain.System

/** A console shown in the carousel: the system and how many games it has. */
data class ConsoleEntry(val system: System, val count: Int)

/** Consoles with at least one game, in canonical [System.entries] order. Pure. */
fun visibleConsoles(counts: Map<System, Int>): List<ConsoleEntry> =
    System.entries.mapNotNull { s ->
        val c = counts[s] ?: 0
        if (c > 0) ConsoleEntry(s, c) else null
    }

/** First visible console, or null when the library is empty. Pure. */
fun defaultFocus(visible: List<ConsoleEntry>): System? = visible.firstOrNull()?.system

/** Move focus by [delta] within [visible], clamped to the ends (no wrap). Pure. */
fun moveFocus(visible: List<ConsoleEntry>, current: System?, delta: Int): System? {
    if (visible.isEmpty()) return null
    val idx = visible.indexOfFirst { it.system == current }.let { if (it < 0) 0 else it }
    return visible[(idx + delta).coerceIn(0, visible.size - 1)].system
}
