package com.ngsoft.getapp.sdk

import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioSdkGetMapService"

    private var deliveryTimeoutMinutes: Int = 5
    private var downloadTimeoutMinutes: Int = 5




    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        deliveryTimeoutMinutes = configuration.deliveryTimeout
        downloadTimeoutMinutes = configuration.downloadTimeout
        return true
    }

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String?{
        Log.d(_tag, "downloadMap for -> $mp")
        var tmr: Timer? = null
        val downloadData: MapDownloadData

        try{
            downloadData = getDownloadData(mp, downloadStatusHandler)
        }catch (e: Exception){
            downloadStatusHandler.invoke(MapDownloadData(
                deliveryStatus=MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                errorContent = e.toString()))

            Log.e(_tag, "downloadMap - getDownloadData: ${e.toString()}")
            return null
        }
        Log.i(_tag, "downloadMap-> download-data: $downloadData")
        if (downloadData.deliveryStatus != MapDeliveryState.START) {
            downloadStatusHandler.invoke(downloadData)
            return null
        }
        val pkgUrl = downloadData.url!!
        val jsonUrl = PackageDownloader.changeFileExtensionToJson(pkgUrl)
        var pkgCompleted = false
        var jsonCompleted = false

        downloadData.deliveryStatus = MapDeliveryState.DOWNLOAD;
        downloadData.statusMessage = appCtx.getString(R.string.delivery_status_download)

        downloadData.jsonName = PackageDownloader.getFileNameFromUri(jsonUrl)
        downloadData.fileName = PackageDownloader.getFileNameFromUri(pkgUrl)

        downloadStatusHandler.invoke(downloadData)

//        val jsonCompletionHandler: (Long) -> Unit = {
//            Log.d(_tag,"processing json download ID=$it completion event...")
//            if (pkgCompleted){
//                Log.d(_tag,"stopping progress watcher...")
//                tmr?.cancel()
//                downloadData.deliveryStatus = MapDeliveryState.DONE;
//                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_done)
//                downloadData.downloadProgress = 100
//            }
//            jsonCompleted = true
//            downloadStatusHandler.invoke(downloadData);
//        }
//
//        val pkgCompletionHandler: (Long) -> Unit = {
//            Log.d(_tag,"processing pkg download ID=$it completion event...")
//            if (jsonCompleted){
//                Log.d(_tag,"stopping progress watcher...")
//                tmr?.cancel()
//                downloadData.deliveryStatus = MapDeliveryState.DONE;
//                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_done)
//                downloadData.downloadProgress = 100
//            }
//            pkgCompleted = true
//            downloadStatusHandler.invoke(downloadData);
//        }

        var jsonDownloadId: Long? = null
        var pkgDownloadId: Long? = null

        val completionHandler: (Long) -> Unit = {
            Log.d(_tag,"processing download ID=$it completion event...")
            if (jsonDownloadId != null && jsonDownloadId == it){
                jsonCompleted = true
            }else if(pkgDownloadId != null && jsonDownloadId == pkgDownloadId){
                pkgCompleted = true
                tmr?.cancel()
            }

            if (jsonCompleted && pkgCompleted){
                Log.d(_tag,"stopping progress watcher...")
                tmr?.cancel()

                downloadData.deliveryStatus = MapDeliveryState.DONE;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_done)
                downloadData.downloadProgress = 100
            }
            downloadStatusHandler.invoke(downloadData);
        }

        try{
            jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
            pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)
            Log.d(_tag, "downloadMap: jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")

        }catch (e: Exception){
            downloadData.deliveryStatus = MapDeliveryState.ERROR;
            downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
            downloadData.errorContent = e.message.toString();
            Log.e(_tag, "downloadMap - downloadFile: ${e.message.toString()} " )
            return null
        }
        Log.d(_tag, "downloadMap -> downloadId: $pkgDownloadId")

        Thread {


            tmr = timer(initialDelay = 100, period = 500) {
                val jsonProgress = downloader.queryProgress(jsonDownloadId)
                val fileProgress = downloader.queryProgress(pkgDownloadId)

                if (jsonProgress.first == -1L || fileProgress.first == -1L) {
                    Log.i(_tag, "downloadMap: DownloadManager to download file")
                    downloadData.deliveryStatus = MapDeliveryState.ERROR;
                    downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                    downloadStatusHandler.invoke(downloadData)
                    this.cancel()
                }
                if (jsonProgress.second > 0) {
                    val progress = (jsonProgress.first * 100 / jsonProgress.second).toInt()
                    Log.d(_tag, "jsonDownloadId: $jsonDownloadId -> process: $progress ")
                    if (progress >= 100) {
                        jsonCompleted = true;
                        if (pkgCompleted) {
                            Log.d(_tag, "stopping progress watcher...")
                            downloadData.deliveryStatus = MapDeliveryState.DONE;
                            downloadData.statusMessage =
                                appCtx.getString(R.string.delivery_status_done)
                            downloadData.downloadProgress = 100
                            downloadStatusHandler.invoke(downloadData)
                            this.cancel()
                        }
                    }
                }
                if (fileProgress.second > 0) {
                    val progress = (fileProgress.first * 100 / fileProgress.second).toInt()
                    Log.d(_tag, "pkgDownloadId: $pkgDownloadId -> process: $progress ")

                    if (progress < 100) {
                        downloadData.deliveryStatus = MapDeliveryState.DOWNLOAD;
                        downloadData.statusMessage =
                            appCtx.getString(R.string.delivery_status_download)
                    } else {
                        downloadData.deliveryStatus = MapDeliveryState.DONE;
                        downloadData.statusMessage = appCtx.getString(R.string.delivery_status_done)
                        this.cancel()
                    }
                    downloadData.downloadProgress = progress
                    downloadStatusHandler.invoke(downloadData)

                }
            }
        }.start()

        return downloadData.id
    }

    private fun getDownloadData(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): MapDownloadData {
        val mapDownloadData = MapDownloadData(deliveryStatus=MapDeliveryState.START);
        mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_req_sent)
        downloadStatusHandler.invoke(mapDownloadData)

        val retCreate = createMapImport(mp)
        mapDownloadData.id = retCreate?.importRequestId

        when(retCreate?.state){
            MapImportState.IN_PROGRESS -> Log.d(_tag,"deliverTile - createMapImport => IN_PROGRESS")
            MapImportState.START -> {
                mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress)
                downloadStatusHandler.invoke(mapDownloadData)

                if( !checkImportStatus(retCreate.importRequestId!!)){
                    mapDownloadData.deliveryStatus = MapDeliveryState.ERROR;
                    mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
//                TODO get the error message
                    return mapDownloadData
                }
            }
            MapImportState.DONE -> Log.d(_tag,"deliverTile - createMapImport => DONE")
            MapImportState.CANCEL -> {
                Log.w(_tag,"getDownloadData - createMapImport => CANCEL")
                mapDownloadData.deliveryStatus = MapDeliveryState.CANCEL;
                mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_canceled)

                return mapDownloadData
            }
            else -> {
                Log.e(_tag,"getDownloadData - createMapImport failed: ${retCreate?.state}")
                mapDownloadData.deliveryStatus = MapDeliveryState.ERROR;
                mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)

