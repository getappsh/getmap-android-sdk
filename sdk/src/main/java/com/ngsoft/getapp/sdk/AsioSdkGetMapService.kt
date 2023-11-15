package com.ngsoft.getapp.sdk

import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.PrepareDeliveryResDto
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioSdkGetMapService"

    private var deliveryTimeoutMinutes: Int = 5
    private var downloadTimeoutMinutes: Int = 5
    private var downloadRetryAttempts: Int = 2

    private lateinit var storagePath: String

    private lateinit var mapRepo: MapRepo

    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        deliveryTimeoutMinutes = configuration.deliveryTimeout
        downloadTimeoutMinutes = configuration.downloadTimeout
        downloadRetryAttempts = configuration.downloadRetry

        storagePath = configuration.storagePath

        mapRepo = MapRepo()
        return true
    }

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        val id = this.mapRepo.create(
            mp.productId, mp.boundingBox, MapDeliveryState.START,
            appCtx.getString(R.string.delivery_status_req_sent), DeliveryFlowState.START, downloadStatusHandler)

        Log.i(_tag, "downloadMap: id: $id")
        Log.d(_tag, "downloadMap: bBox - ${mp.boundingBox}")


        Thread{
            this.mapRepo.update(id, state = MapDeliveryState.START, statusMessage = appCtx.getString(R.string.delivery_status_req_sent))
            try {
                var res = importCreate(id, mp)
                sendDeliveryStatus(id)
                if (!res) {
                    return@Thread
                }
                res = checkImportStatue(id)
                if (!res) {
                    sendDeliveryStatus(id)
                    return@Thread
                }
                res = importDelivery(id)
                if (!res) {
                    sendDeliveryStatus(id)
                    return@Thread
                }
                res = checkDeliveryStatus(id)
                if (!res) {
                    sendDeliveryStatus(id)
                    return@Thread
                }
                res = downloadImport(id)
                if (!res) {
                    sendDeliveryStatus(id)
                    return@Thread
                }

            } catch (e: Exception) {
                Log.e(_tag, "downloadMap: exception:  ${e.message.toString()}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = e.message.toString()
                )
                this.sendDeliveryStatus(id)
            }

        }.start()
        return id
    }

    private fun importCreate(id: String, mp: MapProperties,): Boolean{
        Log.i(_tag, "importCreate")

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "importCreate: Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL)
            return false
        }
        val retCreate = createMapImport(mp)
        Log.d(_tag, "importCreate - import request Id: ${retCreate?.importRequestId}")
        when(retCreate?.state){
            MapImportState.START, MapImportState.DONE ->{
                Log.d(_tag,"deliverTile - createMapImport: OK: ${retCreate.state} ")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.START,
                    flowState = DeliveryFlowState.IMPORT_CREATE,
                    statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress)
                )
                return true
            }
            MapImportState.CANCEL -> {
                Log.w(_tag,"getDownloadData - createMapImport: CANCEL")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.CANCEL,
                    statusMessage = appCtx.getString(R.string.delivery_status_canceled)
                )
                return false
            }
            else -> {
                Log.e(_tag,"getDownloadData - createMapImport failed: ${retCreate?.state}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate?.importRequestId,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = retCreate?.importRequestId
                )
                return false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatue(id: String): Boolean {
        Log.i(_tag, "checkImportStatue")

        val reqId = this.mapRepo.getReqId(id)!!;

        var stat = getCreateMapImportStatus(reqId)

        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state!! != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(2)

            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "checkImportStatue: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL)
                return false
            }

            stat = getCreateMapImportStatus(reqId)

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(_tag,"checkImportStatus - MapImportState.ERROR")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed)
                    )
