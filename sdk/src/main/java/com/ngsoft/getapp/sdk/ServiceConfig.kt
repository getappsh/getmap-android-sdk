package com.ngsoft.getapp.sdk

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.ngsoft.getapp.sdk.jobs.JobScheduler
import timber.log.Timber
import java.io.File
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
    override var relativeStoragePath: String = pref.relativeStoragePath
        set(value) {
            if (field != value){
                field = value
                pref.relativeStoragePath = value
                updateStoragePath()
            }
        }

    override var useSDCard: Boolean = pref.useSDCard
        set(value){
            if (field != value){
                field = value
                pref.useSDCard = useSDCard
                updateStoragePath()
            }
        }

    private fun updateStoragePath(){
        val storageManager: StorageManager = appContext.getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes;

        val base = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            Environment.getExternalStorageDirectory()
        } else if (this.useSDCard && storageList.size > 1){
            storageList[1].directory?.absoluteFile
        }else {
            storageList[0].directory?.absoluteFile
        }
        Environment.getExternalStorageDirectory()
        try {
            val storageDir = File(base, this.relativeStoragePath)
            storageDir.mkdirs()
            storagePath  = storageDir.absolutePath
        }catch (e: Exception){
            Timber.e("Error update storage path. useSD: $useSDCard, relativePath: $relativeStoragePath, error: ${e.message}")
            Toast.makeText(appContext, "Error update storage path, error: ${e.message}", Toast.LENGTH_LONG).show()
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

    override fun toString(): String {
        return "ServiceConfig(storagePath='$storagePath', downloadPath='$downloadPath', deliveryTimeoutMins=$deliveryTimeoutMins, downloadTimeoutMins=$downloadTimeoutMins, downloadRetry=$downloadRetry, maxMapSizeInMB=$maxMapSizeInMB, maxMapAreaSqKm=$maxMapAreaSqKm, maxParallelDownloads=$maxParallelDownloads, periodicInventoryIntervalMins=$periodicInventoryIntervalMins, periodicConfIntervalMins=$periodicConfIntervalMins, applyServerConfig=$applyServerConfig, matomoUrl='$matomoUrl', matomoDimensionId='$matomoDimensionId', matomoSiteId='$matomoSiteId', matomoUpdateIntervalMins=$matomoUpdateIntervalMins, minAvailableSpaceMB=$minAvailableSpaceMB, mapMinInclusionPct=$mapMinInclusionPct, lastConfigCheck=$lastConfigCheck, lastInventoryCheck=$lastInventoryCheck, lastServerConfigUpdate=$lastServerConfigUpdate, lastServerInventoryJob=$lastServerInventoryJob)"
    }
}

