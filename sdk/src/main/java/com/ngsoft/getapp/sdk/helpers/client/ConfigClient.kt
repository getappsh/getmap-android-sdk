package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.MapConfigDto
import android.util.Log
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getappclient.GetAppClient
import java.time.OffsetDateTime

internal object ConfigClient {
    private const val _tag = "ConfigClientHelper"

    fun fetchUpdates(client: GetAppClient, config: GetMapService.GeneralConfig, deviceId: String){
        Log.i(_tag, "getUpdates")

        val configRes = client.getMapApi.getMapControllerGetMapConfig(deviceId)
        Log.v(_tag, "fetchUpdates - configDto: $configRes")
        updateConfigFromDto(config, configRes)
    }

    private fun updateConfigFromDto(config: GetMapService.GeneralConfig, configDto: MapConfigDto){
        Log.d(_tag, "fetchUpdates -  applyServerConfig: ${config.applyServerConfig}")

        config.lastServerConfigUpdate = configDto.lastConfigUpdateDate ?: config.lastServerConfigUpdate
//        TODO
        configDto.lastCheckingMapUpdatesDate

        config.lastConfigCheck = OffsetDateTime.now()
        config.matomoSiteId = configDto.matomoSiteId ?: config.matomoSiteId
        config.matomoDimensionId = configDto.matomoDimensionId ?: config.matomoDimensionId


        if (!config.applyServerConfig)
            return

        config.deliveryTimeoutMins = configDto.deliveryTimeoutMins?.toInt() ?: config.deliveryTimeoutMins
        config.maxMapSizeInMB = configDto.maxMapSizeInMB?.toLong() ?: config.maxMapSizeInMB
        config.maxMapAreaSqKm = configDto.maxMapAreaSqKm?.toLong() ?: config.maxMapAreaSqKm
        config.maxParallelDownloads = configDto.maxParallelDownloads?.toInt() ?: config.maxParallelDownloads
        config.downloadRetry = configDto.downloadRetryTime?.toInt() ?: config.downloadRetry
        config.downloadTimeoutMins = configDto.downloadTimeoutMins?.toInt() ?: config.downloadTimeoutMins
        config.periodicInventoryIntervalMins = configDto.periodicInventoryIntervalMins?.toInt() ?: config.periodicInventoryIntervalMins
        config.periodicConfIntervalMins = configDto.periodicConfIntervalMins?.toInt() ?: config.periodicConfIntervalMins
        config.minAvailableSpaceMB = configDto.minAvailableSpaceMB?.toLong() ?: config.minAvailableSpaceMB
        config.matomoUrl = configDto.matomoUrl ?: config.matomoUrl
        config.mapMinInclusionPct = configDto.mapMinInclusionInPercentages?.toInt() ?: config.mapMinInclusionPct

        Log.v(_tag, "updateConfigFromDto - config: $config")
    }

}