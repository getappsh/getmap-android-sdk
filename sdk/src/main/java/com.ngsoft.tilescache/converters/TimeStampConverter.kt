package com.ngsoft.tilescache.converters

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class TimeStampConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return if (value == null) null else LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC)
    }

    @TypeConverter
    fun toTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }
}