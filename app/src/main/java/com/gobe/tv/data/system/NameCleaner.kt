package com.gobe.tv.data.system

class NameCleaner {
    private val tagRegex = Regex("""[\(\[].*?[\)\]]""")
    fun clean(fileName: String): String {
        val noExt = fileName.substringBeforeLast('.', fileName)
        val noTags = tagRegex.replace(noExt, " ")
        val spaced = noTags.replace('_', ' ').replace('.', ' ')
        return spaced.trim().replace(Regex("""\s+"""), " ")
    }
}
