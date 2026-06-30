package com.gobe.tv.data.db

import androidx.room.TypeConverter
import com.gobe.tv.domain.System

class Converters {
    @TypeConverter fun toSystem(v: String): System = System.valueOf(v)
    @TypeConverter fun fromSystem(s: System): String = s.name
}
