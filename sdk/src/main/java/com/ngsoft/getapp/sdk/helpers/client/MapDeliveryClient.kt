package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.PrepareDeliveryReqDto
import GetApp.Client.models.PrepareDeliveryResDto
import timber.log.Timber
import com.ngsoft.getapp.sdk.models.DeliveryStatus
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal object MapDeliveryClient {

    private const val _tag = "MapDeliveryClient"
     fun setMapImportDeliveryStart(client: GetAppClient, inputImportRequestId: String?, deviceId: String): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val prepareDelivery = PrepareDeliveryReqDto(inputImportRequestId, deviceId, PrepareDeliveryReqDto.ItemType.map)
        val status = client.deliveryApi.deliveryControllerPrepareDelivery(prepareDelivery)

        Timber.d("setMapImportDeliveryStart | download url: ${status.url}")

        val result = MapImportDeliveryStatus()
        result.importRequestId = inputImportRequestId
        result.message = Status()
        result.message!!.statusCode = StatusCode.SUCCESS
        result.url = status.url

        when (status.status){
            PrepareDeliveryResDto.Status.start -> result.state = MapDeliveryState.START
            PrepareDeliveryResDto.Status.inProgress -> result.state = MapDeliveryState.CONTINUE
            PrepareDeliveryResDto.Status.done -> result.state = MapDeliveryState.DONE
            PrepareDeliveryResDto.Status.error -> result.state = MapDeliveryState.ERROR
        }

        return result
    }

    fun getMapImportDeliveryStatus(client: GetAppClient, inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(inputImportRequestId)

        Timber.d("getMapImportDeliveryStatus | download url: ${status.url}")

        val result = MapImportDeliveryStatus()
        result.importRequestId = status.catalogId
        result.message = Status()
        result.message!!.statusCode = StatusCode.SUCCESS
        result.url = status.url

        when (status.status){
            PrepareDeliveryResDto.Status.start -> result.state = MapDeliveryState.START
            PrepareDeliveryResDto.Status.inProgress -> result.state = MapDeliveryState.CONTINUE
            PrepareDeliveryResDto.Status.done -> result.state = MapDeliveryState.DONE
            PrepareDeliveryResDto.Status.error -> result.state = MapDeliveryState.ERROR
        }

        return result
    }

    fun sendDeliveryStatus(client: GetAppClient, mapRepo: MapRepo, id: String, deviceId: String, state: MapDeliveryState?=null, downloaded: Long?=null, downloadedBytesPerSecond: Long?=null, etaInMilliSeconds: Long?=null) {
        val mapPkg = mapRepo.getById(id) ?: return
        val deliveryStatus = DeliveryStatus(
            state = state ?: mapPkg.state,
            reqId = mapPkg.reqId ?: "-1",
            progress = mapPkg.downloadProgress,
            start = mapPkg.downloadStart?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            stop = mapPkg.downloadStop?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            done = mapPkg.downloadDone?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            downloaded = downloaded,
            downloadedBytesPerSecond = downloadedBytesPerSecond,
            etaInMilliSeconds = etaInMilliSeconds
        )

        Timber.d("sendDeliveryStatus: id: $id, state: ${deliveryStatus.state}, request id: ${deliveryStatus.reqId}")

        pushDeliveryStatus(client, deliveryStatus, deviceId)
    }

    private fun pushDeliveryStatus(client: GetAppClient, deliveryStatus: DeliveryStatus, deviceId: String){
        val status = when(deliveryStatus.state){
            MapDeliveryState.START -> DeliveryStatusDto.DeliveryStatus.Start
            MapDeliveryState.DONE -> DeliveryStatusDto.DeliveryStatus.Done
            MapDeliveryState.ERROR -> DeliveryStatusDto.DeliveryStatus.Error
            MapDeliveryState.CANCEL -> DeliveryStatusDto.DeliveryStatus.Cancelled
            MapDeliveryState.PAUSE -> DeliveryStatusDto.DeliveryStatus.Pause
            MapDeliveryState.CONTINUE -> DeliveryStatusDto.DeliveryStatus.Continue
            MapDeliveryState.DOWNLOAD -> DeliveryStatusDto.DeliveryStatus.Download
            MapDeliveryState.DELETED -> DeliveryStatusDto.DeliveryStatus.Deleted
        }

        val dlv = DeliveryStatusDto(
            type = DeliveryStatusDto.Type.map,
            deviceId = deviceId,
            deliveryStatus = status,
            catalogId = deliveryStatus.reqId,
            downloadData = deliveryStatus.progress?.toBigDecimal(),
            downloadStart = deliveryStatus.start,
            downloadStop = deliveryStatus.stop,
            downloadDone = deliveryStatus.done,
            currentTime = OffsetDateTime.now(),
            bitNumber = deliveryStatus.downloaded?.toBigDecimal(),
            downloadSpeed = deliveryStatus.downloadedBytesPerSecond?.toBigDecimal(),
            downloadEstimateTime = deliveryStatus.etaInMilliSeconds,
            itemKey = "gpkg"
        )
        Thread {
            try {
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            } catch (exc: Exception) {
                Timber.e("sendDeliveryStatus failed error: ${exc.message.toString()}")
            }
        }.start()
    }
}