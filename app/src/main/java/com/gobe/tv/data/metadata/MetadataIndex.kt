package com.gobe.tv.data.metadata

import org.json.JSONObject

data class GameMeta(
    val boxart: String?,
    val players: Int?,
    val genre: String? = null,
    val year: Int? = null,
    val recommended: Boolean = false,
)

class MetadataIndex(private val map: Map<String, GameMeta>) {
    operator fun get(normalized: String): GameMeta? = map[normalized]
    companion object {
        fun parse(json: String): MetadataIndex {
            val obj = JSONObject(json)
            val m = HashMap<String, GameMeta>(obj.length())
            for (key in obj.keys()) {
                val o = obj.getJSONObject(key)
                m[key] = GameMeta(
                    boxart = o.optString("boxart").ifBlank { null },
                    players = if (o.has("players")) o.optInt("players") else null,
                    genre = o.optString("genre").ifBlank { null },
                    year = if (o.has("year")) o.optInt("year") else null,
                    recommended = o.optBoolean("recommended", false),
                )
            }
            return MetadataIndex(m)
        }
    }
}
