package com.ngsoft.getapp.sdk.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal object DateHelper {

    fun parse(date: String, formatter: DateTimeFormatter?): OffsetDateTime?{
        return try {
            OffsetDateTime.parse(date, formatter)
        }catch (error: DateTimeParseException){
            null
        }
    }

}