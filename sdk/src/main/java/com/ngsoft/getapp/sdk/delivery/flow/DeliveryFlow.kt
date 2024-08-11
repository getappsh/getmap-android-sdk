package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import timber.log.Timber

internal abstract class DeliveryFlow(dlvCtx: DeliveryContext) {

    protected val checksumAlgorithm = "sha256"

    protected var config = dlvCtx.config
    protected var mapRepo = dlvCtx.mapRepo
    protected var mapFileManager =  dlvCtx.mapFileManager
    protected var pref = dlvCtx.pref
    protected var client = dlvCtx.client
    protected val app = dlvCtx.app
    protected val fetch = Fetch.Impl.getDefaultInstance()

    abstract fun execute(id: String): Boolean

    protected fun sendDeliveryStatus(id: String, state: MapDeliveryState?=null, download: Download? = null) {

        val dbps = if (download?.downloadedBytesPerSecond?.toInt() != -1) download?.downloadedBytesPerSecond else null
        val eta = if (download?.etaInMilliSeconds?.toInt() != -1) download?.etaInMilliSeconds else null

        MapDeliveryClient.sendDeliveryStatus(client, mapRepo, id, pref.deviceId, state, downloaded=download?.downloaded, downloadedBytesPerSecond=dbps, etaInMilliSeconds=eta)
    }

    protected fun handleMapNotExistsOnServer(id: String){
        Timber.i("handleMapNotExistsOnServer")
        val mapPkg = this.mapRepo.getById(id) ?: return
        mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)
        this.mapRepo.update(id=id, flowState = DeliveryFlowState.START, state = MapDeliveryState.ERROR,
            statusMsg = app.getString(R.string.delivery_status_error), statusDescr = app.getString(
                R.string.delivery_status_description_failed_not_exists_on_server),
            downloadProgress = 0, mapDone = false,
            jsonDone = false, mapAttempt = 0, jsonAttempt = 0, connectionAttempt = 0, validationAttempt = 0)
        this.mapRepo.setMapUpdated(id, false)

        mapPkg.JDID?.let { fetch.delete(it.toInt()) }
        mapPkg.MDID?.let { fetch.delete(it.toInt()) }

    }
}