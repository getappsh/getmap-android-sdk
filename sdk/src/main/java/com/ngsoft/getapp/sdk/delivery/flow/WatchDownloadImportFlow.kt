package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.downloader.FetchDownloader
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CountDownLatch

internal class WatchDownloadImportFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx), FetchObserver<Download> {


    private val fetch = Fetch.Impl.getDefaultInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var toContinue = false
    private val latch = CountDownLatch(1)

    private lateinit var id: String
    private var downloadMapId = -1
    private var downloadJsonId = -1

    override fun execute(id: String): Boolean {
        Timber.i("watchDownloadImport - id: $id")
        toContinue = false
        this.id = id
        val mapPkg = this.mapRepo.getById(this.id) ?: return false
        downloadMapId = mapPkg.MDID?.toInt() ?: downloadMapId
        downloadJsonId = mapPkg.JDID?.toInt() ?: downloadJsonId

        fetch.attachFetchObserversForDownload(downloadMapId, this)
        fetch.attachFetchObserversForDownload(downloadJsonId, this)

        latch.await()

        Timber.i("watchDownloadImport - toContinue: $toContinue")
        fetch.removeFetchObserversForDownload(downloadMapId, this)
        fetch.removeFetchObserversForDownload(downloadJsonId, this)
        return toContinue
    }

    override fun onChanged(data: Download, reason: Reason) {
        coroutineScope.launch {

            Timber.d("onChanged - id: $id - reason: ${reason.name} - status: ${data.status} - file: ${data.file}")
            Timber.d("onChanged - ${data.toString()}")
            val isJson = data.id == downloadJsonId

            when (reason) {
//            Reason.NOT_SPECIFIED -> TODO()
//            Reason.DOWNLOAD_ADDED -> TODO()
//            Reason.DOWNLOAD_QUEUED -> TODO()
//            Reason.DOWNLOAD_STARTED -> TODO()
//            Reason.DOWNLOAD_BLOCK_UPDATED -> TODO()

//            Reason.DOWNLOAD_REMOVED -> TODO()
//             Reason.DOWNLOAD_DELETED -> TODO()

                Reason.DOWNLOAD_WAITING_ON_NETWORK -> {
//                TODO Try to get the error message
                    mapRepo.update(
                        id,
                        state = MapDeliveryState.DOWNLOAD,
                        statusDescr = "Waiting on network"
                    )
                }

                Reason.DOWNLOAD_ERROR -> {
                    mapRepo.update(
                        id, state = MapDeliveryState.ERROR, statusMsg = app.getString(
                            R.string.delivery_status_failed
                        ), statusDescr = FetchDownloader.getErrorMessage(data.error)
                    )
                    fetch.cancel(downloadMapId).cancel(downloadJsonId)
                    latch.countDown()
                }

                Reason.DOWNLOAD_CANCELLED -> {
                    latch.countDown()
                }
                Reason.DOWNLOAD_PAUSED -> {
                    fetch.pause(downloadMapId).pause(downloadJsonId)
                    mapRepo.update(
                        id,
                        state = MapDeliveryState.CANCEL,
                        flowState = DeliveryFlowState.IMPORT_DELIVERY,
                        statusMsg = app.getString(
                            R.string.delivery_status_canceled
                        ),
                    )
                    latch.countDown()
                }

                Reason.REPORTING,
                Reason.OBSERVER_ATTACHED,
                Reason.DOWNLOAD_QUEUED,
                Reason.DOWNLOAD_PROGRESS_CHANGED,
                Reason.DOWNLOAD_COMPLETED,
                Reason.DOWNLOAD_RESUMED -> {
                    if (isJson) {
                        onJsonChange(data, reason)
                    } else {
                        onMapChange(data, reason)
                    }
                }

                else -> {
                }
            }
        }

    }

