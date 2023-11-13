package com.ngsoft.getapp.sdk

import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.TilesCache
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioSdkGetMapService"

    private var deliveryTimeoutMinutes: Int = 5
    private var downloadTimeoutMinutes: Int = 5

    private lateinit var mapRepo: MapRepo

    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        deliveryTimeoutMinutes = configuration.deliveryTimeout
        downloadTimeoutMinutes = configuration.downloadTimeout

        mapRepo = MapRepo()
        return true
    }

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        val id = this.mapRepo.create(
            mp.productId, mp.boundingBox, MapDeliveryState.START,
            appCtx.getString(R.string.delivery_status_req_sent), downloadStatusHandler)

        Log.i(_tag, "downloadMap: id: $id")

        Thread{
            this.mapRepo.update(id, state = MapDeliveryState.START, statusMessage = appCtx.getString(R.string.delivery_status_req_sent))
            this.sendDeliveryStatus(id)
            try {
                var res = importCreate(id, mp)
                if (!res) {
                    sendDeliveryStatus(id)
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

            val pkgUrl = this.mapRepo.getUrl(id)!!
            val jsonUrl = PackageDownloader.changeFileExtensionToJson(pkgUrl)
            var pkgCompleted = false
            var jsonCompleted = false
            val jsonDownloadId: Long?
            val pkgDownloadId: Long?

            val completionHandler: (Long) -> Unit = {
                Log.d(_tag, "processing download ID=$it completion event...")
            }

            try {
                jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
                pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)

                Log.d(_tag, "downloadMap: jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")

                this.mapRepo.update(
                    id = id,
                    JDID = jsonDownloadId,
                    MDID = pkgDownloadId,
                    state =  MapDeliveryState.DOWNLOAD,
                    statusMessage = appCtx.getString(R.string.delivery_status_download),
                    jsonName = PackageDownloader.getFileNameFromUri(jsonUrl),
                    fileName = PackageDownloader.getFileNameFromUri(pkgUrl)
                )
                this.sendDeliveryStatus(id)

            } catch (e: Exception) {
                Log.e(_tag, "downloadMap - downloadFile: ${e.message.toString()} ")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = e.message.toString()
                )
                this.sendDeliveryStatus(id)
                return@Thread
            }

            timer(initialDelay = 100, period = 500) {
                if (mapRepo.isDownloadCanceled(id)){
                    Log.d(_tag, "downloadMap: Download $id, canceled by user")
                    downloader.cancelDownload(jsonDownloadId, pkgDownloadId)
                    mapRepo.update(id, state = MapDeliveryState.CANCEL)
                    sendDeliveryStatus(id)
                    this.cancel()
                }
                val jsonProgress = downloader.queryProgress(jsonDownloadId)
                val fileProgress = downloader.queryProgress(pkgDownloadId)

                if (jsonProgress.first == -1L || fileProgress.first == -1L) {
                    Log.i(_tag, "downloadMap: DownloadManager failed to download file")
                    mapRepo.update(
                        id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed),
                        errorContent = "downloadMap: DownloadManager failed to download file"
                    )
                    sendDeliveryStatus(id)
                    this.cancel()
                }
                if (!jsonCompleted && jsonProgress.second > 0) {
                    val progress = (jsonProgress.first * 100 / jsonProgress.second).toInt()
                    Log.d(_tag, "jsonDownloadId: $jsonDownloadId -> process: $progress ")
                    if (progress >= 100) {
                        jsonCompleted = true;
                    }
                }
                if (!pkgCompleted && fileProgress.second > 0) {
                    val progress = (fileProgress.first * 100 / fileProgress.second).toInt()
                    Log.d(_tag, "pkgDownloadId: $pkgDownloadId -> process: $progress ")

                    if (progress >= 100) {
                        pkgCompleted = true
                    }
                    mapRepo.update(
                        id = id,
                        state = MapDeliveryState.DOWNLOAD,
                        statusMessage =  appCtx.getString(R.string.delivery_status_download),
                        downloadProgress = progress
                    )
                    sendDeliveryStatus(id)
                }

                if (pkgCompleted && jsonCompleted) {
                    Log.d(_tag, "downloading Done: ")
                    Log.d(_tag, "stopping progress watcher...")
                    mapRepo.update(
                        id = id,
                        state = MapDeliveryState.DONE,
                        statusMessage = appCtx.getString(R.string.delivery_status_done),
                        downloadProgress = 100,
                    )
                    sendDeliveryStatus(id)
                    this.cancel()
                }
            }
        }.start()
        return id
    }
    private fun importCreate(id: String, mp: MapProperties,): Boolean{
        Log.d(_tag, "importCreate")

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "importCreate: Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL)
            return false
        }
        val retCreate = createMapImport(mp)

        when(retCreate?.state){
            MapImportState.START, MapImportState.DONE ->{
                Log.d(_tag,"deliverTile - createMapImport => OK: ${retCreate.state} ")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.START,
                    statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress)
                )
                return true
            }
            MapImportState.CANCEL -> {
                Log.w(_tag,"getDownloadData - createMapImport => CANCEL")
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
        Log.d(_tag, "checkImportStatue")

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
        return true
    }

    private fun importDelivery(id: String): Boolean{
        Log.d(_tag, "importDelivery")

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
        Log.d(_tag, "checkDeliveryStatus")
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

        this.mapRepo.update(id = id, url = deliveryStatus.url)
        return true
    }

    private fun sendDeliveryStatus(id: String) {
//        TODO check why send delivery status done twice
        Thread{
            val dlv = this.mapRepo.getDeliveryStatus(id, pref.deviceId) ?: return@Thread
            Log.d(_tag, "sendDeliveryStatus: id: $id status: $dlv.deliveryStatus, catalog id: ${dlv.catalogId}")
            try{
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            }catch (exc: Exception){
                Log.e(_tag, "sendDeliveryStatus failed error: $exc", )
            }
        }.start()
    }
    override fun cancelDownload(id: String) {
        Log.d(_tag, "cancelDownload: for id: $id")
        this.mapRepo.setCancelDownload(id)
    }
}