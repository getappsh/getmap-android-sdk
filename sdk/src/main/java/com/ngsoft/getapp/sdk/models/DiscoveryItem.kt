package com.ngsoft.getapp.sdk.models

import java.math.BigDecimal

/**
 * Discovery item - describes map layer in catalog
 *
 * @property id - layer's Id
 * @property productId - layer's product Id
 * @property productName - layer's product name
 * @property productVersion - layer's productVersion
 * @property productType - layer's productType
 * @property productSubType - layer's productSubType
 * @property description - layer's description
 * @property imagingTimeBeginUTC - layer's imagingTimeBeginUTC
 * @property imagingTimeEndUTC - layer's imagingTimeEndUTC
 * @property maxResolutionDeg - layer's maxResolution
 * @property footprint - layer's footprint
 * @property region - layer's region
 * @property ingestionDate - layer's ingestionDate
 * @constructor Create empty Discovery item
 */
data class DiscoveryItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productVersion: String?,
    val productType: String?,
    val productSubType: String?,
    val description: String?,
    val imagingTimeBeginUTC: java.time.OffsetDateTime?,
    val imagingTimeEndUTC: java.time.OffsetDateTime?,
    val maxResolutionDeg: BigDecimal,
    val footprint: String,
    val transparency: String,
    val region: String?,
    val ingestionDate: java.time.OffsetDateTime?,
)
