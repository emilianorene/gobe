package com.gobe.tv.data.metadata
/** Normalizes a game name for matching: drop bracket/paren tags, lowercase, drop a leading
 *  "the", and remove all non-alphanumerics. */
class NameNormalizer {
    private val tags = Regex("""[\(\[].*?[\)\]]""")
    fun normalize(name: String): String {
        var s = tags.replace(name, " ").lowercase().trim()
        if (s.startsWith("the ")) s = s.removePrefix("the ")
        return s.replace(Regex("""[^a-z0-9]"""), "")
    }
}