//                TODO get the error message
                return mapDownloadData
            }
        }

        val retDelivery = setMapImportDeliveryStart(retCreate.importRequestId!!)
        when(retDelivery?.state){
            MapDeliveryState.DONE -> Log.d(_tag,"deliverTile - setMapImportDeliveryStart => DONE")
            MapDeliveryState.START -> if( !checkDeliveryStatus(retCreate.importRequestId!!)) {
                mapDownloadData.deliveryStatus = MapDeliveryState.ERROR;
                mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)

//                TODO get the error message
                return mapDownloadData
            }
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE,
            MapDeliveryState.PAUSE ->  Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
            MapDeliveryState.CANCEL -> {
                Log.w(_tag,"getDownloadData - setMapImportDeliveryStart => CANCEL")
                mapDownloadData.deliveryStatus = MapDeliveryState.CANCEL;
                mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_canceled)

                return mapDownloadData
            }
            else -> {
                Log.e(_tag,"getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                mapDownloadData.deliveryStatus = MapDeliveryState.ERROR;
                mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
//                TODO get the error message
                return mapDownloadData
            }
        }

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(retCreate.importRequestId!!)
        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Log.e(_tag,"getDownloadData - prepared delivery status => ${deliveryStatus.status} is not done!")
            mapDownloadData.deliveryStatus = MapDeliveryState.ERROR;
            mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)

//                TODO get the error message
            return mapDownloadData
        }
        if (deliveryStatus.url == null){
            Log.e(_tag, "getDownloadData - download url is null", )
            mapDownloadData.deliveryStatus = MapDeliveryState.ERROR;
            mapDownloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
            mapDownloadData.errorContent = "getDownloadData - download url is null"
            return mapDownloadData

        }

        mapDownloadData.url = deliveryStatus.url
        return mapDownloadData
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatus(requestId: String) : Boolean {
        var stat = getCreateMapImportStatus(requestId)
        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state!! != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getCreateMapImportStatus(requestId)

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(_tag,"checkImportStatus - MapImportState.ERROR")
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState.CANCEL")
                    return false
                }
                else -> {}
            }

            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkImportStatus - timed out")
                return false
            }
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    private fun checkDeliveryStatus(requestId: String) : Boolean {
        var stat = getMapImportDeliveryStatus(requestId)
        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state != MapDeliveryState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getMapImportDeliveryStatus(requestId)
            when(stat?.state){
                MapDeliveryState.ERROR -> {
                    Log.e(_tag,"checkDeliveryStatus - MapDeliveryState.ERROR")
                    return false
                }
                MapDeliveryState.CANCEL -> {
                    Log.w(_tag,"checkDeliveryStatus - MapDeliveryState.CANCEL")
                    return false
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkDeliveryStatus - timed out")
                return false
            }
        }

        return true
    }
}