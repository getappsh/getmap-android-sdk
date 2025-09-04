package com.ngsoft.getapp.sdk

import GetApp.Client.models.NewBugReportDto
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.annotations.RequiresIMEI
import com.ngsoft.getapp.sdk.exceptions.MapAlreadyExistsException
import com.ngsoft.getapp.sdk.exceptions.QRDataTooLargeException
import com.ngsoft.getapp.sdk.helpers.client.ConfigClient
import com.ngsoft.getapp.sdk.helpers.client.InventoryClient
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.helpers.logger.TimberLogger
import com.ngsoft.getapp.sdk.jobs.DeliveryForegroundService
import com.ngsoft.getapp.sdk.jobs.JobScheduler
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.qr.QRManager
import com.ngsoft.getapp.sdk.utils.DateHelper
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils.getStringOrNull
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.DownloadMetadata
import com.ngsoft.tilescache.models.MapPkg
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.time.format.DateTimeFormatter


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private lateinit var mapRepo: MapRepo
    private lateinit var qrManager: QRManager

    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)

        mapRepo = MapRepo(appCtx)
        qrManager = QRManager(appCtx)

        val isDeliveryServiceRunning = appCtx.isServiceRunning(DeliveryForegroundService::class.java)
        Timber.d("init - delivery service running: $isDeliveryServiceRunning")
        if (!isDeliveryServiceRunning){
            Thread{updateMapsStatusOnStart()}.start()
        }
        JobScheduler().scheduleInventoryOfferingJob(appCtx, config.periodicInventoryIntervalMins)
        JobScheduler().scheduleRemoteConfigJob(appCtx, config.periodicConfIntervalMins)
        return true
    }


    @RequiresIMEI
    override fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem> = requireIMEI{
        return super.getDiscoveryCatalog(inputProperties)
    }

    override fun getDownloadedMap(id: String): MapData? {
        Timber.i("getDownloadedMap - map id: $id")
        return this.mapRepo.getDownloadData(id)
    }

    override fun getDownloadedMaps(): List<MapData> {
        Timber.i("getDownloadedMaps")
        return this.mapRepo.getAllMaps()
    }

    override fun getDownloadedMapsLive(): LiveData<List<MapData>> {
        Timber.i("getDownloadedMapsLive")
        return this.mapRepo.getAllMapsLiveData()
    }
    override fun purgeCache(){
        mapRepo.purge()
    }

    private inline fun <T> requireIMEI(action: () -> T): T {
        pref.checkDeviceIdAvailability()
        return action()
    }

    @RequiresIMEI
    override fun downloadMap(mp: MapProperties): String? = requireIMEI{
        Timber.i("downloadMap")

        this.mapRepo.getByBBox(mp.boundingBox).forEach{
            if (it.isUpdated){
                Timber.e("downloadMap map already exists, abort request", )
                return null
            }
        }

        val id = this.mapRepo.create(
            mp.productId, mp.boundingBox, MapDeliveryState.START,
            appCtx.getString(R.string.delivery_status_req_sent), DeliveryFlowState.START)
        this.mapRepo.invoke(id)

        Timber.i("downloadMap: id: $id")
        Timber.d("downloadMap: bBox - ${mp.boundingBox}")

        if (isEnoughSpace(id)){
            DeliveryForegroundService.startForId(appCtx, id)
        }

        return id
    }

    @RequiresIMEI
    override fun downloadUpdatedMap(id: String): String? = requireIMEI{
        Timber.i("downloadUpdatedMap")
        val mapPkg  = this.mapRepo.getById(id)
        if (mapPkg == null){
            Timber.e("downloadUpdatedMap - old download map id: $id dose not exist")
            return null
        }

        val mp = MapProperties(mapPkg.pId, mapPkg.footprint ?: mapPkg.bBox, false)

        val file = File("${mapPkg.path}/${mapPkg.fileName}")
        this.mapFileManager.getAndValidateStorageDirByPolicy(file.length())
        return this.downloadMap(mp)
    }


    private fun isEnoughSpace(id: String): Boolean{
        Timber.i("isEnoughSpace")

        val obsoletedFileSize: Long? = this.mapRepo.getById(id)?.let { mapPackage ->
            this.mapRepo.getByBBox(mapPackage.bBox, mapPackage.footprint)
                .filter { it.id.toString() != id } // Filter out the current package
                .mapNotNull { pkg ->
                    pkg.path?.let { path ->
                        pkg.fileName?.let { fileName ->
                            File(path, fileName).takeIf { it.exists() }?.length()
                        }
                    }
                }
                .maxOrNull()
                .takeIf { it != 0L } // If maxOrNull is 0, make it null
        }

        val requiredSpace = obsoletedFileSize ?: (config.minAvailableSpaceMB * 1024 * 1024)
        return try {
            this.mapFileManager.getAndValidateStorageDirByPolicy(requiredSpace)
            true
        }catch (io: IOException){
            Timber.e("isEnoughSpace - error: ${io.message}")
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMsg = appCtx.getString(R.string.delivery_status_failed),
                statusDescr = io.message
            )
            false
        }
    }
    override suspend fun synchronizeMapData(){
        Timber.i("synchronizeMapData")
        mapFileManager.synchronizeMapData()
    }


    override fun cancelDownload(id: String) {
        Timber.d("cancelDownload - for id: $id")
        DeliveryForegroundService.cancelForId(appCtx, id)
    }

    override fun deleteMap(id: String){
        Timber.d("deleteMap - id: $id")
        val mapPkg = this.mapRepo.getById(id)
        mapFileManager.deleteMap(mapPkg)

        MapDeliveryClient.sendDeliveryStatus(client, mapRepo, id, pref.deviceId, MapDeliveryState.DELETED)
        this.mapRepo.remove(id)
    }

    override fun resumeDownload(id: String): String{
        Timber.i("resumeDownload for id: $id")
//        TODO all this needs to be as part of delivery manager
        Thread{
            val mapPkg = this.mapRepo.getById(id)

            if (mapPkg == null ||
                !(mapPkg.state == MapDeliveryState.PAUSE ||
                        mapPkg.state == MapDeliveryState.CANCEL||
                        mapPkg.state == MapDeliveryState.ERROR)
            ){
                val errorMsg = "resumeDownload: Unable to resume download map status is: ${mapPkg?.state}"
                Timber.e(errorMsg)
                this.mapRepo.update(id, state = MapDeliveryState.ERROR, statusDescr = errorMsg)
            }

//            TODO set is cancel to false?
            this.mapRepo.update(id,
                state = MapDeliveryState.CONTINUE,
                statusMsg = appCtx.getString(R.string.delivery_status_continue),
                statusDescr = "")

            DeliveryForegroundService.startForId(appCtx, id)
        }.start()

        return id
    }

    override fun generateQrCode(id: String, width: Int, height: Int): Bitmap {
        Timber.i("generateQrCode - for map id: $id")
        val mapPkg = this.mapRepo.getById(id)!!

        if (mapPkg.state != MapDeliveryState.DONE){
            val errorMsg = "generateQrCode - Only when map state is DONE can create QR code, current state: ${mapPkg.state}"
            Timber.e( errorMsg)
            throw Exception(errorMsg)
        }

        if (mapPkg.jsonName == null){
            val errorMsg = "generateQrCode - Data missing, not found jsonName or download url"
            Timber.e( errorMsg)
            throw Exception(errorMsg)
        }
        val file = File(mapPkg.path, mapPkg.jsonName!!)
        val jsonFile = JsonUtils.readJson(file.path)

        val json = JSONObject()

        val reqId = mapPkg.reqId ?: jsonFile.getStringOrNull("reqId")
        val footprint = FootprintUtils.toString(jsonFile.getJSONObject("footprint"))

        json.put("requestedBBox", mapPkg.bBox)
        json.put("reqId", reqId)
        json.put("id", jsonFile.getString("id"))
        json.put("downloadUrl", mapPkg.url)
        json.put("ingestionDate", jsonFile.getString("ingestionDate"))
        json.put("footprint", footprint)
        return try {
            qrManager.generateQrCode(2, json.toString(), width, height)
        }catch (e: QRDataTooLargeException){
            json.remove("footprint")
            qrManager.generateQrCode(2, json.toString(), width, height)
        }
    }

    @RequiresIMEI
    override fun processQrCodeData(data: String): String = requireIMEI{
        Timber.i("processQrCodeData")

        val (version, jsonString) = qrManager.processQrCodeData(data)
        val json = JSONObject(jsonString)

        val reqId = json.getStringOrNull("reqId")
        val pid = json.getString("id")
        val ingestionDate = json.getString("ingestionDate")
        val url = json.getStringOrNull("downloadUrl")

        var bBox = json.getStringOrNull("requestedBBox")
        val footprint = if (version == 1){
            FootprintUtils.toString(json.getJSONObject("footprint"))
        }else {
            json.getStringOrNull("footprint") ?: bBox
        }
        bBox = bBox ?: footprint

        if (bBox == null){
            val errorMsg = "processQrCodeData - footprint and bBox are null"
            Timber.e( errorMsg)
            throw Exception(errorMsg)
        }

        val qrIngDate = DateHelper.parse(ingestionDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        this.mapRepo.getByBBox(bBox, footprint).forEach {
//            TODO put ingestionDate in the DB Table
            val sIngDate = mapFileManager.getJsonString(it.path, it.jsonName)?.getString("ingestionDate") ?: return@forEach
            val dIngDate = DateHelper.parse(sIngDate,  DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: return@forEach
            if(dIngDate >= qrIngDate){
                Timber.e("processQrCodeData - map with the same or grater ingestion date already exist", )
                throw MapAlreadyExistsException(it.id.toString())
            }
        }

        val flowState = when {
            url != null -> DeliveryFlowState.IMPORT_DELIVERY
            reqId == null -> DeliveryFlowState.START
            BuildConfig.USE_MAP_CACHE -> DeliveryFlowState.IMPORT_STATUS
            else -> DeliveryFlowState.IMPORT_CREATE
        }
        val mapPkg = MapPkg(pId = pid, bBox = bBox, footprint=footprint, reqId = reqId, state = MapDeliveryState.CONTINUE,
            url = url, flowState = flowState, statusMsg = appCtx.getString(R.string.delivery_status_continue))

        val id = this.mapRepo.save(mapPkg)

        if (isEnoughSpace(id)){
            Timber.d("processQrCodeData - execute the auth delivery process")
            DeliveryForegroundService.startForId(appCtx, id)
        }

        return id
    }
    
    override fun fetchInventoryUpdates(): List<String> {
        Timber.i("fetchInventoryUpdates")
        return InventoryClient.getDoneMapsToUpdate(client, mapRepo, config, pref.deviceId)
    }

    override fun fetchConfigUpdates() {
        Timber.i("fetchConfigUpdates")
        ConfigClient.fetchUpdates(client, config, pref.deviceId)
    }

    override fun setOnInventoryUpdatesListener(listener: (List<String>) -> Unit) {
        Timber.i("setOnInventoryUpdatesListener")
        MapRepo.onInventoryUpdatesListener = listener
    }

    override fun setOnDownloadErrorListener(listener: (String) -> Unit) {
        Timber.i("setOnDownloadErrorListener")
        MapRepo.onDownloadErrorListener = listener
    }

    @Suppress("DEPRECATION")  // Deprecated for third party Services.
    fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
        return (getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it -> it.service.className == service.name }
    }

    private fun updateMapsStatusOnStart(){
        Timber.d("updateMapsStatusOnStart")
        val mapsData = this.mapRepo.getAll().filter { it.state == MapDeliveryState.START || it.state == MapDeliveryState.CONTINUE || it.state == MapDeliveryState.DOWNLOAD }
        Timber.d("updateMapsStatusOnStart - Found: ${mapsData.size} maps on delivery process")

        for (map in mapsData){
            val id = map.id.toString()
            when(map.flowState){
                DeliveryFlowState.DOWNLOAD -> {
                    val rMap = mapFileManager.refreshMapState(map.copy())

                    if (rMap.flowState <= DeliveryFlowState.IMPORT_DELIVERY) {
                        Timber.d("updateMapsStatusOnStart - Map download failed, set state to pause")
                        this.mapRepo.update(id = id, state = MapDeliveryState.PAUSE, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                            jsonDone = rMap.metadata.jsonDone, mapDone = rMap.metadata.mapDone, statusMsg = appCtx.getString(R.string.delivery_status_paused))
                    }else{
                        Timber.d("updateMapsStatusOnStart - Map download in progress")
                        this.mapRepo.update(id, mapDone = rMap.metadata.mapDone,
                            jsonDone = rMap.metadata.jsonDone, flowState = rMap.flowState)
                        DeliveryForegroundService.startForId(appCtx, id)
                    }
                }
                DeliveryFlowState.MOVE_FILES, DeliveryFlowState.DOWNLOAD_DONE -> {
                    Timber.d("updateMapsStatusOnStart - map id: $id, is on ${map.flowState} continue the flow")
                    DeliveryForegroundService.startForId(appCtx, id)

                }
                else ->{
                    Timber.d("updateMapsStatusOnStart - map id: $id, set state to pause ")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.PAUSE,
                        statusMsg = appCtx.getString(R.string.delivery_status_paused)
                    )
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun sendBugReport(description: String?){
        Timber.d("Send Bug Report")
        val res = client.bugReportApi.bugReportControllerReportNewBug(
            NewBugReportDto(
                deviceId = pref.deviceId,
                agentVersion = BuildConfig.VERSION_NAME,
                description = description ?: "Bug report"
            )
        )
        Timber.d("Report Id: ${res.bugId}")
        val filePath = TimberLogger.getFileTree()?.getFileName(0)
        if (filePath == null){
            Timber.w("sendBugReport - filePath is null")
            return
        }
        client.uploadFile(res.uploadLogsUrl, filePath)
        Timber.d("Bug report sent")
    }
}