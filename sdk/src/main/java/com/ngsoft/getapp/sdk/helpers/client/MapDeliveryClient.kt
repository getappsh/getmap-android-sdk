package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.PrepareDeliveryReqDto
import GetApp.Client.models.PrepareDeliveryResDto
import android.util.Log
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

        Log.d(_tag,"setMapImportDeliveryStart | download url: ${status.url}")

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

        Log.d(_tag,"getMapImportDeliveryStatus | download url: ${status.url}")

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

    fun sendDeliveryStatus(client: GetAppClient, mapRepo: MapRepo, id: String, deviceId: String, state: MapDeliveryState?=null) {
        val mapPkg = mapRepo.getById(id) ?: return
        val deliveryStatus = DeliveryStatus(
            state = state ?: mapPkg.state,
            reqId = mapPkg.reqId ?: "-1",
            progress = mapPkg.downloadProgress,
            start = mapPkg.downloadStart?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            stop = mapPkg.downloadStop?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            done = mapPkg.downloadDone?.let { OffsetDateTime.of(it, ZoneOffset.UTC) }
        )

        Log.d(_tag, "sendDeliveryStatus: id: $id, state: ${deliveryStatus.state}, request id: ${deliveryStatus.reqId}")

        pushDeliveryStatus(client, deliveryStatus, deviceId)
    }

    private fun pushDeliveryStatus(client: GetAppClient, deliveryStatus: DeliveryStatus, deviceId: String){
        val status = when(deliveryStatus.state){
            MapDeliveryState.START -> DeliveryStatusDto.DeliveryStatus.start
            MapDeliveryState.DONE -> DeliveryStatusDto.DeliveryStatus.done
            MapDeliveryState.ERROR -> DeliveryStatusDto.DeliveryStatus.error
            MapDeliveryState.CANCEL -> DeliveryStatusDto.DeliveryStatus.cancelled
            MapDeliveryState.PAUSE -> DeliveryStatusDto.DeliveryStatus.pause
            MapDeliveryState.CONTINUE -> DeliveryStatusDto.DeliveryStatus.`continue`
            MapDeliveryState.DOWNLOAD -> DeliveryStatusDto.DeliveryStatus.download
            MapDeliveryState.DELETED -> DeliveryStatusDto.DeliveryStatus.deleted
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
            currentTime = OffsetDateTime.now()
        )
        Thread {
            try {
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            } catch (exc: Exception) {
                Log.e(_tag, "sendDeliveryStatus failed error: ${exc.message.toString()}")
                exc.printStackTrace()
            }
        }.start()
    }
}