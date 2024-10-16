package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.downloader.FetchDownloader.message
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.getapp.sdk.utils.NetworkUtil
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch

internal class WatchDownloadImportFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx), FetchObserver<Download> {


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
        if (!toContinue) {
            sendDeliveryStatus(id)
        }
        return toContinue
    }

    override fun onChanged(data: Download, reason: Reason) {
        coroutineScope.launch {

            Timber.i("onChanged - id: $id - reason: ${reason.name} - status: ${data.status} - file: ${data.file}")
            Timber.d("onChanged - ${data}")
            val isJson = data.id == downloadJsonId

            if (reason == Reason.DOWNLOAD_WAITING_ON_NETWORK){
//                TODO Try to get the error message
                    mapRepo.update(
                        id,
                        state = MapDeliveryState.DOWNLOAD,
                        statusDescr = "Waiting on network"
                    )
            }

            when (data.status) {
//                Status.REMOVED -> TODO()
                Status.DELETED,
                Status.CANCELLED -> {
                    latch.countDown()
                }
                Status.FAILED -> {
                    Timber.e("onChanged - failed, error code: ${data.error.httpResponse?.code}")
                    if(data.error.httpResponse?.code == 404 || data.error.httpResponse?.code == 403){
                        handleMapNotExistsOnServer(id)
                    }else{
                        mapRepo.update(
                            id, state = MapDeliveryState.ERROR, statusMsg = app.getString(
                                R.string.delivery_status_failed
                            ), statusDescr = data.error.message(), flowState = DeliveryFlowState.IMPORT_DELIVERY
                        )
                        fetch.cancel(downloadMapId).cancel(downloadJsonId)
                    }

                    latch.countDown()
                }
                Status.PAUSED -> {
                    fetch.pause(downloadMapId).pause(downloadJsonId)
                    mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMsg = app.getString(
                            R.string.delivery_status_canceled))
                    latch.countDown()
                }

                Status.ADDED,
                Status.NONE,
                Status.QUEUED,
                Status.DOWNLOADING,
                Status.COMPLETED -> {
                    if (isJson) {
                        onJsonChange(data, reason)
                    } else {
                        onMapChange(data, reason)
                    }
                }

                else -> {}
            }
        }

    }
    private fun checkUrlStatus(url: String): Int? {
        val responseCode: Int?
        try{
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()
            responseCode = connection.responseCode
            connection.disconnect()
        }catch (e: Exception){
            return null
        }
        return responseCode
    }

    private  fun onMapChange(download: Download, reason: Reason){
        var mapPkg = mapRepo.getById(id)

        when(download.status){
            Status.QUEUED -> {
                if(download.autoRetryAttempts > 1){
                    mapRepo.update(id, state = MapDeliveryState.DOWNLOAD, statusMsg = app.getString(R.string.delivery_status_queued_try_again))
                }else if(download.autoRetryAttempts == 1){
                    val statusCode = checkUrlStatus(download.url)
                    if (statusCode == 404 || statusCode == 403){
                        handleMapNotExistsOnServer(id)
                        latch.countDown()
                    }
                } else {
                    mapRepo.update(id, state = MapDeliveryState.DOWNLOAD, statusMsg = app.getString(R.string.delivery_status_queued))
                }

                if (!NetworkUtil.isInternetAvailable(app)){
                    mapRepo.update(id, statusMsg = app.getString(R.string.delivery_status_connection_issue_queued), statusDescr = app.getString(R.string.delivery_status_description_queued_no_internet_connection))
                }
            }

            Status.NONE,
            Status.ADDED,
            Status.DOWNLOADING -> {
                if (mapPkg?.metadata?.mapDone == false){
                    val progress = if (download.progress >= 0) download.progress else mapPkg.downloadProgress
                    mapRepo.update(id = id, downloadProgress = progress, fileName = FileUtils.getFileNameFromUri(download.file),
                        state = MapDeliveryState.DOWNLOAD, statusMsg = app.getString(R.string.delivery_status_download), statusDescr = "")
                    sendDeliveryStatus(id, download=download)
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
                    handleDownloadDone(id, download)
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
            addUrlToJson(download)


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

    private fun handleDownloadDone(id: String, download: Download? = null){
        Timber.d("handleDownloadDone - downloading Done")
        mapRepo.update(id = id, flowState = DeliveryFlowState.DOWNLOAD_DONE, downloadProgress = 100, statusDescr = "")
        sendDeliveryStatus(id, download=download)
        toContinue = true
        latch.countDown()
    }

    private fun addUrlToJson(download: Download){
        Timber.d("addUrlToJson - append download url to json")
        val mapPkg = mapRepo.getById(id) ?: return
        val json = JsonUtils.readJson(download.file)
        json.put("downloadUrl", mapPkg.url)
        json.put("requestedBBox", mapPkg.bBox)
        json.put("reqId", mapPkg.reqId)

        JsonUtils.writeJson(download.file, json)
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
                this.mapRepo.update(id, state = MapDeliveryState.ERROR, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                    statusMsg = app.getString(R.string.error_map_already_exists),
                    jsonName = "fail.json", fileName ="fail.gpkg")

//                    TODO make sure!
                fetch.delete(downloadMapId).delete(downloadJsonId)
                return null
            }
            return footprint

        }catch (e: Exception){
            Timber.e("Failed to get footprint from json, error: ${e.message.toString()}")
            null
        }
    }

}