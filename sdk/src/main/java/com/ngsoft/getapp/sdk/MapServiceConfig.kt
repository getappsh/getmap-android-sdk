package com.ngsoft.getapp.sdk

import android.content.Context

internal class MapServiceConfig private constructor(appContext: Context): GetMapService.GeneralConfig{

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
        }

    override var periodicConfIntervalMins: Int = pref.periodicConfIntervalJob
        set(value) {
            field = value
            pref.periodicConfIntervalJob = value
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


}

