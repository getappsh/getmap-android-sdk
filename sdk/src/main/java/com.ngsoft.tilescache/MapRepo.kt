package com.ngsoft.tilescache

import android.content.Context
import timber.log.Timber
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.tilescache.dao.MapDAO
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

internal class MapRepo(ctx: Context) {

    private val db: TilesDatabase
    private val dao: MapDAO

    init {
        Timber.d("MapRepo init...")
        db = TilesDatabase.getInstance(ctx)
        dao = db.mapDap()
    }

    fun purge(){
        dao.nukeTable()
        //reset auto-increments
//        db.runInTransaction { db.query(SimpleSQLiteQuery("DELETE FROM sqlite_sequence")) }
    }

    fun create(pId:String, bBox: String, state: MapDeliveryState, statusMessage: String, flowState: DeliveryFlowState): String{
        val id = dao.insert(MapPkg(
            pId=pId,
            bBox=bBox,
            state=state,
            flowState=flowState,
            statusMsg = statusMessage,
            downloadStart = LocalDateTime.now(ZoneOffset.UTC))).toString()

        return id
    }
    fun save(mapPkg: MapPkg): String{
        val id = dao.insert(mapPkg)
        return id.toString()
    }
    fun getAll(): List<MapPkg>{
        return dao
            .getAll()
            .sortedByDescending { map-> map.reqDate }
    }

    fun getAllMaps(): List<MapData>{
        return getAll().map { mapPkg2DownloadData(it) }.sortedWith(comparator)
    }

    fun getAllMapsLiveData(): LiveData<List<MapData>>{
        Timber.i("getAllMapsLiveData")
        if (mapMutableLiveHase.value?.isEmpty() != false){
            Thread{ mapMutableLiveHase.postValue(getAll().map { mapPkg2DownloadData(it) }.associateBy { it.id!! }.toConcurrentHashMap()) }.start()
        }
        return mapLiveList
    }

    fun getByBBox(bBox: String, footprint: String?=null): List<MapPkg>{
        val bBoxList = FootprintUtils.toList(bBox)

        val mapList = this.getAll()
        val result = mutableSetOf<MapPkg>()

        result.addAll(mapList.filter { pkg ->
            FootprintUtils.toList(pkg.bBox) == bBoxList ||
                    pkg.footprint?.let { FootprintUtils.toList(it) } == bBoxList})

        if (footprint != null){
            val footprintList = FootprintUtils.toList(footprint)
            result.addAll(mapList.filter { pkg ->
                FootprintUtils.toList(pkg.bBox) == footprintList ||
                        pkg.footprint?.let { FootprintUtils.toList(it) } == footprintList})
        }

        return result.toList()
    }

