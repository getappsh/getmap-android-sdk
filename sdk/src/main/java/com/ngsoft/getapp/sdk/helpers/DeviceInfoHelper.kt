package com.ngsoft.getapp.sdk.helpers

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.ngsoft.getapp.sdk.MapFileManager
import com.ngsoft.getapp.sdk.Pref

internal class DeviceInfoHelper private constructor(context: Context) {

    private val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val pref = Pref.getInstance(context)
    private val mapFileManager = MapFileManager(context)
    fun batteryPower(): Int{
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun deviceId(): String {
        return pref.deviceId
    }

    fun generatedDeviceId(): String {
        return pref.generateDeviceId()
    }

// TODO move logic to here? and remove mapFileManager reference
    fun getAvailableSpaceByPolicy(): Long{
        return mapFileManager.getAvailableSpaceByPolicy()
    }
    fun isInternetAvailable(): Boolean {
        val networkCapabilities = getNetworkCapabilities()
        return networkCapabilities != null &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun getBandwidthQuality(): Int? {
        val networkCapabilities = getNetworkCapabilities() ?: return null

        val downloadSpeedMbps = networkCapabilities.linkDownstreamBandwidthKbps / 1000 // Convert to Mbps
//        return when {
//            downloadSpeedMbps >= 50 -> 10
//            downloadSpeedMbps >= 40 -> 9
//            downloadSpeedMbps >= 30 -> 8
//            downloadSpeedMbps >= 20 -> 7
//            downloadSpeedMbps >= 10 -> 6
//            downloadSpeedMbps >= 5 -> 5
//            downloadSpeedMbps >= 3 -> 4
//            downloadSpeedMbps >= 2 -> 3
//            downloadSpeedMbps >= 1 -> 2
//            else -> 1
//        }
        return downloadSpeedMbps

    }

    private fun getNetworkCapabilities(): NetworkCapabilities?{
        val network = connectivityManager.activeNetwork
        return connectivityManager.getNetworkCapabilities(network)
    }
    companion object {
        @Volatile
        private var instance: DeviceInfoHelper? = null

        fun getInstance(context: Context): DeviceInfoHelper {
            return instance ?: synchronized(this) {
                instance ?: DeviceInfoHelper(context).also { instance = it }
            }
        }
    }
}
