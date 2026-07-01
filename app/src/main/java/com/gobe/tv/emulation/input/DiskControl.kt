package com.gobe.tv.emulation.input

/**
 * Next disk index for a multi-disk game, wrapping back to 0 after the last disk. Returns [current]
 * unchanged when there is nothing to cycle (count <= 1) or an invalid count. `mod` keeps the result
 * non-negative even if a core reports a negative current index. Pure.
 */
fun nextDisk(current: Int, count: Int): Int =
    if (count <= 1) current else (current + 1).mod(count)
