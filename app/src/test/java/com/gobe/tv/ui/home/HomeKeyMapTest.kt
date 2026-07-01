package com.gobe.tv.ui.home

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeKeyMapTest {
    @Test fun l1MapsToSearch() =
        assertEquals(HomeKeyAction.Search, keyToHomeAction(KeyEvent.KEYCODE_BUTTON_L1))

    @Test fun r1MapsToSettings() =
        assertEquals(HomeKeyAction.Settings, keyToHomeAction(KeyEvent.KEYCODE_BUTTON_R1))

    @Test fun otherKeysMapToNull() {
        assertNull(keyToHomeAction(KeyEvent.KEYCODE_BUTTON_A))
        assertNull(keyToHomeAction(KeyEvent.KEYCODE_DPAD_CENTER))
    }
}
