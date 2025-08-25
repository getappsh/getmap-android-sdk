package com.ngsoft.getapp.sdk

import GetApp.Client.models.MapConfigDto
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


    override var baseUrl: String = pref.baseUrl
        set(value) {
            field = value
            pref.baseUrl = value
        }

    override var sdStoragePath: String = pref.sdStoragePath
        set(value) {
            if (field != value){
                field = value
                pref.sdStoragePath = value
            }
        }


    override var flashStoragePath: String = pref.flashStoragePath
        set(value) {
            if (field != value){
                field = value
                pref.flashStoragePath = value
            }
        }

    override var targetStoragePolicy: MapConfigDto.TargetStoragePolicy = pref.targetStoragePolicy
        set(value){
            if (field != value){
                field = value
                pref.targetStoragePolicy = targetStoragePolicy
            }
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
    override var maxMapAreaSqKm: Long = pref.maxMapAreaSqKm
        set(value) {
            field = value
            pref.maxMapAreaSqKm = value
        }
    override var flashInventoryMaxSizeMB: Long = pref.flashInventoryMaxSizeMB
        set(value){
            field = value
            pref.flashInventoryMaxSizeMB = value
        }
    override var sdInventoryMaxSizeMB: Long = pref.sdInventoryMaxSizeMB
        set(value){
            field = value
            pref.sdInventoryMaxSizeMB = value
        }

    override var maxParallelDownloads: Int = pref.maxParallelDownloads
        set(value) {
            field = value
            pref.maxParallelDownloads = value
        }

    override var periodicInventoryIntervalMins: Int = pref.periodicInventoryIntervalJob
        set(value) {
            field = if (value < 15) 15 else value
            pref.periodicInventoryIntervalJob = field
            JobScheduler().updateInventoryOfferingJob(appContext, field)
        }

    override var periodicConfIntervalMins: Int = pref.periodicConfIntervalJob
        set(value) {
            field = if (value < 15) 15 else value
            pref.periodicConfIntervalJob = value
            JobScheduler().updateRemoteConfigJob(appContext, value)
        }

    override var applyServerConfig: Boolean = pref.applyServerConfig
        set(value) {
            field = value
            pref.applyServerConfig = value
        }

    override var matomoUrl: String = pref.matomoUrl
        set(value) {
            field = value
            pref.matomoUrl = value
        }
    override var matomoDimensionId: String = pref.matomoDimensionId
        set(value) {
            field  = value
            pref.matomoDimensionId = value
        }

    override var matomoSiteId: String = pref.matomoSiteId
        set(value) {
            field  = value
            pref.matomoSiteId = value
        }
    override var matomoUpdateIntervalMins: Int = pref.matomoUpdateIntervalMins
        set(value) {
            field = value
            pref.matomoUpdateIntervalMins = value
        }

    override var minAvailableSpaceMB: Long = pref.minAvailableSpaceMB
        set(value) {
            field = value
            pref.minAvailableSpaceMB = value
        }
    override var mapMinInclusionPct: Int = pref.mapMinInclusionPct
        set(value){
            field = value
            pref.mapMinInclusionPct = value
        }
    override var lastConfigCheck: OffsetDateTime? = pref.lastConfigCheck
        set(value) {
            field = value
            pref.lastConfigCheck = value
        }
    override var lastInventoryCheck: OffsetDateTime? = pref.lastInventoryCheck
        set(value) {
            field = value
            pref.lastInventoryCheck = value
        }
    override var lastServerConfigUpdate: OffsetDateTime? = pref.lastServerConfigUpdate
        set(value) {
            field = value
            pref.lastServerConfigUpdate = value
        }

    override var lastServerInventoryJob: OffsetDateTime? = pref.lastServerInventoryJob
        set(value) {
            field = value
            pref.lastServerInventoryJob = value
        }

    override var ortophotoMapPath: String = pref.ortophotoMapPath
        set(value) {
            field = value
            pref.ortophotoMapPath = value
        }

    override var controlMapPath: String = pref.controlMapPath
        set(value){
            field = value
            pref.controlMapPath = value
        }

    override var ortophotoMapPattern: String = pref.ortophotoMapPattern
        set(value) {
            field = value
            pref.ortophotoMapPattern = value
        }

    override var controlMapPattern: String = pref.controlMapPattern
        set(value) {
            field = value
            pref.controlMapPattern = value
        }


    override fun toString(): String {
        return "ServiceConfig(sdStoragePath='$sdStoragePath', flashStoragePath='$flashStoragePath', targetStoragePolicy=$targetStoragePolicy, downloadPath='$downloadPath', deliveryTimeoutMins=$deliveryTimeoutMins, downloadTimeoutMins=$downloadTimeoutMins, downloadRetry=$downloadRetry, maxMapSizeInMB=$maxMapSizeInMB, maxMapAreaSqKm=$maxMapAreaSqKm, maxParallelDownloads=$maxParallelDownloads, periodicInventoryIntervalMins=$periodicInventoryIntervalMins, periodicConfIntervalMins=$periodicConfIntervalMins, applyServerConfig=$applyServerConfig, matomoUrl='$matomoUrl', matomoDimensionId='$matomoDimensionId', matomoSiteId='$matomoSiteId', matomoUpdateIntervalMins=$matomoUpdateIntervalMins, minAvailableSpaceMB=$minAvailableSpaceMB, mapMinInclusionPct=$mapMinInclusionPct, lastConfigCheck=$lastConfigCheck, flashInventoryMaxSizeMB=$flashInventoryMaxSizeMB, sdInventoryMaxSizeMB=$sdInventoryMaxSizeMB, ortophotoMapPath='$ortophotoMapPath', controlMapPath='$controlMapPath, lastInventoryCheck=$lastInventoryCheck, lastServerConfigUpdate=$lastServerConfigUpdate, lastServerInventoryJob=$lastServerInventoryJob')"
    }
}

