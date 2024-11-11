package com.ngsoft.tilescache.models

import com.ngsoft.tilescache.converters.DeliveryFlowStateConverter
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.tilescache.converters.MapDeliveryStateConverter
import com.ngsoft.tilescache.converters.TimeStampConverter
import java.time.LocalDateTime
import java.time.ZoneOffset


data class DownloadMetadata(
    var validationAttempt: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var connectionAttempt: Int = 0,

    @ColumnInfo(defaultValue = "0")
    var mapAttempt: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var mapDone: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    var jsonAttempt: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var jsonDone: Boolean = false,
)
@Entity
data class MapPkg (
    var pId: String,
    var bBox: String,
//    real footprint from the json
    var footprint: String? = null,

    @TypeConverters(DeliveryFlowStateConverter::class)
    var flowState: DeliveryFlowState = DeliveryFlowState.START,

    var reqId: String? = null,
    var JDID: Long? = null, // json download id
    var MDID: Long? = null, // map download id

    @TypeConverters(MapDeliveryStateConverter::class)
    var state: MapDeliveryState,

    var statusMsg: String,
    var fileName: String? = null,
    var jsonName: String? = null,
    var url: String? = null,
    var path: String? = null,
    var downloadProgress: Int = 0,
    var statusDescr: String? = null,
    var cancelDownload: Boolean = false,

    @TypeConverters(TimeStampConverter::class)
    var downloadStart: LocalDateTime? = null,

    @TypeConverters(TimeStampConverter::class)
    var downloadStop: LocalDateTime? = null,

    @TypeConverters(TimeStampConverter::class)
    var downloadDone: LocalDateTime? = null,

    @Embedded
    var metadata: DownloadMetadata = DownloadMetadata(),

    @ColumnInfo(defaultValue = "1")
    var isUpdated: Boolean = true,
){
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

//    @ColumnInfo(defaultValue = "strftime('%s', 'now')")
    @TypeConverters(TimeStampConverter::class)
    var reqDate: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
}

