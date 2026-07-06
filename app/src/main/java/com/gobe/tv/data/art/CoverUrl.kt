package com.gobe.tv.data.art

import com.gobe.tv.domain.Game

/** IGDB CDN cover URL for an image id (t_cover_big), or null when absent. Pure. */
fun igdbCoverUrl(imageId: String?): String? =
    if (imageId.isNullOrBlank()) null
    else "https://images.igdb.com/igdb/image/upload/t_cover_big/$imageId.jpg"

/**
 * Resolve a game's cover URL with a deterministic fallback: libretro thumbnail (when the game matched
 * a boxart name) -> IGDB cover (when present) -> null (Coil then shows the branded placeholder). Pure.
 */
fun coverUrl(game: Game, builder: BoxartUrlBuilder = BoxartUrlBuilder()): String? =
    builder.url(game.system, game.boxartName) ?: igdbCoverUrl(game.igdbCover)
