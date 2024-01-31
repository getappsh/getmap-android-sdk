package com.ngsoft.getapp.sdk.models

import java.time.LocalDateTime

class MapDownloadData(
    var id: String? = null,
    var footprint: String? = null,
    var fileName: String? = null,
    var jsonName: String? = null,
    var deliveryStatus: MapDeliveryState,
    var url: String? = null,
    var statusMessage: String? = null,
    var downloadProgress: Int = 0,
    var errorContent: String? = null,
    var isUpdated: Boolean = true,
    var downloadStart: LocalDateTime?,
    var downloadStop: LocalDateTime?,
    var downloadDone: LocalDateTime?,
)

