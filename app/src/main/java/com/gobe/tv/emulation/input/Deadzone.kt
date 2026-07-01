package com.gobe.tv.emulation.input

import kotlin.math.abs
import kotlin.math.sign

/** Analog-stick deadzone strength. [threshold] is the fraction of travel ignored near center. */
enum class DeadzoneLevel(val threshold: Float) {
    OFF(0f), LOW(0.10f), MEDIUM(0.15f), HIGH(0.25f)
}

/**
 * Per-axis deadzone with rescale: below [threshold] -> 0; at/above it, rescale so the output is 0 at
 * the threshold edge and reaches ±1 at ±1 (no jump at the edge). A [threshold] <= 0 is a passthrough
 * (OFF). Result is clamped to [-1, 1]. Applied per axis by the caller (D-pad/HAT is NOT deadzoned).
 */
fun applyDeadzone(value: Float, threshold: Float): Float {
    if (threshold <= 0f) return value
    val magnitude = abs(value)
    if (magnitude < threshold) return 0f
    val scaled = (magnitude - threshold) / (1f - threshold)
    return (sign(value) * scaled).coerceIn(-1f, 1f)
}

/** Defensive parse of a persisted enum name; unknown/missing -> LOW (the default). */
fun deadzoneFromName(name: String?): DeadzoneLevel =
    DeadzoneLevel.values().firstOrNull { it.name == name } ?: DeadzoneLevel.LOW
