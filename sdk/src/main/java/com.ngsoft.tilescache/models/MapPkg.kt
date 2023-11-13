package com.ngsoft.tilescache.models

import com.ngsoft.getapp.sdk.models.MapDeliveryState
import java.time.OffsetDateTime

data class MapPkg (
    var id: String,
    var pId: String,
    var bBox: String,
    var reqId: String? = null,
    var JDID: Long? = null, // json download id
    var MDID: Long? = null, // map download id

    var state: MapDeliveryState,
    var statusMessage: String,
    var fileName: String? = null,
    var jsonName: String? = null,
    var url: String? = null,
    var downloadProgress: Int = 0,
    var errorContent: String? = null,
    var cancelDownload: Boolean = false,

    var downloadStart: OffsetDateTime? = null,
    var downloadStop: OffsetDateTime? = null,
    var downloadDone: OffsetDateTime? = null,
)
