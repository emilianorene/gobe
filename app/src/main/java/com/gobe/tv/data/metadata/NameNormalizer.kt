package com.gobe.tv.data.metadata
/** Normalizes a game name for matching: drop bracket/paren tags, lowercase, drop articles
 *  (leading "a"/"an"/"the" and trailing ", a"/", an"/", the" — No-Intro moves articles to the
 *  end), and remove all non-alphanumerics. Must stay in sync with tools/build-metadata-index. */
class NameNormalizer {
    private val tags = Regex("""[\(\[].*?[\)\]]""")
    private val leadingArticle = Regex("""^(a|an|the)\s+""")
    private val trailingArticle = Regex(""",\s*(a|an|the)$""")
    fun normalize(name: String): String {
        var s = tags.replace(name, " ").lowercase().trim()
        s = trailingArticle.replace(s, "")
        s = leadingArticle.replace(s, "")
        return s.replace(Regex("""[^a-z0-9]"""), "")
    }
}
