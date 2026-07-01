package com.gobe.tv.emulation.input

/** Which controller (by stable device descriptor) drives which 0-based player port. Pure. */
data class ControllerAssignments(val byDescriptor: Map<String, Int> = emptyMap()) {
    /** Assign a descriptor to a port, removing any OTHER descriptor currently on that port. */
    fun assign(descriptor: String, port: Int): ControllerAssignments {
        val m = byDescriptor.filterValues { it != port }.toMutableMap()
        m[descriptor] = port
        return ControllerAssignments(m)
    }

    fun clear(descriptor: String): ControllerAssignments =
        ControllerAssignments(byDescriptor - descriptor)

    fun portFor(descriptor: String): Int? = byDescriptor[descriptor]
}

/** The port a device's input should go to: its assignment, or P1 (0) by default.
 *  descriptor is null when a runtime deviceId can't be resolved — that maps to P1. */
fun portForDevice(descriptor: String?, assignments: ControllerAssignments): Int =
    descriptor?.let { assignments.portFor(it) } ?: 0
