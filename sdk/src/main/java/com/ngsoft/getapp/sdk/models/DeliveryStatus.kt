package com.ngsoft.getapp.sdk.models

import java.time.OffsetDateTime

data class DeliveryStatus (
    val state: MapDeliveryState,
    val reqId: String,
    val progress: Int?,
    val start: OffsetDateTime?,
    val stop: OffsetDateTime?,
    val done: OffsetDateTime?,
    val downloaded: Long? = null,
    val downloadedBytesPerSecond: Long? = null,
    val etaInMilliSeconds: Long? = null,
    )