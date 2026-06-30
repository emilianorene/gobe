package com.gobe.tv.data.metadata

import org.json.JSONObject

data class GameMeta(val boxart: String?, val players: Int?)

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
                )
            }
            return MetadataIndex(m)
        }
    }
}
