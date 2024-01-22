package com.ngsoft.getapp.sdk

import android.content.Context
import com.ngsoft.getapp.sdk.jobs.JobScheduler
import java.time.OffsetDateTime

internal class ServiceConfig private constructor(private var appContext: Context): GetMapService.GeneralConfig{

    private var pref = Pref.getInstance(appContext)

    companion object {
        @Volatile
        private var instance: ServiceConfig? = null

        fun getInstance(appContext: Context): ServiceConfig {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = ServiceConfig(appContext)
                    }
                }
            }
            return instance!!
        }
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
    override var maxMapSizeInMeter: Long = pref.maxMapSizeInMeter
        set(value) {
            field = value
            pref.maxMapSizeInMeter = value
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

    override var matomoUrl: String = pref.matomoUrl
        set(value) {
            field = value
            pref.matomoUrl = value
        }
    override var matomoUpdateIntervalMins: Int = pref.matomoUpdateIntervalMins
        set(value) {
            field = value
            pref.matomoUpdateIntervalMins = value
        }

    override var minAvailableSpaceBytes: Long = pref.minAvailableSpace
        set(value) {
            field = value
            pref.minAvailableSpace = value
        }
    override var mapMinInclusionPct: Int = pref.mapMinInclusionPct
        set(value){
            field = value
            pref.mapMinInclusionPct = value
        }
    override var lastConfigCheck: OffsetDateTime = pref.lastConfigCheck
        set(value) {
            field = value
            pref.lastConfigCheck = value
        }
    override var lastInventoryCheck: OffsetDateTime = pref.lastInventoryCheck
        set(value) {
            field = value
            pref.lastInventoryCheck = value
        }
    override var lastServerConfigUpdate: OffsetDateTime = pref.lastServerConfigUpdate
        set(value) {
            field = value
            pref.lastServerConfigUpdate = value
        }
}

