package com.ngsoft.getapp.sdk.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtil {



    fun isInternetAvailable(context: Context): Boolean {
        val networkCapabilities = getNetworkCapabilities(context)
        return networkCapabilities != null &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun getBandwidthQuality(context: Context): Int? {
        val networkCapabilities = getNetworkCapabilities(context) ?: return null

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

    private fun getNetworkCapabilities(context: Context): NetworkCapabilities?{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        return connectivityManager.getNetworkCapabilities(network)
    }
}