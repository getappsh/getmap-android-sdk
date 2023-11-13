package com.ngsoft.getapp.sdk

import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
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




    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        deliveryTimeoutMinutes = configuration.deliveryTimeout
        downloadTimeoutMinutes = configuration.downloadTimeout
        return true
    }

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String?{
        val id = abs((0..999999999999).random()).toString()
        Log.i(_tag, "downloadMap: id: $id")

        Thread{
            var downloadData = MapDownloadData(
                deliveryStatus = MapDeliveryState.START,
                statusMessage = appCtx.getString(R.string.delivery_status_req_sent)
            )
            downloadStatusHandler.invoke(downloadData)

            try {
                downloadData = importCreate(mp, downloadData)
                downloadStatusHandler.invoke(downloadData)
                if (downloadData.deliveryStatus != MapDeliveryState.START) {
                    return@Thread
                }
                downloadData = checkImportStatue(downloadData)

                if (downloadData.deliveryStatus != MapDeliveryState.START) {
                    downloadStatusHandler.invoke(downloadData)
                    return@Thread
                }

                downloadData = importDelivery(downloadData)
                if (downloadData.deliveryStatus != MapDeliveryState.START) {
                    downloadStatusHandler.invoke(downloadData)
                    return@Thread
                }

                downloadData = checkDeliveryStatus(downloadData)
                if (downloadData.deliveryStatus != MapDeliveryState.START) {
                    downloadStatusHandler.invoke(downloadData)
                    return@Thread
                }
            } catch (e: Exception) {
                Log.e(_tag, "downloadMap: exception:  ${e.message.toString()}")
                downloadData.deliveryStatus = MapDeliveryState.ERROR;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                downloadData.errorContent = e.message.toString();
                downloadStatusHandler.invoke(downloadData)
            }


            val pkgUrl = downloadData.url!!
            val jsonUrl = PackageDownloader.changeFileExtensionToJson(pkgUrl)

            downloadData.jsonName = PackageDownloader.getFileNameFromUri(jsonUrl)
            downloadData.fileName = PackageDownloader.getFileNameFromUri(pkgUrl)

            var pkgCompleted = false
            var jsonCompleted = false
            var jsonDownloadId: Long? = null
            var pkgDownloadId: Long? = null

            val completionHandler: (Long) -> Unit = {
                Log.d(_tag, "processing download ID=$it completion event...")
            }

            try {
                jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
                pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)

                Log.d(
                    _tag,
                    "downloadMap: jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId"
                )

                downloadData.deliveryStatus = MapDeliveryState.DOWNLOAD;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_download)
                downloadStatusHandler.invoke(downloadData)
            } catch (e: Exception) {
                Log.e(_tag, "downloadMap - downloadFile: ${e.message.toString()} ")
                downloadData.deliveryStatus = MapDeliveryState.ERROR;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                downloadData.errorContent = e.message.toString();
                downloadStatusHandler.invoke(downloadData)
                return@Thread
            }

            timer(initialDelay = 100, period = 500) {
                val jsonProgress = downloader.queryProgress(jsonDownloadId)
                val fileProgress = downloader.queryProgress(pkgDownloadId)

                if (jsonProgress.first == -1L || fileProgress.first == -1L) {
                    Log.i(_tag, "downloadMap: DownloadManager failed to download file")
                    downloadData.deliveryStatus = MapDeliveryState.ERROR;
                    downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                    downloadData.errorContent =
                        "downloadMap: DownloadManager failed to download file"
                    downloadStatusHandler.invoke(downloadData)
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

                    if (progress < 100) {
                        downloadData.deliveryStatus = MapDeliveryState.DOWNLOAD;
                        downloadData.statusMessage =
                            appCtx.getString(R.string.delivery_status_download)
                    } else {
                        pkgCompleted = true
                    }
                    downloadData.downloadProgress = progress

                    downloadStatusHandler.invoke(downloadData)
                }

                if (pkgCompleted && jsonCompleted) {
                    Log.d(_tag, "downloading Done: ")
                    Log.d(_tag, "stopping progress watcher...")
                    downloadData.deliveryStatus = MapDeliveryState.DONE;
                    downloadData.statusMessage =
                        appCtx.getString(R.string.delivery_status_done)
                    downloadData.downloadProgress = 100
                    downloadStatusHandler.invoke(downloadData)

                    this.cancel()
                }
            }
        }.start()
        return id
    }
    private fun importCreate(mp: MapProperties, downloadData: MapDownloadData): MapDownloadData{
        Log.d(_tag, "importCreate")
        val retCreate = createMapImport(mp)

        downloadData.id = retCreate?.importRequestId

        when(retCreate?.state){
            MapImportState.START, MapImportState.DONE ->{
                Log.d(_tag,"deliverTile - createMapImport => OK: ${retCreate.state} ")
                downloadData.deliveryStatus = MapDeliveryState.START
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress)
            }
            MapImportState.CANCEL -> {
                Log.w(_tag,"getDownloadData - createMapImport => CANCEL")
                downloadData.deliveryStatus = MapDeliveryState.CANCEL;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_canceled)
            }
            else -> {
                Log.e(_tag,"getDownloadData - createMapImport failed: ${retCreate?.state}")
                downloadData.deliveryStatus = MapDeliveryState.ERROR;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                downloadData.errorContent = retCreate?.importRequestId
            }
        }

        return downloadData
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatue(downloadData: MapDownloadData) :  MapDownloadData{
        Log.d(_tag, "checkImportStatue")
        var stat = getCreateMapImportStatus(downloadData.id)

        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state!! != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(2)
            stat = getCreateMapImportStatus(downloadData.id)

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(_tag,"checkImportStatus - MapImportState.ERROR")
                    downloadData.deliveryStatus = MapDeliveryState.ERROR;
                    downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
//                    todo error message
                    return downloadData
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState.CANCEL")
                    downloadData.deliveryStatus = MapDeliveryState.CANCEL;
                    downloadData.statusMessage = appCtx.getString(R.string.delivery_status_canceled)
                    return downloadData

                }
                else -> {}
            }

            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkImportStatus - timed out")
                downloadData.deliveryStatus = MapDeliveryState.ERROR;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                downloadData.errorContent = "checkImportStatus - timed out"
                return downloadData

            }
        }

        Log.d(_tag, "checkImportStatue: MapImportState.Done")
        return downloadData
    }

    private fun importDelivery(downloadData: MapDownloadData): MapDownloadData{
        Log.d(_tag, "importDelivery")
        val retDelivery = setMapImportDeliveryStart(downloadData.id!!)

        when(retDelivery?.state){
            MapDeliveryState.DONE,
            MapDeliveryState.START,
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE ->  {
                Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
                return downloadData
            }
            MapDeliveryState.CANCEL,  MapDeliveryState.PAUSE -> {
                Log.w(_tag,"getDownloadData - setMapImportDeliveryStart => CANCEL")
                downloadData.deliveryStatus = MapDeliveryState.CANCEL;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_canceled)

                return downloadData
            }
            else -> {
                Log.e(_tag,"getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                downloadData.deliveryStatus = MapDeliveryState.ERROR;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                downloadData.errorContent = "getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}"
                return downloadData
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun checkDeliveryStatus(downloadData: MapDownloadData) : MapDownloadData {
        Log.d(_tag, "checkDeliveryStatus")

        var deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(downloadData.id!!)

        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (deliveryStatus.status != PrepareDeliveryResDto.Status.done){
            TimeUnit.SECONDS.sleep(2)
            deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(downloadData.id!!)

            when(deliveryStatus.status){
                PrepareDeliveryResDto.Status.error -> {
                    Log.e(_tag,"getDownloadData - prepared delivery status: Error")
                    downloadData.deliveryStatus = MapDeliveryState.ERROR;
                    downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                    downloadData.errorContent = "getDownloadData - prepared delivery status: Error"
                    return downloadData
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkDeliveryStatus - timed out")
                downloadData.deliveryStatus = MapDeliveryState.ERROR;
                downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
                downloadData.errorContent = "checkDeliveryStatus - timed out"
                return downloadData
            }
        }


        if (deliveryStatus.url == null){
            Log.e(_tag, "getDownloadData - download url is null", )
            downloadData.deliveryStatus = MapDeliveryState.ERROR;
            downloadData.statusMessage = appCtx.getString(R.string.delivery_status_failed)
            downloadData.errorContent = "getDownloadData - download url is null"
            return downloadData

        }

        downloadData.url = deliveryStatus.url
        return downloadData
    }
}