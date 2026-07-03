package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity
import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendedBackfillTest {
    private fun g(id: Long, name: String, recommended: Boolean) = GameEntity(
        id = id, path = "/$name", system = System.SNES, displayName = name,
        fileName = "$name.sfc", sizeBytes = 0, dateAdded = 0, recommended = recommended,
    )

    @Test fun returnsOnlyChangedRows() {
        val games = listOf(g(1, "a", false), g(2, "b", true), g(3, "c", false))
        val desired = mapOf("a" to true, "b" to true, "c" to false)
        val out = recommendedBackfillUpdates(games) { desired[it.displayName] ?: false }
        assertEquals(listOf(1L to true), out) // only 'a' flips; b already true, c already false
    }

    @Test fun backfillsExistingMatchedRow() { // the blocker: already-scanned row, flag currently false
        val games = listOf(g(1, "Super Metroid", false))
        val out = recommendedBackfillUpdates(games) { it.displayName == "Super Metroid" }
        assertEquals(listOf(1L to true), out)
    }

    @Test fun clearsWhenNoLongerRecommended() {
        val games = listOf(g(1, "x", true))
        val out = recommendedBackfillUpdates(games) { false }
        assertEquals(listOf(1L to false), out)
    }
}
