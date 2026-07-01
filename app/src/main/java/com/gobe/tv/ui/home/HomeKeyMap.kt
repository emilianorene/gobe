package com.gobe.tv.ui.home

import android.view.KeyEvent

/** Home gamepad shortcut actions. */
enum class HomeKeyAction { Search, Settings }

/** Maps a gamepad key code to a Home shortcut action (L1→Search, R1→Settings), or null. Pure. */
fun keyToHomeAction(keyCode: Int): HomeKeyAction? = when (keyCode) {
    KeyEvent.KEYCODE_BUTTON_L1 -> HomeKeyAction.Search
    KeyEvent.KEYCODE_BUTTON_R1 -> HomeKeyAction.Settings
    else -> null
}
