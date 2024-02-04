package com.ngsoft.getapp.sdk.models

import java.time.OffsetDateTime

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
    var downloadStart: OffsetDateTime?,
    var downloadStop: OffsetDateTime?,
    var downloadDone: OffsetDateTime?,
)

