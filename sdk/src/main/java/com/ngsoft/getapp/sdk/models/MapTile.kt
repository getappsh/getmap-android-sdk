package com.ngsoft.getapp.sdk.models

import java.time.LocalDateTime

data class MapTile(
    val productId: String,
    val boundingBox: String,
    val x: Int,
    val y: Int,
    val zoom: Int,
    val dateUpdated: LocalDateTime
)
