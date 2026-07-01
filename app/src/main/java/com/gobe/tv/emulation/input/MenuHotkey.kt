package com.gobe.tv.emulation.input

import android.view.KeyEvent
import androidx.annotation.StringRes
import com.gobe.tv.R

/**
 * The two-button combo that opens the pause menu in-game. Only the simultaneous combo triggers the
 * menu; the individual buttons still reach the core so they stay usable in games. (Back and the
 * gamepad Home/Mode button always toggle the menu regardless of this setting.)
 */
enum class MenuHotkey(val codes: Set<Int>, @StringRes val labelRes: Int) {
    SELECT_START(setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START), R.string.hotkey_select_start),
    L1_R1(setOf(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1), R.string.hotkey_l1_r1),
    L3_R3(setOf(KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR), R.string.hotkey_l3_r3)
}

/** True only when every key of the hotkey combo is currently held. */
fun isHotkeyCombo(held: Set<Int>, hotkey: MenuHotkey): Boolean =
    hotkey.codes.all { it in held }

/** Defensive parse of a persisted enum name; unknown/missing -> SELECT_START (the default). */
fun menuHotkeyFromName(name: String?): MenuHotkey =
    MenuHotkey.values().firstOrNull { it.name == name } ?: MenuHotkey.SELECT_START
