package com.gobe.tv.controllers

import android.content.Context
import com.gobe.tv.emulation.input.ControllerAssignments

/** Persists ControllerAssignments in the shared gobe.settings prefs, one key per descriptor
 *  (descriptors are opaque vendor strings, so per-key storage avoids delimiter issues). */
object ControllerPrefs {
    private const val PREFS = "gobe.settings"
    private const val PREFIX = "ctrl.port."

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
}
