package com.ngsoft.tilescache

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneOffset

class TimeStampConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return if (value == null) null else LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC)
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }
}