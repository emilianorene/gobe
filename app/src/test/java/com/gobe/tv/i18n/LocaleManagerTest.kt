package com.gobe.tv.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class LocaleManagerTest {
    @Test fun tagRoundTrip() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
    }
    @Test fun unknownTagFallsBackToSystem() =
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("zz"))
    @Test fun resolveLocale() {
        assertNull(LocaleManager.resolveLocale(AppLanguage.SYSTEM))
        assertEquals(Locale("es"), LocaleManager.resolveLocale(AppLanguage.SPANISH))
        assertEquals(Locale("en"), LocaleManager.resolveLocale(AppLanguage.ENGLISH))
    }
}
