package com.ngsoft.tilematrix

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class TileMatrix(ctx: Context) {
    init {
        if( !Python.isStarted() )
            Python.start( AndroidPlatform(ctx) )
    }

    //TODO
    // 1. wrap that simple POC stuff with all niceties
    // 2. drill down into returned PyObject and extract values into proper Kotlyn types
    fun getTile(lon: Double, lat: Double, zoom: Int) : String {
        val module = Python.getInstance().getModule( "inspire_tile" )
        val func = module["get_tile"]
        val ret = func?.call(lon, lat, zoom)

        val x = ret?.asList()?.get(0)?.toInt()
        println("x is:$x")

        return ret.toString()
    }

    fun getBBoxes(left: Double, bottom: Double, right: Double, top: Double, zoom: Int) : String {
        val module = Python.getInstance().getModule( "inspire_tile" )
        val func = module["get_bboxes"]
        val ret = func?.call(left, bottom, right, top, zoom)
        return ret.toString()
    }

    fun getBBoxesEx(left: Double, bottom: Double, right: Double, top: Double, zoom: Int) : List<BBox> {
        val module = Python.getInstance().getModule( "inspire_tile" )
        val func = module["get_bboxes"]

        val pyBBoxes = func?.call(left, bottom, right, top, zoom)?.asList()

        val result = mutableListOf<BBox>()

        pyBBoxes?.forEachIndexed { index, pyBBox ->
            println("pyBBox[$index]: $pyBBox")
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

}