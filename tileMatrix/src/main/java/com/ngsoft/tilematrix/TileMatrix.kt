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
        return ret.toString()
    }
}