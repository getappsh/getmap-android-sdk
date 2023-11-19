package com.ngsoft.tilescache

import androidx.room.TypeConverter
import com.ngsoft.getapp.sdk.models.MapDeliveryState

internal class MapDeliveryStateConverter {

    @TypeConverter
    fun toMapDeliveryState(value: String) = enumValueOf<MapDeliveryState>(value)

    @TypeConverter
    fun fromMapDeliveryState(value: MapDeliveryState) = value.name
}