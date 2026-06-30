package com.gobe.tv.data.system

import org.junit.Assert.assertEquals
import org.junit.Test

class NameCleanerTest {
    private val c = NameCleaner()

    @Test fun stripsExtension() = assertEquals("Contra", c.clean("Contra.nes"))
    @Test fun stripsParenTag() = assertEquals("Super Mario 64", c.clean("Super Mario 64 (USA).z64"))
    @Test fun stripsBracketTag() = assertEquals("Chrono Trigger", c.clean("Chrono Trigger [!].sfc"))
    @Test fun stripsMultiTags() = assertEquals("Zelda", c.clean("Zelda (USA) (Rev 1) [!].z64"))
    @Test fun underscoresToSpaces() = assertEquals("Mega Man 2", c.clean("Mega_Man_2.nes"))
    @Test fun collapsesWhitespace() = assertEquals("Punch Out", c.clean("Punch   Out  .nes"))
    @Test fun keepsInnerNumbers() = assertEquals("F-Zero", c.clean("F-Zero (USA).sfc"))
}
