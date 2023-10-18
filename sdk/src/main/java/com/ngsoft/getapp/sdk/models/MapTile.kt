package com.ngsoft.getapp.sdk.models

import java.time.LocalDateTime

/**
 * Map tile
 *
 * @property productId product Id of layer
 * @property boundingBox tile's bounding box
 * @property x tile's WorldCRS84Quad's X
 * @property y tile's WorldCRS84Quad's Y
 * @property zoom tile's WorldCRS84Quad's zoom level
 * @property dateUpdated tile's layer's update date
 * @property fileName tile file name (when delivered)
 * @constructor Create empty Map tile
 */
data class MapTile(
    val productId: String,
    val boundingBox: String,
    val x: Int,
    val y: Int,
    val zoom: Int,
    val dateUpdated: LocalDateTime,
    var fileName: String?
)
