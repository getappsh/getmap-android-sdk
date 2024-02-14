package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.MapConfigDto
import timber.log.Timber
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getappclient.GetAppClient
import java.time.OffsetDateTime
import java.time.ZoneId

internal object ConfigClient {
    private const val _tag = "ConfigClientHelper"

    fun fetchUpdates(client: GetAppClient, config: GetMapService.GeneralConfig, deviceId: String){
        Timber.i("getUpdates")

        val configRes = client.getMapApi.getMapControllerGetMapConfig(deviceId)
        Timber.v("fetchUpdates - configDto: $configRes")
        updateConfigFromDto(config, configRes)
    }

    private fun updateConfigFromDto(config: GetMapService.GeneralConfig, configDto: MapConfigDto){
        Timber.d("fetchUpdates -  applyServerConfig: ${config.applyServerConfig}")

        val localZone = ZoneId.systemDefault()
        config.lastServerConfigUpdate = configDto.lastConfigUpdateDate
            ?.atZoneSameInstant(localZone)
            ?.toOffsetDateTime() ?: config.lastServerConfigUpdate

        config.lastServerInventoryJob = configDto.lastCheckingMapUpdatesDate
            ?.atZoneSameInstant(localZone)
            ?.toOffsetDateTime() ?: config.lastServerInventoryJob

        config.lastConfigCheck = OffsetDateTime.now()

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
        config.matomoSiteId = configDto.matomoSiteId ?: config.matomoSiteId
        config.matomoDimensionId = configDto.matomoDimensionId ?: config.matomoDimensionId
        config.matomoUpdateIntervalMins = configDto.periodicMatomoIntervalMins?.toInt() ?: config.matomoUpdateIntervalMins

        Timber.v("updateConfigFromDto - config: $config")
    }

}