    fun update(
        id: String,
        reqId: String? = null,
        JDID: Long? = null,
        MDID: Long? = null,
        state: MapDeliveryState? = null,
        flowState: DeliveryFlowState? = null,
        statusMsg: String? = null,
        fileName: String? = null,
        jsonName: String? = null,
        url: String? = null,
        path: String? = null,
        downloadProgress: Int? = null,
        statusDescr: String? = null,
        validationAttempt: Int? = null,
        connectionAttempt: Int? = null,
        mapAttempt: Int? = null,
        mapDone: Boolean? = null,
        jsonAttempt: Int? = null,
        jsonDone: Boolean? = null,
        downloadDone: LocalDateTime? = null,
    ) {
        var map: MapPkg? = null;

        if (state == MapDeliveryState.ERROR){
             map = this.getById(id)
        }
        this.dao.updateMapFields(
            id,
            reqId=reqId,
            JDID=JDID,
            MDID=MDID,
            state=state,
            flowState=flowState,
            statusMsg=statusMsg,
            fileName=fileName,
            jsonName=jsonName,
            url=url,
            path=path,
            downloadProgress=downloadProgress,
            statusDescr=statusDescr,
            validationAttempt=validationAttempt,
            connectionAttempt=connectionAttempt,
            mapAttempt=mapAttempt,
            mapDone=mapDone,
            jsonAttempt=jsonAttempt,
            jsonDone=jsonDone,
            downloadDone=downloadDone
        )
        invoke(id)
        if (state == MapDeliveryState.ERROR && map?.state != MapDeliveryState.ERROR){
            Timber.i("New download error, id: $id")
            onDownloadErrorListener?.invoke(id)
        }
    }
//    TODO potential issue with this calls, they are not in the same transaction
    private fun updateInternal(
        id: String,
        reqId: String? = null,
        JDID: Long? = null,
        MDID: Long? = null,
        state: MapDeliveryState? = null,
        flowState: DeliveryFlowState? = null,
        statusMsg: String? = null,
        fileName: String? = null,
        jsonName: String? = null,
        url: String? = null,
        downloadProgress: Int? = null,
        statusDescr: String? = null,
        validationAttempt: Int? = null,
        connectionAttempt: Int? = null,
        mapAttempt: Int? = null,
        mapDone: Boolean? = null,
        jsonAttempt: Int? = null,
        jsonDone: Boolean? = null,
        cancelDownload: Boolean? = null
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
            this.statusMsg = statusMsg ?: this.statusMsg
            this.downloadProgress = downloadProgress ?: this.downloadProgress
            this.statusDescr = statusDescr ?: this.statusDescr
            this.cancelDownload = cancelDownload ?: this.cancelDownload

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
            else if (state == MapDeliveryState.CANCEL ||state == MapDeliveryState.PAUSE || state == MapDeliveryState.ERROR
                && (this.state != MapDeliveryState.CANCEL && this.state != MapDeliveryState.PAUSE && this.state != MapDeliveryState.ERROR)){
                this.downloadStop = LocalDateTime.now(ZoneOffset.UTC)
            }else if(state == MapDeliveryState.CONTINUE){
                this.downloadStart = LocalDateTime.now(ZoneOffset.UTC)
            }else if(state == MapDeliveryState.DONE){
                this.downloadDone = LocalDateTime.now(ZoneOffset.UTC)
            }
            if ((state == MapDeliveryState.START ||
                        state == MapDeliveryState.CONTINUE ||
                        state == MapDeliveryState.DOWNLOAD) &&
                this.downloadStart == null){
                this.downloadStart = LocalDateTime.now(ZoneOffset.UTC)
            }
            dao.update(this)
        }
    }



    fun updateAndReturn(id: String, mapDone: Boolean?=null, fileName: String?=null, jsonDone: Boolean?=null, jsonName: String?=null): MapPkg?{
        dao.updateMapFields(id, mapDone=mapDone, fileName=fileName, jsonDone=jsonDone, jsonName=jsonName)
        return this.getById(id)
    }

    fun setFootprint(id: String, footprint: String){
        this.dao.updateMapFields(id, footprint=footprint)
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

    fun remove(id: String){
        try {
            update(id, state = MapDeliveryState.DELETED)
            dao.deleteById(id.toInt())

            mapMutableLiveHase.value?.remove(id)
            mapMutableLiveHase.postValue(mapMutableLiveHase.value)

        }catch (_: NumberFormatException){ }
    }

    fun setCancelDownload(id: String){
        this.dao.updateMapFields(id, cancelDownload = true)
    }

    fun getReqId(id: String): String?{
        return this.getById(id)?.reqId
    }

    fun isDownloadCanceled(id: String): Boolean{
        return this.getById(id)?.cancelDownload ?: false
    }

    fun invoke(id: String){
        val map = getDownloadData(id);
        if (map != null) {
            if (mapMutableLiveHase.value?.isEmpty() != false){
                Thread{ mapMutableLiveHase.postValue(getAll().map { mapPkg2DownloadData(it) }.associateBy { it.id!! }.toConcurrentHashMap()) }.start()
            }else{
                mapMutableLiveHase.value?.set(id, map)
                mapMutableLiveHase.postValue(mapMutableLiveHase.value)

            }

        }else{
            Timber.e("invoke: not found map id: $id", )
        }
    }

    fun isMapUpdated(id: String): Boolean?{
        return this.getById(id)?.isUpdated
    }

    fun setMapUpdated(id: String, isUpdated: Boolean){
        this.dao.updateMapFields(id, isUpdated=isUpdated)
        this.invoke(id)
    }
    fun getMapsToUpdate(): List<String>{
        return this.getAll().filter { !it.isUpdated }.map { it.id.toString() }
    }
    fun setMapsUpdatedValue(values: Map<String, Boolean>){
        values.forEach{ (reqId, isUpdated) ->
            this.dao.setUpdatedByReqId(reqId, isUpdated)
            val id = this.dao.getByReqId(reqId)?.id ?: return@forEach
            invoke(id.toString())
        }
        onInventoryUpdatesListener?.invoke(this.getMapsToUpdate())
    }
    fun getDownloadData(id: String): MapData?{
        val map = this.getById(id);
        if (map != null) {
            return mapPkg2DownloadData(map)
        }
        return null
    }
    private fun mapPkg2DownloadData(map: MapPkg): MapData{
        val localZone = ZoneId.systemDefault()
        return MapData(
            id = map.id.toString(),
            footprint = map.footprint ?: map.bBox,
            fileName = map.fileName,
            jsonName = map.jsonName,
            deliveryState = map.state,
            url = map.url,
            path = map.path,
            statusMsg = map.statusMsg,
            progress = map.downloadProgress,
            statusDescr = map.statusDescr,
            isUpdated = map.isUpdated,
            downloadStart = map.downloadStart?.let { OffsetDateTime.ofInstant(it.toInstant(ZoneOffset.UTC), localZone)},
            downloadStop = map.downloadStop?.let { OffsetDateTime.ofInstant(it.toInstant(ZoneOffset.UTC), localZone)},
            downloadDone = map.downloadDone?.let { OffsetDateTime.ofInstant(it.toInstant(ZoneOffset.UTC), localZone)},
            reqDate = OffsetDateTime.ofInstant(map.reqDate.toInstant(ZoneOffset.UTC), localZone),
            flowState = map.flowState,
        )
    }

    private fun <K, V> Map<K, V>.toConcurrentHashMap(): ConcurrentHashMap<K, V> {
        val concurrentHashMap = ConcurrentHashMap<K, V>()
        concurrentHashMap.putAll(this)
        return concurrentHashMap
    }
    companion object {
        var onInventoryUpdatesListener: ((List<String>) -> Unit)? = null
        var onDownloadErrorListener: ((id: String) -> Unit)? = null

//        private val customOrder = listOf(MapDeliveryState.START, MapDeliveryState.DOWNLOAD, MapDeliveryState.CONTINUE)
//
//        private val comparator = compareBy<MapData> {
//            // Get the index of the state in the custom order; default to Int.MAX_VALUE if not found
//            customOrder.indexOf(it.deliveryState).let { index -> if (index == -1) Int.MAX_VALUE else index }
//        }.thenByDescending{ it.id }


        private val comparator = Comparator<MapData> { o1, o2 ->
            o2.reqDate.compareTo(o1.reqDate)
        }

        private val mapMutableLiveHase = MutableLiveData(ConcurrentHashMap<String, MapData>())
        private val mapLiveList: LiveData<List<MapData>> = Transformations.map(mapMutableLiveHase){
            it.values.toList().sortedWith(comparator)
        }
    }

}