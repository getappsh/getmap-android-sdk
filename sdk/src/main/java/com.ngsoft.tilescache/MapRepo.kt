package com.ngsoft.tilescache

import GetApp.Client.models.DeliveryStatusDto
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

internal class MapRepo {

    private val _tag = "MapRepo"


    fun create(pId:String, bBox: String, state: MapDeliveryState, statusMessage: String, flowState: DeliveryFlowState , dsh: (MapDownloadData) -> Unit): String{
        val id = abs((0..999999999999).random()).toString()
        hashMapData[id] = MapPkg(
            id=id, pId=pId,
            bBox=bBox,
            state=state,
            flowState=flowState,
            statusMessage = statusMessage,
            downloadStart = OffsetDateTime.now())
        downloadStatusHandlers[id] = dsh;
        return id
    }

    fun update(
        id: String,
        reqId: String? = null,
        JDID: Long? = null,
        MDID: Long? = null,
        state: MapDeliveryState? = null,
        flowState: DeliveryFlowState? = null,
        statusMessage: String? = null,
        fileName: String? = null,
        jsonName: String? = null,
        url: String? = null,
        downloadProgress: Int? = null,
        errorContent: String? = null,
    ) {
        hashMapData[id]?.apply {
            this.reqId = reqId ?: this.reqId
            this.JDID = JDID ?: this.JDID
            this.MDID = MDID ?: this.MDID
            this.fileName = fileName ?: this.fileName
            this.jsonName = jsonName ?: this.jsonName
            this.url = url ?: this.url
            this.state = state ?: this.state
            this.flowState = flowState ?: this.flowState
            this.statusMessage = statusMessage ?: this.statusMessage
            this.downloadProgress = downloadProgress ?: this.downloadProgress
            this.errorContent = errorContent ?: this.errorContent

            if (state == MapDeliveryState.CANCEL && this.cancelDownload){
                this.downloadStop = OffsetDateTime.now()
                this.cancelDownload = false
            }
            else if (state == MapDeliveryState.CANCEL ||state == MapDeliveryState.PAUSE){
                this.downloadStop = OffsetDateTime.now()
            }else if(state == MapDeliveryState.CONTINUE){
                this.downloadStart = OffsetDateTime.now()
            }else if(state == MapDeliveryState.DONE){
                this.downloadDone = OffsetDateTime.now()
            }
        }

        invoke(id)
    }

    fun updateFlowState(id: String, flowState: DeliveryFlowState){
        hashMapData[id]?.flowState = flowState
    }

    fun remove(id: String){
        update(id, state = MapDeliveryState.DELETED)
        hashMapData.remove(id)
        downloadStatusHandlers.remove(id)
    }
    fun setCancelDownload(id: String){
        hashMapData[id]?.cancelDownload = true

    }
    fun getUrl(id: String):String?{
        return hashMapData[id]?.url
    }
    fun getReqId(id: String): String?{
        return hashMapData[id]?.reqId
    }
    fun isDownloadCanceled(id: String): Boolean{
        return hashMapData[id]?.cancelDownload ?: false
    }

    fun invoke(id: String){
        val map = getDownloadData(id);
        if (map != null) {
            downloadStatusHandlers[id]?.invoke(map)
        }else{
            Log.e(_tag, "invoke: not found map id: $id", )
        }
    }
    fun getDownloadData(id: String): MapDownloadData?{
        val map = hashMapData[id];
        if (map != null) {
            return MapDownloadData(
                id = id,
                fileName = map.fileName,
                jsonName = map.jsonName,
                deliveryStatus = map.state,
                url = map.url,
                statusMessage = map.statusMessage,
                downloadProgress = map.downloadProgress,
                errorContent = map.errorContent
            )
        }
        return null
    }

    fun getDeliveryStatus(id: String, deviceId: String): DeliveryStatusDto? {
        val map = hashMapData[id];
        if (map != null) {
            val status = when(map.state){
                MapDeliveryState.START -> DeliveryStatusDto.DeliveryStatus.start
                MapDeliveryState.DONE -> DeliveryStatusDto.DeliveryStatus.done
                MapDeliveryState.ERROR -> DeliveryStatusDto.DeliveryStatus.error
                MapDeliveryState.CANCEL -> DeliveryStatusDto.DeliveryStatus.cancelled
                MapDeliveryState.PAUSE -> DeliveryStatusDto.DeliveryStatus.pause
                MapDeliveryState.CONTINUE -> DeliveryStatusDto.DeliveryStatus.`continue`
                MapDeliveryState.DOWNLOAD -> DeliveryStatusDto.DeliveryStatus.download
                MapDeliveryState.DELETED -> DeliveryStatusDto.DeliveryStatus.deleted
            }
            return DeliveryStatusDto(
                type = DeliveryStatusDto.Type.map,
                deviceId = deviceId,
                deliveryStatus = status,
                catalogId = map.reqId,
                downloadData = map.downloadProgress.toBigDecimal() ?: null,
                downloadStart = map.downloadStart,
                downloadStop = map.downloadStop,
                downloadDone = map.downloadDone,
                currentTime = OffsetDateTime.now()
            )
        }
        return null
    }

    companion object {
        val hashMapData = ConcurrentHashMap<String, MapPkg>()
        val downloadStatusHandlers = ConcurrentHashMap<String, (MapDownloadData) -> Unit>()

    }

}