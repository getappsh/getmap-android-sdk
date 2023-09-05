package com.ngsoft.getapp.sdk.models

class MapImportDeliveryStatus {
    var importRequestId: String? = null
    var state: MapDeliveryState? = null
    var message: Status? = null
    var estimationTimeToDownload: String? = null
    var downloadData = 0f
}