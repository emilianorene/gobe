package com.gobe.tv.ui.library

/** In-library collection filter, replacing the old top-level Recommended/Favorites sections. */
enum class CollectionFilter { ALL, RECOMMENDED, FAVORITES }

/** DB flags a [CollectionFilter] maps to. */
data class CollectionFlags(val recommendedOnly: Boolean, val favoritesOnly: Boolean)

fun collectionFlags(filter: CollectionFilter): CollectionFlags = when (filter) {
    CollectionFilter.ALL -> CollectionFlags(false, false)
    CollectionFilter.RECOMMENDED -> CollectionFlags(true, false)
    CollectionFilter.FAVORITES -> CollectionFlags(false, true)
}
