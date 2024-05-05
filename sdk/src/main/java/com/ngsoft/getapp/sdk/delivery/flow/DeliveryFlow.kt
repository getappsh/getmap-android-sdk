package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber

internal abstract class DeliveryFlow(dlvCtx: DeliveryContext) {

    protected val checksumAlgorithm = "sha256"

    protected var config = dlvCtx.config
    protected var mapRepo = dlvCtx.mapRepo
    protected var downloader = dlvCtx.downloader
    protected var mapFileManager =  dlvCtx.mapFileManager
    protected var pref = dlvCtx.pref
    protected var client = dlvCtx.client
    protected val app = dlvCtx.app

    abstract fun execute(id: String): Boolean

    protected fun sendDeliveryStatus(id: String, state: MapDeliveryState?=null) {
        MapDeliveryClient.sendDeliveryStatus(client, mapRepo, id, pref.deviceId, state)
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
        mapPkg.JDID?.let { downloader.cancelDownload(it) }
        mapPkg.MDID?.let { downloader.cancelDownload(it) }

    }
// TODO dose not need to be here
    protected fun downloadFile(url: String): Long {
        Timber.i("downloadFile")
        val fileName = try {
            val storagePath = mapFileManager.getAndValidateStorageDirByPolicy((config.minAvailableSpaceMB * 1024 * 1024)).path
            // TODO note suer why its have been done
            FileUtils.getUniqueFileName(storagePath, FileUtils.getFileNameFromUri(url))
        }catch (e: Exception){
            FileUtils.getFileNameFromUri(url)
        }

        val downloadId = downloader.downloadFile(url, fileName){
            Timber.d("downloadImport - completionHandler: processing download ID=$it completion event...")
        }

        return downloadId
    }
}