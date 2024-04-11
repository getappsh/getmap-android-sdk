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
import com.ngsoft.getapp.sdk.GetMapService

abstract class MapInteraction<T: View>(protected val ctx: Context, protected val service: GetMapService) {

    private val TAG = MapInteraction::class.qualifiedName


    abstract val mapView: T
    abstract fun setMapView(parent: FrameLayout, lifecycle: Lifecycle)
    abstract fun checkBBoxBeforeSent()
    abstract suspend fun renderBaseMap()
    abstract fun onDelivery()


    @RequiresApi(Build.VERSION_CODES.R)
    protected fun getBaseMapLocation(): String{
        val storageManager: StorageManager = ctx.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList.getOrNull(1)?.directory?.absoluteFile ?: ""
        Log.i(TAG, "$volume")

        return "${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg"
    }
}