package GetApp.Client.infrastructure

import android.util.Log
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class OffsetDateTimeAdapter {
    @ToJson
    fun toJson(value: OffsetDateTime): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)
    }

    @FromJson
    fun fromJson(value: String): OffsetDateTime {
        val dateTime = try{
            OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }catch (e: Exception){
            Log.e("OffsetDateTimeAdapter", "fromJson: ${e.message.toString()}, parse to local date time")
            val localDateTime = LocalDateTime.parse("2023-11-12T12:00:00")
            val offset = ZoneOffset.UTC
            OffsetDateTime.of(localDateTime, offset)
        }

        return dateTime
    }

}
