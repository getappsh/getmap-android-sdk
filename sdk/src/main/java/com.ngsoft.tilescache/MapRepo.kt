package com.ngsoft.tilescache

import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.tilescache.models.MapPkg
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

internal class MapRepo {

    private val _tag = "MapRepo"


    fun create(pId:String, bBox: String, state: MapDeliveryState, statusMessage: String, dsh: (MapDownloadData) -> Unit): String{
        val id = abs((0..999999999999).random()).toString()
        hashMapData[id] = MapPkg(id=id, pId=pId, bBox=bBox, state=state, statusMessage = statusMessage)
        downloadStatusHandlers[id] = dsh;
        return id
    }

//    fun get(key: String): MapDownloadData?{
//
//        return hashMapData.get(key)
//    }
    fun update(
        id: String,
        reqId: String? = null,
        JDID: Long? = null,
        MDID: Long? = null,
        state: MapDeliveryState? = null,
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
            this.statusMessage = statusMessage ?: this.statusMessage
            this.downloadProgress = downloadProgress ?: this.downloadProgress
            this.errorContent = errorContent ?: this.errorContent
            if (state == MapDeliveryState.CANCEL){
                this.cancelDownload = false
            }
        }

        invoke(id)
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
        val map = hashMapData[id];
        if (map != null) {
            downloadStatusHandlers[id]?.invoke(
                MapDownloadData(
                    id = id,
                    fileName = map.fileName,
                    jsonName = map.jsonName,
                    deliveryStatus = map.state,
                    url = map.url,
                    statusMessage = map.statusMessage,
                    downloadProgress = map.downloadProgress,
                    errorContent = map.errorContent
                )
            )
        }else{
            Log.e(_tag, "invoke: not found map id: $id", )
        }
    }

    companion object {
        val hashMapData = ConcurrentHashMap<String, MapPkg>()
        val downloadStatusHandlers = ConcurrentHashMap<String, (MapDownloadData) -> Unit>()

    }

}