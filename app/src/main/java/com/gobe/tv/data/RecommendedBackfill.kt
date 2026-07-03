package com.gobe.tv.data

import com.gobe.tv.data.db.GameEntity

/**
 * Given the current games and a "should this game be recommended?" predicate, return the
 * (id -> recommended) changes needed — only for rows whose stored flag differs from the desired
 * value. Pure/testable. Used by [LibraryRepository.rescan] to backfill the flag across the whole
 * library (including already-scanned games) with DB writes only where something changed.
 */
fun recommendedBackfillUpdates(
    games: List<GameEntity>,
    desiredRecommended: (GameEntity) -> Boolean,
): List<Pair<Long, Boolean>> =
    games.mapNotNull { g ->
        val want = desiredRecommended(g)
        if (g.recommended != want) g.id to want else null
    }
