package com.ngsoft.tilescache

import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.tilescache.dao.MapDAO
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.time.LocalDateTime
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

    fun setListener(id: String, dsh: (MapDownloadData) -> Unit){
        downloadStatusHandlers[id] = dsh
    }
    fun save(mapPkg: MapPkg): String{
        val id = dao.insert(mapPkg)
        return id.toString()
    }
    fun save(pId:String, bBox: String, fileName: String, jsonName: String, state: MapDeliveryState, statusMessage: String, flowState: DeliveryFlowState): String{
        val id = dao.insert(MapPkg(
            pId=pId,
            bBox=bBox,
            state=state,
            flowState=flowState,
            statusMessage = statusMessage,
            fileName = fileName,
            jsonName = jsonName,
        ))

        return id.toString()
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
        validationAttempt: Int? = null,
        connectionAttempt: Int? = null,
        mapAttempt: Int? = null,
        mapDone: Boolean? = null,
        jsonAttempt: Int? = null,
        jsonDone: Boolean? = null
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
            errorContent,
            validationAttempt,
            connectionAttempt,
            mapAttempt,
            mapDone,
            jsonAttempt,
            jsonDone
        )
        invoke(id)
    }
//    TODO potential issue with this calls, they are not in the same transaction
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
        validationAttempt: Int? = null,
        connectionAttempt: Int? = null,
        mapAttempt: Int? = null,
        mapDone: Boolean? = null,
        jsonAttempt: Int? = null,
        jsonDone: Boolean? = null,
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

            this.metadata.validationAttempt = validationAttempt ?: this.metadata.validationAttempt
            this.metadata.connectionAttempt = connectionAttempt ?: this.metadata.connectionAttempt

            this.metadata.mapAttempt = mapAttempt ?: this.metadata.mapAttempt
            this.metadata.mapDone = mapDone ?: this.metadata.mapDone

            this.metadata.jsonAttempt = jsonAttempt ?: this.metadata.jsonAttempt
            this.metadata.jsonDone = jsonDone ?: this.metadata.jsonDone



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

    fun updateAndReturn(id: String, mapDone: Boolean?=null, fileName: String?=null, jsonDone: Boolean?=null, jsonName: String?=null): MapPkg?{
        val mapPkg = this.getById(id) ?: return null
        mapPkg.apply {
            this.fileName = fileName ?: this.fileName
            this.jsonName = jsonName ?: this.jsonName

            this.metadata.mapDone = mapDone ?: this.metadata.mapDone
            this.metadata.jsonDone = jsonDone ?: this.metadata.jsonDone
        }
        return dao.updateAndReturn(mapPkg)
    }

    fun getById(id: String): MapPkg?{
        return try{
            dao.getById(id.toInt())
        }catch (e: NumberFormatException){
            null
        }
    }

    fun doesMapFileExist(name: String): Boolean{
        return dao.doesMapFileExist(name)
    }

    fun doesJsonFileExist(name: String): Boolean{
        return dao.doesJsonFileExist(name)
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

    companion object {
        val downloadStatusHandlers = ConcurrentHashMap<String, (MapDownloadData) -> Unit>()

    }

}