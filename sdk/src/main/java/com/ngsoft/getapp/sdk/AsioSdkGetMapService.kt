package com.ngsoft.getapp.sdk

import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.DownloadHebStatus
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

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (DownloadHebStatus, Int) -> Unit){
        Log.d(_tag, "downloadMap for -> $mp")
        var tmr: Timer? = null

        val downloadData = getDownloadData(mp, downloadStatusHandler)
        Log.i(_tag, "downloadMap-> download-data: $downloadData")
        if (downloadData?.url == null) {
            downloadStatusHandler.invoke(DownloadHebStatus.FAILED, 0)
            return
        }

//        val fileName = PackageDownloader.getFileNameFromUri(downloadData.url)

        val downloadCompletionHandler: (Long) -> Unit = {
            Log.d(_tag,"processing download ID=$it completion event...")
            Log.d(_tag,"stopping progress watcher...")
            tmr?.cancel()

            downloadStatusHandler.invoke(DownloadHebStatus.DONE, 100)

        }
        downloadStatusHandler.invoke(DownloadHebStatus.DOWNLOAD, 0)

        val downloadId = downloader.downloadFile(downloadData.url, downloadCompletionHandler)
        Log.d(_tag, "downloadMap -> downloadId: $downloadId")


        tmr = timer(initialDelay = 100, period = 500 ) {
            val fileProgress = downloader.queryProgress(downloadId)
            if(fileProgress.second > 0) {
                val progress = (fileProgress.first * 100 / fileProgress.second).toInt()
                Log.d(_tag, "downloadId: $downloadId -> process: $progress ")

                downloadStatusHandler.invoke(DownloadHebStatus.DOWNLOAD, progress)
            }
        }

    }



    private fun getDownloadData(mp: MapProperties, downloadStatus: (DownloadHebStatus, Int) -> Unit): PrepareDeliveryResDto? {
        val retCreate = createMapImport(mp)

        downloadStatus.invoke(DownloadHebStatus.REQUEST_SENT, 0)

        when(retCreate?.state){
            MapImportState.IN_PROGRESS -> Log.d(_tag,"deliverTile - createMapImport => IN_PROGRESS")
            MapImportState.START -> if( !checkImportStatus(retCreate.importRequestId!!)) return null
            MapImportState.DONE -> Log.d(_tag,"deliverTile - createMapImport => DONE")
            MapImportState.CANCEL -> {
                Log.w(_tag,"deliverTile - createMapImport => CANCEL")
                downloadStatus.invoke(DownloadHebStatus.CANCELED, 0)
                return null
            }
            else -> {
                Log.e(_tag,"deliverTile - createMapImport failed: ${retCreate?.state}")
                downloadStatus.invoke(DownloadHebStatus.FAILED, 0)
                return null
            }
        }

        downloadStatus.invoke(DownloadHebStatus.REQUEST_IN_PROCESS, 0)

        val retDelivery = setMapImportDeliveryStart(retCreate.importRequestId!!)
        when(retDelivery?.state){
            MapDeliveryState.DONE -> Log.d(_tag,"deliverTile - setMapImportDeliveryStart => DONE")
            MapDeliveryState.START -> if( !checkDeliveryStatus(retCreate.importRequestId!!)) return null
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE,
            MapDeliveryState.PAUSE ->  Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
            MapDeliveryState.CANCEL -> {
                Log.w(_tag,"deliverTile - setMapImportDeliveryStart => CANCEL")
                downloadStatus.invoke(DownloadHebStatus.CANCELED, 0)
                return null
            }
            else -> {
                Log.e(_tag,"deliverTile - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                downloadStatus.invoke(DownloadHebStatus.FAILED, 0)
                return null
            }
        }

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(retCreate.importRequestId!!)
        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Log.e(_tag,"deliverTile - prepared delivery status => ${deliveryStatus.status} is not done!")
            downloadStatus.invoke(DownloadHebStatus.FAILED, 0)
            return null
        }

        return deliveryStatus
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