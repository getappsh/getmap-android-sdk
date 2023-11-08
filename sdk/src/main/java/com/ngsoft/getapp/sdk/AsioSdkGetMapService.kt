package com.ngsoft.getapp.sdk

import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import java.util.concurrent.TimeUnit
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

    override fun downloadMap(mp: MapProperties){
        val downloadData = getDownloadData(mp)
        Log.i(_tag, "downloadMap-> download-data: $downloadData")
        if (downloadData?.url == null) {
            return
        }


        val downloadCompletionHandler: (Long) -> Unit = {
            Log.d(_tag,"processing download ID=$it completion event...")
        }
        val downloadId = downloader.downloadFile(downloadData.url, downloadCompletionHandler)

    }



    private fun getDownloadData(mp: MapProperties): PrepareDeliveryResDto? {
        val retCreate = createMapImport(mp)
        when(retCreate?.state){
            MapImportState.IN_PROGRESS -> Log.d(_tag,"deliverTile - createMapImport => IN_PROGRESS")
            MapImportState.START -> if( !checkImportStatus(retCreate.importRequestId!!)) return null
            MapImportState.DONE -> Log.d(_tag,"deliverTile - createMapImport => DONE")
            MapImportState.CANCEL -> {
                Log.w(_tag,"deliverTile - createMapImport => CANCEL")
                return null
            }
            else -> {
                Log.e(_tag,"deliverTile - createMapImport failed: ${retCreate?.state}")
                return null
            }
        }

        val retDelivery = setMapImportDeliveryStart(retCreate.importRequestId!!)
        when(retDelivery?.state){
            MapDeliveryState.DONE -> Log.d(_tag,"deliverTile - setMapImportDeliveryStart => DONE")
            MapDeliveryState.START -> if( !checkDeliveryStatus(retCreate.importRequestId!!)) return null
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE,
            MapDeliveryState.PAUSE ->  Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
            MapDeliveryState.CANCEL -> {
                Log.w(_tag,"deliverTile - setMapImportDeliveryStart => CANCEL")
                return null
            }
            else -> {
                Log.e(_tag,"deliverTile - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                return null
            }
        }

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(retCreate.importRequestId!!)
        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Log.e(_tag,"deliverTile - prepared delivery status => ${deliveryStatus.status} is not done!")
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