//                    todo error message
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState.CANCEL")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.CANCEL,
                        statusMessage = appCtx.getString(R.string.delivery_status_canceled)
                    )
                    return false

                }
                else -> {}
            }

            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkImportStatus - timed out")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = "checkImportStatus - timed out"
                )
                return false

            }
        }

        Log.d(_tag, "checkImportStatue: MapImportState.Done")
        this.mapRepo.updateFlowState(id, DeliveryFlowState.IMPORT_STATUS)
        return true
    }

    private fun importDelivery(id: String): Boolean{
        Log.i(_tag, "importDelivery")

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "importDelivery: Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL)
            return false
        }
        val reqId = this.mapRepo.getReqId(id)!!;

        val retDelivery = setMapImportDeliveryStart(reqId)

        when(retDelivery?.state){
            MapDeliveryState.DONE,
            MapDeliveryState.START,
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE ->  {
                Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
                this.mapRepo.updateFlowState(id, DeliveryFlowState.IMPORT_DELIVERY)
                return true
            }
            MapDeliveryState.CANCEL,  MapDeliveryState.PAUSE -> {
                Log.w(_tag,"getDownloadData - setMapImportDeliveryStart => CANCEL")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.CANCEL,
                    statusMessage = appCtx.getString(R.string.delivery_status_canceled))

                return false
            }
            else -> {
                Log.e(_tag,"getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = "getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}"
                )
                return false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun checkDeliveryStatus(id: String): Boolean {
        Log.i(_tag, "checkDeliveryStatus")
        val reqId = this.mapRepo.getReqId(id)!!;

        var deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(reqId)

        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (deliveryStatus.status != PrepareDeliveryResDto.Status.done){
            TimeUnit.SECONDS.sleep(2)

            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "checkDeliveryStatus: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL)
                return false
            }
            deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(reqId)

            when(deliveryStatus.status){
                PrepareDeliveryResDto.Status.error -> {
                    Log.e(_tag,"getDownloadData - prepared delivery status: Error")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed),
                        errorContent = "getDownloadData - prepared delivery status: Error"
                    )
                    return false
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkDeliveryStatus - timed out")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = "checkDeliveryStatus - timed out"
                )
                return false
            }
        }

        if (deliveryStatus.url == null){
            Log.e(_tag, "getDownloadData - download url is null", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                errorContent = "getDownloadData - download url is null"
            )
            return false

        }

        this.mapRepo.update(id = id, url = deliveryStatus.url, flowState = DeliveryFlowState.IMPORT_DELIVERY_STATUS)
        return true
    }

    private fun downloadImport(id: String): Boolean{
        Log.i(_tag, "downloadImport")

        val pkgUrl = this.mapRepo.getUrl(id)!!
        val jsonUrl = PackageDownloader.changeFileExtensionToJson(pkgUrl)
        var pkgCompleted = false
        var jsonCompleted = false

        val completionHandler: (Long) -> Unit = {
            Log.d(_tag, "downloadImport - completionHandler: processing download ID=$it completion event...")
        }

        var jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
        var pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)

        Log.d(_tag, "downloadImport - jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")

        this.mapRepo.update(
            id = id,
            JDID = jsonDownloadId,
            MDID = pkgDownloadId,
            state =  MapDeliveryState.DOWNLOAD,
            flowState = DeliveryFlowState.DOWNLOAD,
            statusMessage = appCtx.getString(R.string.delivery_status_download),
            jsonName = PackageDownloader.getFileNameFromUri(jsonUrl),
            fileName = PackageDownloader.getFileNameFromUri(pkgUrl)
        )
        this.sendDeliveryStatus(id)

        var res = true
        var pkgReqRetry = 1
        var jsonReqRetry = 1

        timer(initialDelay = 100, period = 500) {
            if (mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "downloadImport - Download $id, canceled by user")
                downloader.cancelDownload(jsonDownloadId, pkgDownloadId)
                mapRepo.update(id, state = MapDeliveryState.CANCEL)
                res = false
                this.cancel()
            }

            val pkgStatus = downloader.queryStatus(pkgDownloadId)
            val jsonStatus = downloader.queryStatus(jsonDownloadId)


            when(pkgStatus?.status){
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_SUCCESSFUL -> {
                    val progress = (pkgStatus.downloadBytes * 100 / pkgStatus.totalBytes).toInt()
                    Log.d(_tag, "downloadImport - pkgDownloadId: $pkgDownloadId -> process: $progress ")
                    if (!pkgCompleted){
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.DOWNLOAD,
                            statusMessage =  appCtx.getString(R.string.delivery_status_download),
                            downloadProgress = progress
                        )
                        sendDeliveryStatus(id)
                    }

                    if (progress >= 100 || pkgStatus.status == DownloadManager.STATUS_SUCCESSFUL){
                        pkgCompleted = true
                    }

                }
                else -> {
                    Log.e(_tag, "downloadImport -  DownloadManager failed to download pkg, reason: ${pkgStatus?.reason}")
                    if (pkgReqRetry < downloadRetryAttempts){
                        Log.d(_tag, "downloadImport - retry download")
                        pkgReqRetry ++
                        pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)
                        mapRepo.update(
                            id = id,
                            MDID = pkgDownloadId,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed_try_again),
                            errorContent = pkgStatus?.reason ?: "downloadImport - DownloadManager failed to download file"
                        )

                    }else{
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.ERROR,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed),
                            errorContent = pkgStatus?.reason ?: "downloadImport - DownloadManager failed to download file"
                        )
                        res = false
                        this.cancel()
                    }

                }
            }
            when(jsonStatus?.status){
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {}
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d(_tag, "downloadImport - download json Done!")
                    jsonCompleted = true
                }
                else -> {
                    Log.e(_tag, "downloadImport -  DownloadManager failed to download json, reason: ${jsonStatus?.reason}")
                    if (jsonReqRetry < downloadRetryAttempts){
                        Log.d(_tag, "downloadImport - retry download")
                        jsonReqRetry ++
                        jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
                        mapRepo.update(
                            id = id,
                            JDID = jsonDownloadId,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed_try_again),
                            errorContent = pkgStatus?.reason ?: "downloadImport - DownloadManager failed to download json"
                        )
                    }else{
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.ERROR,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed),
                            errorContent = pkgStatus?.reason ?: "downloadImport - DownloadManager failed to download file"
                        )
                        res = false
                        this.cancel()
                    }
                }
            }

            if (pkgCompleted && jsonCompleted) {
                Log.d(_tag, "downloadImport - downloading Done")
                Log.d(_tag, "downloadImport - stopping progress watcher...")
                mapRepo.update(
                    id = id,
                    state = MapDeliveryState.DONE,
                    flowState = DeliveryFlowState.DONE,
                    statusMessage = appCtx.getString(R.string.delivery_status_done),
                    downloadProgress = 100,
                )
                sendDeliveryStatus(id)
                res = true
                this.cancel()
            }
        }

        return res

    }

    private fun sendDeliveryStatus(id: String) {
//        TODO check why send delivery status done twice
        val dlv = this.mapRepo.getDeliveryStatus(id, pref.deviceId) ?: return

        Thread{
            Log.d(_tag, "sendDeliveryStatus - id: $id status: $dlv")
            try{
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            }catch (exc: Exception){
                Log.e(_tag, "sendDeliveryStatus failed error: ${exc.message.toString()}", )
                exc.printStackTrace()
            }
        }.start()
    }
    override fun cancelDownload(id: String) {
        Log.d(_tag, "cancelDownload: for id: $id")
        this.mapRepo.setCancelDownload(id)
    }

    override fun deleteMap(id: String){
        Log.d(_tag, "deleteMap - id: $id")
        val downloadData = this.mapRepo.getDownloadData(id)

        if (downloadData == null ||
            downloadData.deliveryStatus == MapDeliveryState.START ||
            downloadData.deliveryStatus == MapDeliveryState.DOWNLOAD ||
            downloadData.deliveryStatus == MapDeliveryState.CONTINUE){

            val errorMsg = "deleteMap: Unable to delete map when status is: ${downloadData?.deliveryStatus}"
            Log.e(_tag,  errorMsg)
            throw Exception(errorMsg)
        }

        val map = downloadData.fileName
        val json = downloadData.jsonName

        if (map != null){
            this.deleteFile(map)
        }
        if (json != null){
            this.deleteFile(json)
        }

        val delivery = this.mapRepo.getDeliveryStatus(id, pref.deviceId)

        this.mapRepo.remove(id)

//        Send delivery status to server
        Thread{
            var dlv = delivery
            if (dlv != null){
                dlv = dlv.copy(deliveryStatus = DeliveryStatusDto.DeliveryStatus.deleted)
                try{
                    Log.d(_tag, "sendDeliveryStatus: id: $id status: $dlv.deliveryStatus, catalog id: ${dlv.catalogId}")
                    client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
                }catch (exc: Exception){
                    Log.e(_tag, "sendDeliveryStatus failed error: $exc", )
                }
            }
        }.start()
    }
    private fun deleteFile(fileName: String){
        val dirPath = Environment.getExternalStoragePublicDirectory(storagePath)
        val file = File(dirPath, fileName)

        Log.d(_tag, "deleteFile - File path ${file.path}")

        if (file.exists()){
            if (file.delete() ) {
                Log.d(_tag, "deleteFile - File deleted successfully. $fileName")
            } else {
                Log.d(_tag, "deleteFile -Failed to delete the file. $fileName")
            }
        }else{
            Log.d(_tag, "deleteFile - File dose not exist. $fileName")
        }
    }
}