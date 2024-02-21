package com.ngsoft.getapp.sdk.models

import java.time.OffsetDateTime

class MapData(
    var id: String? = null,
    var footprint: String? = null,
    var fileName: String? = null,
    var jsonName: String? = null,
    var deliveryState: MapDeliveryState,
    var url: String? = null,
    var statusMsg: String? = null,
    var progress: Int = 0,
    var statusDescr: String? = null,
    var isUpdated: Boolean = true,
    var downloadStart: OffsetDateTime?,
    var downloadStop: OffsetDateTime?,
    var downloadDone: OffsetDateTime?,
)

