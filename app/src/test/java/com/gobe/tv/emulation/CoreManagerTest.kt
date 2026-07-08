package com.gobe.tv.emulation

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreManagerTest {
    private val cm = CoreManager(nativeLibDir = "/data/app/x/lib/arm")

    @Test fun snesResolvesToBundledCore() =
        assertEquals("/data/app/x/lib/arm/libsnes9x_libretro_android.so", cm.corePath(System.SNES))

    @Test fun arcadeResolvesToBundledCore() =
        assertEquals("/data/app/x/lib/arm/libfbneo_libretro_android.so", cm.corePath(System.ARCADE))

    @Test fun nesResolvesToBundledCore() =
        assertEquals("/data/app/x/lib/arm/libfceumm_libretro_android.so", cm.corePath(System.NES))

    @Test fun n64ResolvesToBundledCore() =
        assertEquals(
            "/data/app/x/lib/arm/libmupen64plus_next_gles3_libretro_android.so",
            cm.corePath(System.N64),
        )
}
