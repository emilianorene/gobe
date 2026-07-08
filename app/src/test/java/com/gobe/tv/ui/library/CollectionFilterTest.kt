package com.gobe.tv.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionFilterTest {
    @Test fun allClearsBothFlags() =
        assertEquals(CollectionFlags(false, false), collectionFlags(CollectionFilter.ALL))
    @Test fun recommendedSetsOnlyRecommended() =
        assertEquals(CollectionFlags(true, false), collectionFlags(CollectionFilter.RECOMMENDED))
    @Test fun favoritesSetsOnlyFavorites() =
        assertEquals(CollectionFlags(false, true), collectionFlags(CollectionFilter.FAVORITES))
}
