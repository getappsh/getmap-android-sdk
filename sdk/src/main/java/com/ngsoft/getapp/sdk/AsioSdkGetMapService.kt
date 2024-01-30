package com.ngsoft.getapp.sdk

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.helpers.client.ConfigClient
import com.ngsoft.getapp.sdk.helpers.client.InventoryClient
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.jobs.DeliveryForegroundService
import com.ngsoft.getapp.sdk.jobs.JobScheduler
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.qr.QRManager
import com.ngsoft.getapp.sdk.utils.DateHelper
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.DownloadMetadata
import com.ngsoft.tilescache.models.MapPkg
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.time.format.DateTimeFormatter


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioSdkGetMapService"

    private lateinit var mapRepo: MapRepo
    private lateinit var qrManager: QRManager

    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)

        mapRepo = MapRepo(appCtx)
        qrManager = QRManager(appCtx)

        val isDeliveryServiceRunning = appCtx.isServiceRunning(DeliveryForegroundService::class.java)
        Log.d(_tag, "init - delivery service running: $isDeliveryServiceRunning")
        if (!isDeliveryServiceRunning){
            Thread{updateMapsStatusOnStart()}.start()
        }
        JobScheduler().scheduleInventoryOfferingJob(appCtx, config.periodicInventoryIntervalMins)
        JobScheduler().scheduleRemoteConfigJob(appCtx, config.periodicConfIntervalMins)
        return true
    }

    override fun getDownloadedMap(id: String): MapDownloadData? {
        Log.i(_tag, "getDownloadedMap - map id: $id")
        return this.mapRepo.getDownloadData(id)
    }

    override fun getDownloadedMaps(): List<MapDownloadData> {
        Log.i(_tag, "getDownloadedMaps")
        return this.mapRepo.getAllMaps()
    }

    override fun getDownloadedMapsLive(): LiveData<List<MapDownloadData>> {
        Log.i(_tag, "getDownloadedMapsLive")
        return this.mapRepo.getAllMapsLiveData()
    }
    override fun purgeCache(){
        mapRepo.purge()
    }
    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String?{
        Log.i(_tag, "downloadMap")

        this.mapRepo.getByBBox(mp.boundingBox).forEach{
            if (it.isUpdated){
                Log.e(_tag, "downloadMap map is already exit, abort request", )
                return null
            }
        }

        val id = this.mapRepo.create(
            mp.productId, mp.boundingBox, MapDeliveryState.START,
            appCtx.getString(R.string.delivery_status_req_sent), DeliveryFlowState.START, downloadStatusHandler)
        this.mapRepo.invoke(id)

        Log.i(_tag, "downloadMap: id: $id")
        Log.d(_tag, "downloadMap: bBox - ${mp.boundingBox}")

        if (isEnoughSpace(id, config.storagePath, config.minAvailableSpaceMB)){
            DeliveryForegroundService.startForId(appCtx, id)
        }

        return id
    }
    
    override fun downloadUpdatedMap(id: String, downloadStatusHandler: (MapDownloadData) -> Unit): String?{
        Log.i(_tag, "downloadUpdatedMap")
        val mapPkg  = this.mapRepo.getById(id)
        if (mapPkg == null){
            Log.e(_tag, "downloadUpdatedMap - old download map id: $id dose not exist")
            return null
        }

        val mp = MapProperties(mapPkg.pId, mapPkg.bBox, false)

        return this.downloadMap(mp, downloadStatusHandler)
    }


    private fun isEnoughSpace(id: String, path: String, requiredSpaceMB: Long): Boolean{
        Log.i(_tag, "isEnoughSpace")
        val availableSpace = FileUtils.getAvailableSpace(path)
        if ((requiredSpaceMB * 1024 * 1024) >= availableSpace){
            Log.e(_tag, "isEnoughSpace - Available Space: $availableSpace is lower then then required: $requiredSpaceMB", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                errorContent = appCtx.getString(R.string.error_not_enough_space)
            )
            return false
        }
        return true
    }
    override fun synchronizeMapData(){
        Log.i(_tag, "synchronizeMapData")
        syncDatabase ()
        syncStorage()
    }

    private fun syncDatabase (){
        Log.i(_tag, "syncDatabase")
        val mapsData = this.mapRepo.getAll().filter { it.state == MapDeliveryState.DONE ||
                    it.state == MapDeliveryState.ERROR || it.state == MapDeliveryState.CANCEL ||
                    it.state == MapDeliveryState.PAUSE || it.state == MapDeliveryState.DELETED ||
                    it.state == MapDeliveryState.DOWNLOAD}

        for (map in mapsData) {
            Log.d(_tag, "syncDatabase  - map id: ${map.id}, state: ${map.state}")

            if (map.state == MapDeliveryState.DELETED) {
                mapFileManager.deleteMapFiles(map.fileName, map.jsonName)
                continue
            }

            val rMap = mapFileManager.refreshMapState(map.copy())
            if(map.state == MapDeliveryState.DOWNLOAD || map.state == MapDeliveryState.CONTINUE){
                if ((rMap.metadata.mapDone ||!downloader.isDownloadFailed(map.MDID)) &&
                    (rMap.metadata.jsonDone || !downloader.isDownloadFailed(map.JDID))){
                    continue
                }
            }

            this.mapRepo.update(map.id.toString(), state = rMap.state, flowState = rMap.flowState, errorContent = rMap.errorContent,
                statusMessage = rMap.statusMessage, mapDone = rMap.metadata.mapDone, jsonDone = rMap.metadata.jsonDone)
        }
    }

    private fun syncStorage(){
        Log.i(_tag, "syncStorage")
        val dir =  File(config.storagePath)
        val mapFiles = dir.listFiles { _, name -> name.endsWith(FileUtils.MAP_EXTENSION) }
        val jsonFiles = dir.listFiles { _, name -> name.endsWith(FileUtils.JSON_EXTENSION) }
        val journalFiles = dir.listFiles { _, name -> name.endsWith(FileUtils.JOURNAL_EXTENSION) }

//        delete map file when there is no corresponding json file and no record in the DB
        mapFiles?.forEach { file ->
            val correspondingJsonFile = File(FileUtils.changeFileExtensionToJson(file.absolutePath))
            if (!this.mapRepo.doesMapFileExist(file.name) && !correspondingJsonFile.exists()) {
                Log.d(_tag, "syncStorage - Not found corresponding json file for mapFile: ${file.name}, delete it.")
                file.delete()
            }
        }
//        delete journal file when there is no corresponding map file
        journalFiles?.forEach { file ->
            val correspondingMapFile = File(FileUtils.changeFileExtensionToMap(file.absolutePath))
            if (!correspondingMapFile.exists()) {
                Log.d(_tag, "syncStorage - Not found corresponding map file for journalFile: ${file.name}, delete it.")
                file.delete()
            }
        }

        jsonFiles?.forEach { file ->
            if (!this.mapRepo.doesJsonFileExist(file.name)) {
                Log.d(_tag, "syncStorage - found json file not in the inventory, fileName: ${file.name}. insert it.")

                val pId: String; val bBox: String; val url: String?;
                try{
                    val json = JsonUtils.readJson(file.path)
                    pId = json.getString("id")
                    bBox = json.getString("productBoundingBox")
                    url =  if (json.has("downloadUrl")) json.getString("downloadUrl") else null
                }catch (e: JSONException){
                    Log.e(_tag, "syncStorage - not valid json object: ${e.message.toString()}")
                    Log.d(_tag, "syncStorage - delete json file: ${file.name}")
                    file.delete()
                    return@forEach
                }

                val mapPkg = mapFileManager.refreshMapState(MapPkg(
                    pId = pId,
                    bBox = bBox,
                    state = MapDeliveryState.ERROR,
                    flowState = DeliveryFlowState.MOVE_FILES,
                    url = url,
                    fileName = FileUtils.changeFileExtensionToMap(file.name),
                    jsonName = file.name,
                    statusMessage = appCtx.getString(R.string.delivery_status_in_verification)
                ))

                val id = this.mapRepo.save(mapPkg)
                Log.d(_tag, "syncStorage - new map id: $id, deliveryFlowState: ${mapPkg.flowState} ")
                if (mapPkg.flowState == DeliveryFlowState.MOVE_FILES){
                    Log.i(_tag, "syncStorage - execute delivery flow for map id: $id")
                    DeliveryForegroundService.startForId(appCtx, id)
                }
            }
        }
    }

    override fun cancelDownload(id: String) {
        Log.d(_tag, "cancelDownload - for id: $id")
        DeliveryForegroundService.cancelForId(appCtx, id)
    }

    override fun deleteMap(id: String){
        Log.d(_tag, "deleteMap - id: $id")
        val mapPkg = this.mapRepo.getById(id)
        mapFileManager.deleteMap(mapPkg)

        MapDeliveryClient.sendDeliveryStatus(client, mapRepo, id, pref.deviceId, MapDeliveryState.DELETED)
        this.mapRepo.remove(id)
    }

    override fun resumeDownload(id: String, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        Log.i(_tag, "resumeDownload for id: $id")
//        TODO all this needs to be as part of delivery manager
        Thread{
            val mapPkg = this.mapRepo.getById(id)
            this.mapRepo.setListener(id, downloadStatusHandler)

            if (mapPkg == null ||
                !(mapPkg.state == MapDeliveryState.PAUSE ||
                        mapPkg.state == MapDeliveryState.CANCEL||
                        mapPkg.state == MapDeliveryState.ERROR)
            ){
                val errorMsg = "deleteMap: Unable to resume download map status is: ${mapPkg?.state}"
                Log.e(_tag, errorMsg)
                this.mapRepo.update(id, state = MapDeliveryState.ERROR, errorContent = errorMsg)
            }

            this.mapRepo.update(id,
                state = MapDeliveryState.CONTINUE,
                statusMessage = appCtx.getString(R.string.delivery_status_continue),
                errorContent = "")

            DeliveryForegroundService.startForId(appCtx, id)
        }.start()

        return id
    }

    override fun generateQrCode(id: String, width: Int, height: Int): Bitmap {
        Log.i(_tag, "generateQrCode - for map id: $id")
        val mapPkg = this.mapRepo.getById(id)!!

        if (mapPkg.state != MapDeliveryState.DONE){
            val errorMsg = "generateQrCode - Only when map state is DONE can create QR code, current state: ${mapPkg.state}"
            Log.e(_tag,  errorMsg)
            throw Exception(errorMsg)
        }

        if (mapPkg.jsonName == null || mapPkg.url == null){
            val errorMsg = "generateQrCode - Missing data, not found jsonName or download url"
            Log.e(_tag,  errorMsg)
            throw Exception(errorMsg)
        }
        val file = File(config.storagePath, mapPkg.jsonName!!)
        val json = JsonUtils.readJson(file.path)

        Log.d(_tag, "generateQrCode - append download url to json")
        json.put("downloadUrl", mapPkg.url)
        json.put("reqId", mapPkg.reqId)
        json.put("requestedBBox", mapPkg.bBox)

        return qrManager.generateQrCode(json.toString(), width, height)
    }

    override fun processQrCodeData(data: String, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        Log.i(_tag, "processQrCodeData")

        val jsonString = qrManager.processQrCodeData(data)
        val json = JSONObject(jsonString)

        val url = json.getString("downloadUrl")
        val reqId = json.getString("reqId")
        val bBox = json.getString("requestedBBox")
        val pid = json.getString("id")
        val ingestionDate = json.getString("ingestionDate")

        val qrIngDate = DateHelper.parse(ingestionDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        this.mapRepo.getByBBox(bBox).forEach {
            val sIngDate = mapFileManager.getJsonString(it.jsonName)?.getString("ingestionDate") ?: return@forEach
            val dIngDate = DateHelper.parse(sIngDate,  DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: return@forEach
            if(dIngDate >= qrIngDate){
                Log.e(_tag, "processQrCodeData - map with the same or grater ingestion date already exist", )
                throw Exception(appCtx.getString(R.string.error_qr_map_already_exists))
            }
        }


        Log.d(_tag, "processQrCodeData - download url: $url, reqId: $reqId")
        var jsonName = FileUtils.changeFileExtensionToJson(FileUtils.getFileNameFromUri(url))
        jsonName = FileUtils.writeFile(config.downloadPath, jsonName, jsonString)
        Log.d(_tag, "processQrCodeData - fileName: $jsonName")

        val mapPkg = MapPkg(pId = pid, bBox = bBox, reqId = reqId, jsonName = jsonName, url = url,
            metadata = DownloadMetadata(jsonDone = true), state = MapDeliveryState.CONTINUE,
            flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMessage = appCtx.getString(R.string.delivery_status_continue))


        val id = this.mapRepo.save(mapPkg)
        this.mapRepo.setListener(id, downloadStatusHandler)
        this.mapRepo.invoke(id)

        if (isEnoughSpace(id, config.storagePath, config.minAvailableSpaceMB)){
            Log.d(_tag, "processQrCodeData - execute the auth delivery process")
            DeliveryForegroundService.startForId(appCtx, id)
        }

        return id
    }
    
    override fun fetchInventoryUpdates(): List<String> {
        Log.i(_tag, "fetchInventoryUpdates")
        return InventoryClient.getDoneMapsToUpdate(client, mapRepo, config, pref.deviceId)
    }

    override fun fetchConfigUpdates() {
        Log.i(_tag, "fetchConfigUpdates")
        ConfigClient.fetchUpdates(client, config, pref.deviceId)
    }

    override fun setOnInventoryUpdatesListener(listener: (List<String>) -> Unit) {
        Log.i(_tag, "setOnInventoryUpdatesListener")
        MapRepo.onInventoryUpdatesListener = listener
    }
    override fun registerDownloadHandler(id: String, downloadStatusHandler: (MapDownloadData) -> Unit) {
        Log.i(_tag, "registerDownloadHandler, downloadId: $id")
        this.mapRepo.setListener(id, downloadStatusHandler)
    }

    @Suppress("DEPRECATION")  // Deprecated for third party Services.
    fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it -> it.service.className == service.name }
    }

    private fun updateMapsStatusOnStart(){
        Log.d(_tag, "updateMapsStatusOnStart")
        val mapsData = this.mapRepo.getAll().filter { it.state == MapDeliveryState.START || it.state == MapDeliveryState.CONTINUE || it.state == MapDeliveryState.DOWNLOAD }
        Log.d(_tag, "updateMapsStatusOnStart - Found: ${mapsData.size} maps on delivery process")

        for (map in mapsData){
            val id = map.id.toString()
            when(map.flowState){
                DeliveryFlowState.DOWNLOAD -> {
                    val rMap = mapFileManager.refreshMapState(map.copy())

                    if (rMap.flowState <= DeliveryFlowState.IMPORT_DELIVERY) {
                        Log.d(_tag, "updateMapsStatusOnStart - Map download failed, set state to pause")
                        this.mapRepo.update(id = id, state = MapDeliveryState.PAUSE, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                            jsonDone = rMap.metadata.jsonDone, mapDone = rMap.metadata.mapDone, statusMessage = appCtx.getString(R.string.delivery_status_paused))
                    }else{
                        Log.d(_tag, "updateMapsStatusOnStart - Map download in progress")
                        this.mapRepo.update(id, mapDone = rMap.metadata.mapDone,
                            jsonDone = rMap.metadata.jsonDone, flowState = rMap.flowState)
                        DeliveryForegroundService.startForId(appCtx, id)
                    }
                }
                DeliveryFlowState.MOVE_FILES, DeliveryFlowState.DOWNLOAD_DONE -> {
                    Log.d(_tag, "updateMapsStatusOnStart - map id: $id, is on ${map.flowState} continue the flow")
                    DeliveryForegroundService.startForId(appCtx, id)

                }
                else ->{
                    Log.d(_tag, "updateMapsStatusOnStart - map id: $id, set state to pause ")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.PAUSE,
                        statusMessage = appCtx.getString(R.string.delivery_status_paused)
                    )
                }
            }
        }
    }
}