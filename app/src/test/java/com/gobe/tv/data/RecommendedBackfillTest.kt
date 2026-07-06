package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendedBackfillTest {
    private fun g(id: Long, name: String, rec: Boolean, desc: String?, cover: String?) = GameEntity(
        id = id, path = "/$name", system = System.SNES, displayName = name,
        fileName = "$name.sfc", sizeBytes = 0, dateAdded = 0, recommended = rec, description = desc, igdbCover = cover,
    )
    @Test fun returnsOnlyChangedRows() {
        val games = listOf(
            g(1, "a", false, null, null),                 // will change (recommended -> true)
            g(2, "b", true, "d", "c"),                    // unchanged
        )
        val desired = mapOf(
            "a" to IndexExtras(true, null, null),
            "b" to IndexExtras(true, "d", "c"),
        )
        val out = indexExtrasBackfillUpdates(games) { desired[it.displayName]!! }
        assertEquals(listOf(1L to IndexExtras(true, null, null)), out)
    }
    @Test fun backfillsDescriptionAndCoverOnExistingRow() {
        val games = listOf(g(1, "Super Metroid", true, null, null))
        val out = indexExtrasBackfillUpdates(games) { IndexExtras(true, "Explore Zebes.", "co1") }
        assertEquals(listOf(1L to IndexExtras(true, "Explore Zebes.", "co1")), out)
    }
}
