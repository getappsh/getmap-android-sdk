package com.ngsoft.tilematrix

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class TileMatrix(ctx: Context) {

    private val _getBBoxes: PyObject?

    init {
        if( !Python.isStarted() )
            Python.start(AndroidPlatform(ctx))

        val module = Python.getInstance().getModule( "inspire_tile" )
        _getBBoxes = module["get_bboxes"]
    }

    fun getTile(lon: Double, lat: Double, zoom: Int) : Tile? {
        val module = Python.getInstance().getModule( "inspire_tile" )
        val func = module["get_tile"]
        val tile = func?.call(lon, lat, zoom)

        if(tile != null)
            return Tile(
                tile.asList()[0]?.toInt()!!,
                tile.asList()[1]?.toInt()!!,
                tile.asList()[2]?.toInt()!!,
            )

        return null
    }

    fun getBBoxes(left: Double, bottom: Double, right: Double, top: Double, zoom: Int) : List<BBox> {
//        val module = Python.getInstance().getModule( "inspire_tile" )
//        val func = module["get_bboxes"]

        val pyBBoxes =
            //func?.call(left, bottom, right, top, zoom)?.asList()
            _getBBoxes?.call(left, bottom, right, top, zoom)?.asList()

        val result = mutableListOf<BBox>()

        pyBBoxes?.forEachIndexed { index, pyBBox ->
            //println("pyBBox[$index]: $pyBBox")
            result.add(BBox(
                //pyObject?.asList()?.get(0)?.toDouble()!!,
                pyBBox.asList()[0]?.toDouble()!!,
                pyBBox.asList()[1]?.toDouble()!!,
                pyBBox.asList()[2]?.toDouble()!!,
                pyBBox.asList()[3]?.toDouble()!!
            ))
        }

        return result
    }

    fun getTilesAndBBoxes(left: Double, bottom: Double, right: Double, top: Double, zoom: Int) : List<Pair<Tile, BBox>> {
        val module = Python.getInstance().getModule( "inspire_tile" )
        val func = module["get_tiles_n_bboxes"]

        val pyTilesNBBoxes = func?.call(left, bottom, right, top, zoom)?.asList()
        val result = mutableListOf<Pair<Tile, BBox>>()

        pyTilesNBBoxes?.forEachIndexed { index, pyTileNBBox ->
            //println("pyTileNBBox[$index]: $pyTileNBBox")
            val pyTile = pyTileNBBox.asList()[0]
            val pyBBox = pyTileNBBox.asList()[1]
            result.add(Pair(
                Tile(pyTile.asList()[0]?.toInt()!!,pyTile.asList()[1]?.toInt()!!,pyTile.asList()[2]?.toInt()!!),
                BBox(pyBBox.asList()[0]?.toDouble()!!,pyBBox.asList()[1]?.toDouble()!!,pyBBox.asList()[2]?.toDouble()!!,pyBBox.asList()[3]?.toDouble()!!)
            ))
        }

        return result
    }

}