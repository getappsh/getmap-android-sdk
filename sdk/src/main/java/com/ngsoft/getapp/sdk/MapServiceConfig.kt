package com.ngsoft.getapp.sdk

import GetApp.Client.models.MapConfigDto
import android.content.Context
import com.ngsoft.getapp.sdk.jobs.JobScheduler

internal class MapServiceConfig private constructor(private var appContext: Context): GetMapService.GeneralConfig{

    private var pref = Pref.getInstance(appContext)

    companion object {
        @Volatile
        private var instance: MapServiceConfig? = null

        fun getInstance(appContext: Context): MapServiceConfig {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = MapServiceConfig(appContext)
                    }
                }
            }
            return instance!!
        }
    }


    override var matomoUrl: String = pref.matomoUrl
        set(value) {
            field = value
            pref.matomoUrl = value
        }
    override var storagePath: String = pref.storagePath
        set(value) {
            field = value
            pref.storagePath = value
        }

    override var downloadPath: String = pref.downloadPath
        set(value) {
            field = value
            pref.downloadPath = value
        }

    override var deliveryTimeoutMins: Int = pref.deliveryTimeout
        set(value) {
            field = value
            pref.deliveryTimeout = value
        }

    override var downloadTimeoutMins: Int = pref.downloadTimeout
        set(value) {
            field = value
            pref.downloadTimeout = value
        }

    override var downloadRetry: Int = pref.downloadRetry
        set(value) {
            field = value
            pref.downloadRetry = value
        }

    override var maxMapSizeInMB: Long = pref.maxMapSizeInMB
        set(value) {
            field = value
            pref.maxMapSizeInMB = value
        }

    override var maxParallelDownloads: Int = pref.maxParallelDownloads
        set(value) {
            field = value
            pref.maxParallelDownloads = value
        }

    override var periodicInventoryIntervalMins: Int = pref.periodicInventoryIntervalJob
        set(value) {
            field = value
            pref.periodicInventoryIntervalJob = value
            JobScheduler().updateInventoryOfferingJob(appContext, value)
        }

    override var periodicConfIntervalMins: Int = pref.periodicConfIntervalJob
        set(value) {
            field = value
            pref.periodicConfIntervalJob = value
            JobScheduler().updateRemoteConfigJob(appContext, value)
        }

    override var runConfJob: Boolean = pref.runConfJob
        set(value) {
            field = value
            pref.runConfJob = value
        }

    override var minAvailableSpaceBytes: Long = pref.minAvailableSpace
        set(value) {
            field = value
            pref.minAvailableSpace = value
        }


    internal fun updateFromConfigDto(newConfig: MapConfigDto){
        deliveryTimeoutMins = newConfig.deliveryTimeoutMins?.toInt() ?: deliveryTimeoutMins
//        TODO maxMapSizeInMeter
        maxMapSizeInMB = newConfig.maxMapSizeInMB?.toLong() ?: maxMapSizeInMB
        maxParallelDownloads = newConfig.maxParallelDownloads?.toInt() ?: maxParallelDownloads
        downloadRetry = newConfig.downloadRetryTime?.toInt() ?: downloadRetry
        downloadTimeoutMins = newConfig.downloadTimeoutMins?.toInt() ?: downloadTimeoutMins
        periodicInventoryIntervalMins = newConfig.periodicInventoryIntervalMins?.toInt() ?: periodicInventoryIntervalMins
        periodicConfIntervalMins = newConfig.periodicConfIntervalMins?.toInt() ?: periodicConfIntervalMins
        minAvailableSpaceBytes = newConfig.minAvailableSpaceBytes?.toLong() ?: minAvailableSpaceBytes
        matomoUrl = newConfig.matomoUrl ?: matomoUrl
    }
}

