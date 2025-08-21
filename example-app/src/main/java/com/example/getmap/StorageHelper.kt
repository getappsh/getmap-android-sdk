package com.example.getmap

import android.content.Context
import com.ngsoft.getapp.sdk.MapFileManager

class StorageHelper {
     private  val units = arrayOf("B", "KB", "MB", "GB", "TB")

    private fun getAvailableSpaceBytes(context: Context): Long = MapFileManager(context).getAvailableSpaceByPolicy()

    fun getAvailableSpaceMb(context: Context): Double = getAvailableSpaceBytes(context).toDouble() / (1024 * 1024)

    fun getAvailableSpaceInfo(context: Context): Pair<Double, String> {
        var size = getAvailableSpaceBytes(context).toDouble()
        var index = 0
        while (size > 1024 && index < units.size - 1) {
            size /= 1024
            index++
        }
        return Pair(size, units[index])
    }
}