    private  fun onMapChange(download: Download, reason: Reason){
        var mapPkg = mapRepo.getById(id)

        when(download.status){
            Status.QUEUED -> {
                mapRepo.update(id, state = MapDeliveryState.DOWNLOAD, statusMsg = app.getString(R.string.delivery_status_queued))
            }

            Status.NONE,
            Status.ADDED,
            Status.DOWNLOADING -> {
                if (mapPkg?.metadata?.mapDone == false){
                    val progress = if (download.progress >= 0) download.progress else mapPkg.downloadProgress
                    mapRepo.update(id = id, downloadProgress = progress, fileName = FileUtils.getFileNameFromUri(download.file),
                        state = MapDeliveryState.DOWNLOAD, statusMsg = app.getString(R.string.delivery_status_download), statusDescr = "")
                    sendDeliveryStatus(id)
                }
            }
            Status.COMPLETED -> {
                mapPkg = mapRepo.updateAndReturn(id, mapDone = true, fileName = FileUtils.getFileNameFromUri(download.file))
                Timber.i("downloadFile - id: $id, Map Done: ${mapPkg?.metadata?.mapDone}, Json Done: ${mapPkg?.metadata?.jsonDone}, state: ${mapPkg?.state} ")
                if (mapPkg?.state == MapDeliveryState.ERROR){
                    latch.countDown()
                    return
                }

                if (mapPkg?.metadata?.jsonDone == true){
                    handleDownloadDone(id)
                }
//                TODO old version makes validation if json is done, check if needed too.
            }
//            Status.PAUSED -> TODO()
//            Status.CANCELLED -> TODO()
//            Status.FAILED -> TODO()
//            Status.REMOVED -> TODO()
//            Status.DELETED -> TODO()
            else -> {}
        }

    }
    private fun onJsonChange(download: Download, reason: Reason){
        if (download.status == Status.COMPLETED){

            Timber.d("handleJsonDone - for id: $id")
            val mapPkg = extractFootprint(download)?.let {
                mapRepo.setFootprint(id, it)
                mapRepo.updateAndReturn(id, jsonDone = true, jsonName = FileUtils.getFileNameFromUri(download.file))
            }


            Timber.i("downloadFile - id: $id, Map Done: ${mapPkg?.metadata?.mapDone}, Json Done: ${mapPkg?.metadata?.jsonDone}, state: ${mapPkg?.state} ")
            if (mapPkg?.state == MapDeliveryState.ERROR){
                latch.countDown()
                return
            }

            if (mapPkg?.metadata?.mapDone == true){
                handleDownloadDone(id)
            }
        }

    }

    private fun handleDownloadDone(id: String){
        Timber.d("handleDownloadDone - downloading Done")
        mapRepo.update(id = id, flowState = DeliveryFlowState.DOWNLOAD_DONE, downloadProgress = 100, statusDescr = "")
        sendDeliveryStatus(id)
        toContinue = true
        latch.countDown()
    }

    private fun extractFootprint(download: Download): String?{
        return try {
            val mapPkg = mapRepo.getById(id) ?: return null

            val json = JsonUtils.readJson(download.file)
            val footprint = FootprintUtils.toString(json.getJSONObject("footprint"))

            val res = this.mapRepo
                .getByBBox(mapPkg.bBox, footprint)
                .filter {it.id.toString() != id && it.isUpdated }

            if (res.isNotEmpty()){
                Timber.e("handleJsonDone - map already exists, set to error", )
                this.mapRepo.update(id, state = MapDeliveryState.ERROR,
                    statusMsg = app.getString(R.string.error_map_already_exists),
                    jsonName = "fail.json", fileName ="fail.gpkg")

//                    TODO make sure!
                fetch.delete(downloadMapId).delete(downloadJsonId)
                return null
            }
            return footprint

        }catch (e: Exception){
            Timber.e("Failed to get footprint from json, error: ${e.message.toString()}")
//            TODO why do not set the state to error?
            null
        }
    }

}