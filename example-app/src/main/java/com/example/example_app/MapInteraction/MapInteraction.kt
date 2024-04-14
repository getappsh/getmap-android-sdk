package com.example.example_app.MapInteraction

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.example.example_app.PolyObject
import com.ngsoft.getapp.sdk.GetMapService
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

abstract class MapInteraction<T: View>(protected val ctx: Context, protected val service: GetMapService) {

    private val TAG = MapInteraction::class.qualifiedName


    abstract val mapView: T
    abstract fun setMapView(parent: FrameLayout, lifecycle: Lifecycle)
    abstract fun renderBBoxData(): PolyObject?
    abstract suspend fun renderBaseMap()

    @RequiresApi(Build.VERSION_CODES.R)
    protected fun getBaseMapLocation(): String{
        val storageManager: StorageManager = ctx.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList.getOrNull(1)?.directory?.absoluteFile ?: ""
        Log.i(TAG, "$volume")

//        return "${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg"
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).path.toString() + "/gaza1.gpkg"
    }


    protected fun calculateDistance(point1X: Double, point1Y: Double, point2X: Double, point2Y: Double): Double {
        val lat1 = Math.toRadians(point1Y)
        val lon1 = Math.toRadians(point1X)
        val lat2 = Math.toRadians(point2Y)
        val lon2 = Math.toRadians(point2X)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Radius of the Earth in meters
        val radius = 6371000 // 6371 km converted to meters

        return radius * c
    }
}