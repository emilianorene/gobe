package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity

/** The index-derived fields refreshed across the whole library on rescan. */
data class IndexExtras(val recommended: Boolean, val description: String?, val igdbCover: String?)

/**
 * Given the current games and a "what should this game's index-derived fields be?" function, return
 * the (id -> IndexExtras) changes needed — only for rows where any of the three differ. Pure/testable.
 * Used by [LibraryRepository.rescan] to backfill recommended/description/igdbCover across the whole
 * library (including already-scanned games), writing only where something changed.
 */
fun indexExtrasBackfillUpdates(
    games: List<GameEntity>,
    desired: (GameEntity) -> IndexExtras,
): List<Pair<Long, IndexExtras>> =
    games.mapNotNull { g ->
        val want = desired(g)
        if (g.recommended != want.recommended || g.description != want.description || g.igdbCover != want.igdbCover)
            g.id to want else null
    }
