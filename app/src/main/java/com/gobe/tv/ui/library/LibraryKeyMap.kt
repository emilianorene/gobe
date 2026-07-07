package com.gobe.tv.ui.library

import android.view.KeyEvent

/** Library gamepad shortcut actions (page-jump within the poster rail). */
enum class LibraryKeyAction { PagePrev, PageNext }

/** Maps a gamepad key code to a library page-jump action (L1→prev, R1→next), or null. Pure.
 *  Note: inside the library, L1/R1 are repurposed from their Home meaning (Search/Settings). */
fun keyToLibraryAction(keyCode: Int): LibraryKeyAction? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L1 -> LibraryKeyAction.PagePrev
    KeyEvent.KEYCODE_BUTTON_R1 -> LibraryKeyAction.PageNext
    else -> null
}
