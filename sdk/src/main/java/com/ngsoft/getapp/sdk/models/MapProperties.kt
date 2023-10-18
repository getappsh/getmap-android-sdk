package com.ngsoft.getapp.sdk.models

/**
 * Map properties
 *
 * @property productId - product id of the layer of interest
 * @property boundingBox - bounding box
 * @property isBest
 * @constructor Create empty Map properties
 */
data class MapProperties (
    val productId: String,
    val boundingBox: String,
    val isBest: Boolean
)
