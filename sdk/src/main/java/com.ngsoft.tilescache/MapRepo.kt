package com.ngsoft.tilescache

import GetApp.Client.models.DeliveryStatusDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

internal class MapRepo(ctx: Context) {

    private val _tag = "MapRepo"
    private val db: TilesDatabase
    private val dao: MapDAO

    init {
        Log.d(_tag,"MapRepo init...")
        db = TilesDatabase.connect(ctx)
        dao = db.mapDap()
    }

    fun create(pId:String, bBox: String, state: MapDeliveryState, statusMessage: String, flowState: DeliveryFlowState , dsh: (MapDownloadData) -> Unit): String{
        val id = dao.insert(MapPkg(
            pId=pId,
            bBox=bBox,
            state=state,
            flowState=flowState,
            statusMessage = statusMessage,
            downloadStart = LocalDateTime.now(ZoneOffset.UTC))).toString()

        downloadStatusHandlers[id] = dsh;
        return id
    }

    fun purge(){
        dao.nukeTable()
        //reset auto-increments
//        db.runInTransaction { db.query(SimpleSQLiteQuery("DELETE FROM sqlite_sequence")) }
    }

    fun getAll(): List<MapPkg>{
        return dao
            .getAll()
            .sortedBy { map-> map.downloadStart }
    }
    fun getAllMapsDownloadData(): List<MapDownloadData>{
        return getAll().map {mapPkg2DownloadData(it)}
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

        updateInternal(
            id,
            reqId,
            JDID,
            MDID,
            state,
            flowState,
            statusMessage,
            fileName,
            jsonName,
            url,
            downloadProgress,
            errorContent
        )
        invoke(id)
    }

    fun getById(id: String): MapPkg?{
        return try{
            dao.getById(id.toInt())
        }catch (e: NumberFormatException){
            null
        }
    }
    private fun updateInternal(
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
        cancelDownland: Boolean? = null
    ){
        val mapPkg = this.getById(id);
        mapPkg?.apply {
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
            this.cancelDownload = cancelDownland ?: this.cancelDownload

            if (state == MapDeliveryState.CANCEL && this.cancelDownload){
                this.downloadStop = LocalDateTime.now(ZoneOffset.UTC)
                this.cancelDownload = false
            }
            else if (state == MapDeliveryState.CANCEL ||state == MapDeliveryState.PAUSE){
                this.downloadStop = LocalDateTime.now(ZoneOffset.UTC)
            }else if(state == MapDeliveryState.CONTINUE){
                this.downloadStart = LocalDateTime.now(ZoneOffset.UTC)
            }else if(state == MapDeliveryState.DONE){
                this.downloadDone = LocalDateTime.now(ZoneOffset.UTC)
            }
            dao.update(this)
        }
    }

    fun updateFlowState(id: String, flowState: DeliveryFlowState){
        this.updateInternal(id, flowState=flowState)
    }

    fun remove(id: String){
        try {
            update(id, state = MapDeliveryState.DELETED)
            dao.deleteById(id.toInt())
            downloadStatusHandlers.remove(id)
        }catch (_: NumberFormatException){ }
    }

    fun setCancelDownload(id: String){
        this.updateInternal(id, cancelDownland = true)
    }

    fun getUrl(id: String):String?{
        return this.getById(id)?.url
    }

    fun getReqId(id: String): String?{
        return this.getById(id)?.reqId
    }

    fun getDeliveryFlowState(id: String): DeliveryFlowState?{
        return this.getById(id)?.flowState
    }

    fun isDownloadCanceled(id: String): Boolean{
        return this.getById(id)?.cancelDownload ?: false
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
        val map = this.getById(id);
        if (map != null) {
            return mapPkg2DownloadData(map)
        }
        return null
    }
    private fun mapPkg2DownloadData(map: MapPkg): MapDownloadData{
        return MapDownloadData(
            id = map.id.toString(),
            bBox = map.bBox,
            fileName = map.fileName,
            jsonName = map.jsonName,
            deliveryStatus = map.state,
            url = map.url,
            statusMessage = map.statusMessage,
            downloadProgress = map.downloadProgress,
            errorContent = map.errorContent
        )
    }

    fun getDeliveryStatus(id: String, deviceId: String): DeliveryStatusDto? {
        val map = this.getById(id);
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
            Log.d(_tag, "getDeliveryStatus: $map")
            return DeliveryStatusDto(
                type = DeliveryStatusDto.Type.map,
                deviceId = deviceId,
                deliveryStatus = status,
                catalogId = map.reqId,
                downloadData = map.downloadProgress.toBigDecimal() ?: null,
                downloadStart = if (map.downloadStart != null) OffsetDateTime.of(map.downloadStart, ZoneOffset.UTC) else null,
                downloadStop = if (map.downloadStop != null) OffsetDateTime.of(map.downloadStop, ZoneOffset.UTC) else null,
                downloadDone = if(map.downloadDone != null) OffsetDateTime.of(map.downloadDone, ZoneOffset.UTC) else null,
                currentTime = OffsetDateTime.now()
            )
        }
        return null
    }

    companion object {
        val downloadStatusHandlers = ConcurrentHashMap<String, (MapDownloadData) -> Unit>()

    }

}