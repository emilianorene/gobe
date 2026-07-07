package com.gobe.tv.ui.library

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryKeyMapTest {
    @Test fun l1MapsToPagePrev() =
        assertEquals(LibraryKeyAction.PagePrev, keyToLibraryAction(KeyEvent.KEYCODE_BUTTON_L1))

    @Test fun r1MapsToPageNext() =
        assertEquals(LibraryKeyAction.PageNext, keyToLibraryAction(KeyEvent.KEYCODE_BUTTON_R1))

    @Test fun otherKeysMapToNull() {
        assertNull(keyToLibraryAction(KeyEvent.KEYCODE_BUTTON_A))
        assertNull(keyToLibraryAction(KeyEvent.KEYCODE_DPAD_DOWN))
    }
}
