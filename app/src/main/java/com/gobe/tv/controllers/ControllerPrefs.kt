package com.gobe.tv.controllers

import android.content.Context
import com.gobe.tv.emulation.input.ButtonSwaps
import com.gobe.tv.emulation.input.ControllerAssignments

/** Persists ControllerAssignments in the shared gobe.settings prefs, one key per descriptor
 *  (descriptors are opaque vendor strings, so per-key storage avoids delimiter issues). */
object ControllerPrefs {
    private const val PREFS = "gobe.settings"
    private const val PREFIX = "ctrl.port."
    private const val SWAP_AB = "ctrl.swapab."
    private const val SWAP_XY = "ctrl.swapxy."

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): ControllerAssignments {
        val map = prefs(context).all
            .filterKeys { it.startsWith(PREFIX) }
            .mapNotNull { (k, v) -> (v as? Int)?.let { k.removePrefix(PREFIX) to it } }
            .toMap()
        return ControllerAssignments(map)
    }

    fun save(context: Context, a: ControllerAssignments) {
        val e = prefs(context).edit()
        prefs(context).all.keys.filter { it.startsWith(PREFIX) }.forEach { e.remove(it) }
        a.byDescriptor.forEach { (desc, port) -> e.putInt(PREFIX + desc, port) }
        e.apply()
    }

    fun loadSwaps(context: Context): Map<String, ButtonSwaps> {
        val p = prefs(context)
        val descriptors = p.all.keys
            .filter { it.startsWith(SWAP_AB) || it.startsWith(SWAP_XY) }
            .map { it.removePrefix(SWAP_AB).removePrefix(SWAP_XY) }
            .toSet()
        return descriptors.associateWith { d ->
            ButtonSwaps(p.getBoolean(SWAP_AB + d, false), p.getBoolean(SWAP_XY + d, false))
        }
    }

    fun saveSwaps(context: Context, descriptor: String, s: ButtonSwaps) {
        prefs(context).edit()
            .putBoolean(SWAP_AB + descriptor, s.swapAB)
            .putBoolean(SWAP_XY + descriptor, s.swapXY)
            .apply()
    }
}
