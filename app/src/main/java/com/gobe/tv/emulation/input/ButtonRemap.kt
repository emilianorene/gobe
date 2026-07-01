package com.gobe.tv.emulation.input

/** Per-controller custom button remap: physical Android keycode -> target keycode. Pure. */
data class ButtonRemap(val byPhysical: Map<Int, Int> = emptyMap()) {
    /** Bind physical->target, enforcing one physical per target (remove any other physical on it). */
    fun bind(physical: Int, target: Int): ButtonRemap {
        val m = byPhysical.filterValues { it != target }.toMutableMap()
        m[physical] = target
        return ButtonRemap(m)
    }

    fun reset(): ButtonRemap = ButtonRemap(emptyMap())

    /** Which physical key currently triggers this target (for the UI), or null. */
    fun physicalFor(target: Int): Int? = byPhysical.entries.firstOrNull { it.value == target }?.key
}

/** Custom binding wins; else the Swap preset; else passthrough. */
fun applyMapping(code: Int, remap: ButtonRemap, swaps: ButtonSwaps): Int =
    remap.byPhysical[code] ?: remapCode(code, swaps)

fun serializeRemap(r: ButtonRemap): String =
    r.byPhysical.entries.joinToString(",") { "${it.key}:${it.value}" }

fun parseRemap(s: String?): ButtonRemap {
    if (s.isNullOrBlank()) return ButtonRemap()
    val m = mutableMapOf<Int, Int>()
    for (part in s.split(",")) {
        val kv = part.split(":")
        if (kv.size != 2) continue
        val p = kv[0].toIntOrNull() ?: continue
        val t = kv[1].toIntOrNull() ?: continue
        m[p] = t
    }
    return ButtonRemap(m)
}
