package com.gobe.tv.emulation.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControllerAssignmentsTest {
    @Test fun assignSetsPort() {
        val a = ControllerAssignments().assign("dev-A", 1)
        assertEquals(1, a.portFor("dev-A"))
    }
    @Test fun assignEnforcesUniquePort() {
        val a = ControllerAssignments().assign("dev-A", 0).assign("dev-B", 0)
        assertNull(a.portFor("dev-A"))       // A bumped off port 0
        assertEquals(0, a.portFor("dev-B"))
    }
    @Test fun reassigningSameDeviceMovesIt() {
        val a = ControllerAssignments().assign("dev-A", 0).assign("dev-A", 2)
        assertEquals(2, a.portFor("dev-A"))
    }
    @Test fun clearRemoves() {
        val a = ControllerAssignments().assign("dev-A", 2).clear("dev-A")
        assertNull(a.portFor("dev-A"))
    }
    @Test fun portForDeviceDefaultsToP1() {
        val a = ControllerAssignments().assign("dev-A", 3)
        assertEquals(3, portForDevice("dev-A", a))
        assertEquals(0, portForDevice("dev-Z", a))  // unknown -> P1
        assertEquals(0, portForDevice(null, a))      // null (unresolvable deviceId) -> P1
    }
}
