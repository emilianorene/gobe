package com.gobe.tv.i18n

import android.content.Context
import java.util.Locale

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    SPANISH("es"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/** Stores the UI language preference and applies it to a Context via base-context wrapping. */
object LocaleManager {
    private const val PREFS = "gobe.settings"
    private const val KEY_LANGUAGE = "app_language"

    fun getLanguage(context: Context): AppLanguage =
        AppLanguage.fromTag(prefs(context).getString(KEY_LANGUAGE, AppLanguage.SYSTEM.tag))

    fun setLanguage(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    /** null = use the system locale (no override). */
    fun resolveLocale(language: AppLanguage): Locale? = when (language) {
        AppLanguage.SYSTEM -> null
        AppLanguage.SPANISH -> Locale("es")
        AppLanguage.ENGLISH -> Locale("en")
    }

    /** Returns a context whose resources use the chosen locale; the ORIGINAL context for SYSTEM. */
    fun wrap(context: Context): Context {
        val locale = resolveLocale(getLanguage(context)) ?: return context
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
