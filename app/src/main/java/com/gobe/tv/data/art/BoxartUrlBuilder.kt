package com.gobe.tv.data.art

import com.gobe.tv.domain.System
import java.net.URLEncoder

/** Builds a libretro-thumbnails box-art URL for a (system, canonical name). */
class BoxartUrlBuilder {
    fun url(system: System, name: String?): String? {
        if (name.isNullOrBlank()) return null
        val folder = folder(system) ?: return null
        val sanitized = name.replace(Regex("""[&*/:`<>?\\|"]"""), "_")
        return "https://thumbnails.libretro.com/${enc(folder)}/Named_Boxarts/${enc(sanitized)}.png"
    }

    private fun folder(system: System): String? = when (system) {
        System.SNES -> "Nintendo - Super Nintendo Entertainment System"
        System.NES -> "Nintendo - Nintendo Entertainment System"
        System.N64 -> "Nintendo - Nintendo 64"
        System.ARCADE -> "MAME"
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
