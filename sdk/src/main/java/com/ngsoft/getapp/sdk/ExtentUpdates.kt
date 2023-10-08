package com.ngsoft.getapp.sdk

import android.content.Context
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.tilematrix.TileMatrix
import com.ngsoft.tilescache.TilesCache
import java.time.LocalDateTime

class ExtentUpdates(appCtx: Context) {

    private val matrixGrid: TileMatrix = TileMatrix(appCtx)
    private val cache = TilesCache(appCtx)

    fun getExtentUpdates(extent: MapProperties, zoomLevel: Int, updDate: LocalDateTime): List<MapProperties> {
        val bBox = string2BBox(extent.boundingBox)
        val tilesAndBBoxes = matrixGrid.getTilesAndBBoxes(bBox.left, bBox.bottom, bBox.left, bBox.top, zoomLevel)

        val result = mutableListOf<MapProperties>()
        tilesAndBBoxes.forEach {
            if( !cache.isTileInCache(extent.productId, it.first.x, it.first.y, it.first.zoom, updDate)){
                result.add(MapProperties(extent.productId,
        "${it.second.left},${it.second.bottom},${it.second.right},${it.second.top}",
                    false)
                )
            }
        }

        return result
    }

    private fun string2BBox(bBoxStr: String): com.ngsoft.tilematrix.BBox {
        val digits = bBoxStr.split(',')
        return com.ngsoft.tilematrix.BBox(
            digits[0].toDouble(),
            digits[1].toDouble(),
            digits[2].toDouble(),
            digits[3].toDouble()
        )
    }

}