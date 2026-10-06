package com.github.altusea.thatday.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTimeKind(value: TimeKind): String = value.name

    @TypeConverter
    fun toTimeKind(value: String): TimeKind = TimeKind.valueOf(value)
}
