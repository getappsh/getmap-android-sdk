package com.ngsoft.getapp.sdk.helpers

import GetApp.Client.models.MapConfigDto
import android.util.Log
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getappclient.GetAppClient
import java.time.OffsetDateTime

internal object ConfigClientHelper {
    private const val _tag = "ConfigClientHelper"

    fun fetchUpdates(config: GetMapService.GeneralConfig, client: GetAppClient, deviceId: String){
        Log.i(_tag, "getUpdates")
        if (!config.runConfJob){
            Log.d(_tag, "fetchUpdates -  runConfJob: ${config.runConfJob}, abort request")
            return
        }
        val configRes = client.getMapApi.getMapControllerGetMapConfig(deviceId)
        Log.v(_tag, "fetchUpdates - config: $configRes")
        updateConfigFromDto(config, configRes)
    }

    private fun updateConfigFromDto(config: GetMapService.GeneralConfig, configDto: MapConfigDto){
        config.deliveryTimeoutMins = configDto.deliveryTimeoutMins?.toInt() ?: config.deliveryTimeoutMins
        config.maxMapSizeInMB = configDto.maxMapSizeInMB?.toLong() ?: config.maxMapSizeInMB
        config.maxMapSizeInMerer = configDto.maxMapSizeInMeter?.toLong() ?: config.maxMapSizeInMerer
        config.maxParallelDownloads = configDto.maxParallelDownloads?.toInt() ?: config.maxParallelDownloads
        config.downloadRetry = configDto.downloadRetryTime?.toInt() ?: config.downloadRetry
        config.downloadTimeoutMins = configDto.downloadTimeoutMins?.toInt() ?: config.downloadTimeoutMins
        config.periodicInventoryIntervalMins = configDto.periodicInventoryIntervalMins?.toInt() ?: config.periodicInventoryIntervalMins
        config.periodicConfIntervalMins = configDto.periodicConfIntervalMins?.toInt() ?: config.periodicConfIntervalMins
        config.minAvailableSpaceBytes = configDto.minAvailableSpaceBytes?.toLong() ?: config.minAvailableSpaceBytes
        config.matomoUrl = configDto.matomoUrl ?: config.matomoUrl
        config.lastServerConfigUpdate = configDto.lastUpdate ?: config.lastServerConfigUpdate

        config.lastServerConfigUpdate = OffsetDateTime.now()
    }


}