package com.ngsoft.getapp.sdk.models

import java.math.BigDecimal

/**
 * Discovery item - describes map layer in catalog
 *
 * @property productId - layer's product Id
 * @property productName - layer's product name
 * @property boundingBox - layer's bounding box
 * @property footprint - layer's footprint
 * @property maxResolution - layer's maxResolution
 * @property region - layer's region
 * @property productVersion - layer's productVersion
 * @property ingestionDate - layer's ingestionDate
 * @property takenDate - layer's takenDate
 * @property updateDate - layer's update date
 * @constructor Create empty Discovery item
 */
data class DiscoveryItem(
    val productId: String,
    val productName: String,
    val boundingBox: String,
    val footprint: String,
    val maxResolution: BigDecimal,
    val region: String?,
    val productVersion: String?,
    val ingestionDate: java.time.OffsetDateTime?,
    val takenDate: java.time.OffsetDateTime?,
    val updateDate: java.time.OffsetDateTime?,
)
