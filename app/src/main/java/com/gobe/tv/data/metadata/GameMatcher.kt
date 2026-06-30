package com.gobe.tv.data.metadata
class GameMatcher(private val normalizer: NameNormalizer) {
    fun match(displayName: String, index: MetadataIndex): GameMeta? =
        index[normalizer.normalize(displayName)]
}
