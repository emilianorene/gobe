package com.gobe.tv.emulation

import android.content.Context
import com.gobe.tv.emulation.input.DeadzoneLevel
import com.gobe.tv.emulation.input.MenuHotkey
import com.gobe.tv.emulation.input.deadzoneFromName
import com.gobe.tv.emulation.input.menuHotkeyFromName

/**
 * Global, read-at-launch game settings persisted in the shared `gobe.settings` prefs (same store as
 * ControllerPrefs / LocaleManager). Enum values are stored by name; parsing falls back to defaults.
 */
object GameSettings {
    private const val PREFS = "gobe.settings"
    private const val KEY_DEADZONE = "game.deadzone"
    private const val KEY_HOTKEY = "game.menuhotkey"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadDeadzone(c: Context): DeadzoneLevel = deadzoneFromName(prefs(c).getString(KEY_DEADZONE, null))
    fun saveDeadzone(c: Context, level: DeadzoneLevel) =
        prefs(c).edit().putString(KEY_DEADZONE, level.name).apply()

    fun loadMenuHotkey(c: Context): MenuHotkey = menuHotkeyFromName(prefs(c).getString(KEY_HOTKEY, null))
    fun saveMenuHotkey(c: Context, hotkey: MenuHotkey) =
        prefs(c).edit().putString(KEY_HOTKEY, hotkey.name).apply()
}
