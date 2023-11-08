package com.ngsoft.getapp.sdk.models

class MapDownloadData (
    var fileName: String? = null,
    var jsonName: String? = null,
    var deliveryStatus: MapDeliveryState,
    var url: String? = null,
    var statusMessage: String? = null,
    var downloadProgress: Int = 0,
    var errorContent: String? = null,
)

