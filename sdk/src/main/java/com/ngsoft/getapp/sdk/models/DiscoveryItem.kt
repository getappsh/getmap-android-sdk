package com.ngsoft.getapp.sdk.models

/**
 * Discovery item - describes map layer in catalog
 *
 * @property productId - layer's product Id
 * @property productName - layer's product name
 * @property boundingBox - layer's bounding box
 * @property updateDate - layer's update date
 * @constructor Create empty Discovery item
 */
data class DiscoveryItem(
    val productId: String,
    val productName: String,
    val boundingBox: String,
    val updateDate: java.time.OffsetDateTime
)
