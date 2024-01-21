package com.ngsoft.getapp.sdk

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.helpers.ConfigClientHelper
import com.ngsoft.getapp.sdk.helpers.InventoryClientHelper
import com.ngsoft.getapp.sdk.jobs.JobScheduler
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DeliveryStatus
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.qr.QRManager
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.HashUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.DownloadMetadata
import com.ngsoft.tilescache.models.MapPkg
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioSdkGetMapService"

    private val checksumAlgorithm = "sha256"

    private lateinit var mapRepo: MapRepo
    private lateinit var qrManager: QRManager

    companion object {
        private var instanceCount = 0
    }

    private val completionHandler: (Long) -> Unit = {
        Log.d(_tag, "downloadImport - completionHandler: processing download ID=$it completion event...")
    }

    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)


        mapRepo = MapRepo(appCtx)
        qrManager = QRManager(appCtx)

        if (instanceCount == 0){
            Thread{updateMapsStatusOnStart()}.start()
        }
        instanceCount++
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
    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        val id = this.mapRepo.create(
            mp.productId, mp.boundingBox, MapDeliveryState.START,
            appCtx.getString(R.string.delivery_status_req_sent), DeliveryFlowState.START, downloadStatusHandler)
        this.mapRepo.invoke(id)

        Log.i(_tag, "downloadMap: id: $id")
        Log.d(_tag, "downloadMap: bBox - ${mp.boundingBox}")

        if (isEnoughSpace(id, config.storagePath, config.minAvailableSpaceBytes)){
            Thread{executeDeliveryFlow(id)}.start()
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


    private fun isEnoughSpace(id: String, path: String, requiredSpace: Long): Boolean{
        Log.i(_tag, "isEnoughSpace")
        val availableSpace = FileUtils.getAvailableSpace(path)
        if (requiredSpace >= availableSpace){
            Log.e(_tag, "isEnoughSpace - Available Space: $availableSpace is lower then then required: $requiredSpace", )
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

    private fun executeDeliveryFlow(id: String){
        val mapPkg = this.mapRepo.getById(id)
        try{
            val toContinue = when(mapPkg?.flowState){
                DeliveryFlowState.START -> importCreate(id)
                DeliveryFlowState.IMPORT_CREATE -> checkImportStatus(id)
                DeliveryFlowState.IMPORT_STATUS -> importDelivery(id)
                DeliveryFlowState.IMPORT_DELIVERY -> downloadImport(id)
                DeliveryFlowState.DOWNLOAD ->  watchDownloadImport(id)
                DeliveryFlowState.DOWNLOAD_DONE -> moveImportFiles(id)
                DeliveryFlowState.MOVE_FILES -> validateImport(id)
                DeliveryFlowState.DONE -> false
                else -> false
            }
            if (toContinue) {
                executeDeliveryFlow(id)
            }
        }catch (e: IOException){
            var attempt = mapPkg?.metadata?.connectionAttempt ?: 5
            if (attempt < 5){
                Log.e(_tag, "executeDeliveryFlow - IOException try again, attempt: $attempt, Error: ${e.message.toString()}")

                this.mapRepo.update(
                    id = id,
                    statusMessage = appCtx.getString(R.string.delivery_status_connection_issue_try_again),
                    errorContent = e.message.toString(),
                    connectionAttempt = ++attempt
                )
                TimeUnit.SECONDS.sleep(2)
                executeDeliveryFlow(id)
            }else{
                Log.e(_tag, "executeDeliveryFlow - exception:  ${e.message.toString()}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = e.message.toString()
                )
                this.sendDeliveryStatus(id)
            }
        }

    }

    private fun importCreate(id: String): Boolean{
        Log.i(_tag, "importCreate")

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "importCreate - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = appCtx.getString(R.string.delivery_status_canceled))
            this.sendDeliveryStatus(id)
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!
        val mapProperties = MapProperties(productId = mapPkg.pId, boundingBox = mapPkg.bBox, isBest = false)
        val retCreate = createMapImport(mapProperties)

        Log.d(_tag, "importCreate - import request Id: ${retCreate?.importRequestId}")
        when(retCreate?.state){
            MapImportState.START, MapImportState.IN_PROGRESS, MapImportState.DONE,  ->{
                Log.d(_tag,"deliverTile - createMapImport -> OK, state: ${retCreate.state} message: ${retCreate.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.START,
                    flowState = DeliveryFlowState.IMPORT_CREATE,
                    statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress),
                    errorContent = retCreate.statusCode?.messageLog ?: "",
                    downloadProgress = retCreate.progress
                )
                this.sendDeliveryStatus(id)
                return true
            }
            MapImportState.CANCEL -> {
                Log.w(_tag,"getDownloadData - createMapImport -> CANCEL, message: ${retCreate.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.CANCEL,
                    statusMessage = appCtx.getString(R.string.delivery_status_canceled),
                    errorContent = retCreate.statusCode?.messageLog

                )
                this.sendDeliveryStatus(id)
                return false
            }
            else -> {
                Log.e(_tag,"getDownloadData - createMapImport failed: ${retCreate?.state}, error: ${retCreate?.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate?.importRequestId,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = retCreate?.statusCode?.messageLog
                )
                this.sendDeliveryStatus(id)
                return false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatus(id: String): Boolean{
        Log.i(_tag, "checkImportStatue")

        val reqId = this.mapRepo.getReqId(id)!!;
        var timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        var stat : CreateMapImportStatus? = null
        var lastProgress : Int? = null
        do{
            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkImportStatus - timed out")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = "checkImportStatus - timed out"
                )
                this.sendDeliveryStatus(id)
                return false

            }

            TimeUnit.SECONDS.sleep(2)
            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "checkImportStatue: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = appCtx.getString(R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }


            try {
                stat = getCreateMapImportStatus(reqId)
            }catch (e: IOException){
                Log.e(_tag, "checkImportStatue - SocketException, try again. error: ${e.message.toString()}" )
                this.mapRepo.update(
                    id = id,
                    statusMessage = appCtx.getString(R.string.delivery_status_connection_issue_try_again),
                    errorContent = e.message.toString()
                )
                continue
            }

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(_tag,"checkImportStatus - MapImportState -> ERROR, error:  ${stat.statusCode?.messageLog}")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed),
                        errorContent = stat.statusCode?.messageLog
                    )
                    this.sendDeliveryStatus(id)
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState -> CANCEL, message: ${stat.statusCode?.messageLog}")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.CANCEL,
                        statusMessage = appCtx.getString(R.string.delivery_status_canceled),
                        errorContent = stat.statusCode?.messageLog
                    )
                    this.sendDeliveryStatus(id)
                    return false

                }
                MapImportState.IN_PROGRESS -> {
                    Log.w(_tag,"checkImportStatus - MapImportState -> IN_PROGRESS, progress: ${stat.progress}")
                    this.mapRepo.update(
                        id = id,
                        downloadProgress = stat.progress,
                        statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress),
                        errorContent = ""
                    )
                    if (lastProgress != stat.progress){
                        timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes
                    }
                    lastProgress = stat.progress
                }
                else -> {}
            }

        }while (stat == null || stat.state!! != MapImportState.DONE)

        Log.d(_tag, "checkImportStatue: MapImportState.Done")
        this.mapRepo.update(
            id = id,
            downloadProgress = 100,
            flowState = DeliveryFlowState.IMPORT_STATUS,
            errorContent = "")
        return true
    }

    @OptIn(ExperimentalTime::class)
    private fun importDelivery(id: String): Boolean{
        Log.i(_tag, "importDelivery")

        val reqId = this.mapRepo.getReqId(id)!!;

        var retDelivery = setMapImportDeliveryStart(reqId)
        val timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        while (retDelivery?.state != MapDeliveryState.DONE) {
            when (retDelivery?.state) {
                MapDeliveryState.DONE, MapDeliveryState.START, MapDeliveryState.DOWNLOAD, MapDeliveryState.CONTINUE -> {
                    this.mapRepo.update(id = id, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                        statusMessage = appCtx.getString(R.string.delivery_status_prepare_to_download), errorContent = "")

                }

                MapDeliveryState.CANCEL, MapDeliveryState.PAUSE -> {
                    Log.w(_tag, "importDelivery - setMapImportDeliveryStart => CANCEL")
                    this.mapRepo.update(id = id, state = MapDeliveryState.CANCEL, statusMessage = appCtx.getString(R.string.delivery_status_canceled))
                    this.sendDeliveryStatus(id)
                    return false
                }
                else -> {
                    Log.e(_tag, "importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                    this.mapRepo.update(id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed),
                        errorContent = "importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}"
                    )
                    this.sendDeliveryStatus(id)
                    return false
                }
            }
            if(timeoutTime.hasPassedNow()){
                Log.e(_tag,"importDelivery - timed out")
                this.mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = appCtx.getString(R.string.delivery_status_failed), errorContent = "ImportDelivery- timed out")
                this.sendDeliveryStatus(id)
                return false
            }

            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "importDelivery: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = appCtx.getString(R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }
            TimeUnit.SECONDS.sleep(2)
            retDelivery = getMapImportDeliveryStatus(reqId)
        }

        if (retDelivery.url == null){
            Log.e(_tag, "importDelivery- download url is null", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                errorContent = "importDelivery - download url is null"
            )
            this.sendDeliveryStatus(id)
            return false

        }

        Log.d(_tag, "importDelivery - delivery is ready, download url: ${retDelivery.url} ")
        this.mapRepo.update(id = id, url = retDelivery.url, flowState = DeliveryFlowState.IMPORT_DELIVERY, errorContent = "")
        return true
    }
    private fun downloadImport(id: String): Boolean{
        Log.i(_tag, "downloadImport")

        val mapPkg = this.mapRepo.getById(id)!!
        val pkgUrl = mapPkg.url!!
        val jsonUrl = FileUtils.changeFileExtensionToJson(pkgUrl)

        val jsonDownloadId = if(!mapPkg.metadata.jsonDone) downloadFile(id, jsonUrl, true) else null
        val pkgDownloadId = if(!mapPkg.metadata.mapDone) downloadFile(id, pkgUrl, false) else null
        Log.d(_tag, "downloadImport - jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")

        val statusMessage = if(mapPkg.metadata.validationAttempt <= 0) appCtx.getString(R.string.delivery_status_download) else appCtx.getString(R.string.delivery_status_failed_verification_try_again)
        this.mapRepo.update(
            id = id, JDID = jsonDownloadId, MDID = pkgDownloadId,
            state =  MapDeliveryState.DOWNLOAD, flowState = DeliveryFlowState.DOWNLOAD,
            statusMessage = statusMessage, errorContent = "", downloadProgress = 0
        )
        this.sendDeliveryStatus(id)

        return false
    }

    private fun downloadFile(id: String, url: String, isJson: Boolean): Long {
        Log.i(_tag, "downloadFile")
        val downloadId = downloader.downloadFile(url, completionHandler);
        watchDownloadProgress(downloadId, id, url, isJson)
        return downloadId
    }

    private fun watchDownloadImport(id: String): Boolean{
        Log.i(_tag, "watchDownloadImport")
        val mapPkg = this.mapRepo.getById(id) ?: return false

        val pkgUrl = mapPkg.url ?: return false
        val jsonUrl = FileUtils.changeFileExtensionToJson(pkgUrl)

        mapPkg.MDID?.let {
            watchDownloadProgress(it, id, pkgUrl, false)
        }
        mapPkg.JDID?.let{
            watchDownloadProgress(it, id, jsonUrl, true)
        }
        return false
    }
    private fun watchDownloadProgress(downloadId: Long, id: String, url: String, isJson: Boolean): Long{
        Log.i(_tag, "watchDownloadProgress, isJson: $isJson")
        timer(initialDelay = 100, period = 2000) {
//            todo heck what happen when cancel the download in the download manager
            val mapPkg = mapRepo.getById(id) ?: return@timer
            if (mapPkg.cancelDownload){
                Log.d(_tag, "downloadFile - Download $id, canceled by user")
                downloader.cancelDownload(downloadId)
                if (!isJson){
                    mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMessage = appCtx.getString(R.string.delivery_status_canceled),)
                }
                this.cancel()
                return@timer
            }

            val statusInfo = downloader.queryStatus(downloadId)
            when(statusInfo?.status){
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                    mapRepo.update(id = id, errorContent = statusInfo.reason)
                }
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_SUCCESSFUL -> {
                    val progress = (statusInfo.downloadBytes * 100 / statusInfo.totalBytes).toInt()
                    Log.d(_tag, "downloadFile - DownloadId: $downloadId -> process: $progress ")

                    if (!isJson && !mapPkg.metadata.mapDone){
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.DOWNLOAD,
                            downloadProgress = progress,
                            fileName = statusInfo.fileName,
                            statusMessage = appCtx.getString(R.string.delivery_status_download),
                            errorContent = ""
                        )
                        sendDeliveryStatus(id)
                    }

                    if (progress >= 100 || statusInfo.status == DownloadManager.STATUS_SUCCESSFUL){
                        val updatedMapPkg  = if (isJson){
                             mapRepo.updateAndReturn(id, jsonDone = true, jsonName = statusInfo.fileName)
                        }else{
                            mapRepo.updateAndReturn(id, mapDone = true)
                        }

                        if (updatedMapPkg?.metadata?.mapDone == true && updatedMapPkg.metadata.jsonDone){
                            Log.d(_tag, "downloadFile - downloading Done")
                            mapRepo.update(
                                id = id,
                                flowState = DeliveryFlowState.DOWNLOAD_DONE,
                                downloadProgress = 100,
                                errorContent = "")
                            sendDeliveryStatus(id)
                            Thread{executeDeliveryFlow(id)}.start()
                        }
                        this.cancel()
                    }
                }
                else -> {
                    Log.e(_tag, "downloadFile -  DownloadManager failed to download file. id: $downloadId, reason: ${statusInfo?.reason}")

                    val downloadAttempts = if (isJson) mapPkg.metadata.jsonAttempt else mapPkg.metadata.mapAttempt

                    if (statusInfo?.status == DownloadManager.STATUS_FAILED && (statusInfo.reasonCode == 403 || statusInfo.reasonCode == 404)){
                        Log.e(_tag, "watchDownloadProgress - download status is ${statusInfo.reasonCode}, set as obsolete")
                        mapRepo.setMapUpdated(id, false)
                        mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = appCtx.getString(R.string.delivery_status_failed), errorContent = statusInfo.reason)

                    }else if (downloadAttempts < config.downloadRetry) {
                        Log.d(_tag, "downloadFile - retry download")
                        downloader.cancelDownload(downloadId)

                        mapRepo.update(id, statusMessage = appCtx.getString(R.string.delivery_status_failed_verification_try_again),
                            errorContent = statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file")
                        handelDownloadRetry(id, url, isJson, downloadAttempts)
                    }else{
                        mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = appCtx.getString(R.string.delivery_status_failed),
                            errorContent = statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file"
                        )
                    }
                    sendDeliveryStatus(id)
                    this.cancel()
                }
            }
        }
        return downloadId
    }

    private fun handelDownloadRetry(id: String, url: String, isJson: Boolean, downloadAttempts: Int) {
        Log.i(_tag, "handelDownloadRetry, id: $id, isJson: $isJson ")
        val waitTime = TimeUnit.MINUTES.toMillis(if (downloadAttempts == 1) 5 else 10)
        val startTime = System.currentTimeMillis()

        Log.d(_tag, "handelDownloadRetry try again in: ${TimeUnit.MILLISECONDS.toMinutes(waitTime)} minutes")
        timer(initialDelay = 100, period = 2000) {
            val diff = System.currentTimeMillis() - startTime
            if (diff > waitTime){
                Log.d(_tag, "handelDownloadRetry - try again id: $id, isJson: $isJson")
                this.cancel()
                val mapPkg = mapRepo.getById(id) ?: return@timer

                val statusMessage = appCtx.getString(R.string.delivery_status_failed_try_again)

                if (isJson) {
                    val downloadId = downloadFile(id, url, true)
                    val attempts = ++mapPkg.metadata.jsonAttempt
                    mapRepo.update(id, JDID = downloadId, jsonAttempt = attempts, statusMessage = statusMessage)
                } else {
                    val downloadId = downloadFile(id, url, false)
                    val attempts = ++mapPkg.metadata.mapAttempt
                    mapRepo.update(id, MDID = downloadId, mapAttempt = attempts, statusMessage = statusMessage)
                }

            }else {
                if (mapRepo.isDownloadCanceled(id)) {
                    Log.d(_tag, "handelDownloadRetry - Download $id, canceled by user")
                    if (!isJson) {
                        mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMessage = appCtx.getString(R.string.delivery_status_canceled))
                        this.cancel()
                    }
                }
                if (!isJson) {
                    val secToFinish = TimeUnit.MILLISECONDS.toSeconds(waitTime - diff)
                    mapRepo.update(id, statusMessage = appCtx.getString(R.string.delivery_status_failed_try_again_in, secToFinish))
                }
            }
        }
    }
    private fun moveImportFiles(id: String): Boolean{
        Log.i(_tag, "moveImportFiles - id: $id")

        val mapPkg = this.mapRepo.getById(id)!!

        return try {
            Log.d(_tag, "moveImportFiles - fileName ${mapPkg.fileName} jsonName: ${mapPkg.jsonName}")
            val fileName = mapFileManager.moveFileToTargetDir(mapPkg.fileName!!)
            val jsonName = mapFileManager.moveFileToTargetDir(mapPkg.jsonName!!)
            this.mapRepo.update(id, flowState = DeliveryFlowState.MOVE_FILES, fileName = fileName, jsonName = jsonName)
            true
        }catch (e: Exception){
            Log.e(_tag, "moveImportFiles - move file failed: ${e.message.toString()}", )
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                errorContent = e.message.toString()
            )
            sendDeliveryStatus(id)
            false
        }

    }

    private fun validateImport(id: String): Boolean{
        Log.i(_tag, "validateImport - id: $id")
        this.mapRepo.update(
            id = id,
            statusMessage = appCtx.getString(R.string.delivery_status_in_verification),
            downloadProgress = 0, errorContent = ""
        )

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "validateImport - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = appCtx.getString(R.string.delivery_status_canceled))
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!

        val isValid = try{
            Log.d(_tag, "validateImport - fileName ${mapPkg.fileName}, jsonName ${mapPkg.jsonName}")
            val mapFile = File(config.storagePath, mapPkg.fileName!!)
            val jsonFile = File(config.storagePath, mapPkg.jsonName!!)

            val expectedHash = JsonUtils.getStringOrThrow(checksumAlgorithm, jsonFile.path)
            val actualHash = HashUtils.getCheckSumFromFile(checksumAlgorithm, mapFile) {
                Log.d(_tag, "validateImport - progress: $it")
                this.mapRepo.update(id, downloadProgress = it, statusMessage = appCtx.getString(R.string.delivery_status_in_verification), errorContent = "")
            }
            Log.d(_tag, "validateImport - expectedHash: $expectedHash, actualHash: $actualHash")

            val isValid = expectedHash == actualHash
            Log.d(_tag, "validateImport - validation result for id: $id is: $isValid")

            isValid

        }catch (e: Exception){
            Log.e(_tag, "validateImport - Failed to validate map, error: ${e.message.toString()} ")
            true
        }
        if (isValid){
            this.findAndRemoveDuplicates(id)
            this.mapRepo.update(
                id = id,
                state =  MapDeliveryState.DONE,
                flowState = DeliveryFlowState.DONE,
                statusMessage = appCtx.getString(R.string.delivery_status_done),
                errorContent = ""
            )
        }else{
            if (mapPkg.metadata.validationAttempt < 1){
                mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)

                this.mapRepo.update(
                    id = id,
                    flowState = DeliveryFlowState.IMPORT_DELIVERY,
                    validationAttempt = ++mapPkg.metadata.validationAttempt,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed_verification_try_again),
                    errorContent = "Checksum validation Failed try downloading again",
                )
                Log.d(_tag, "validateImport - Failed downloading again")
                return true
            }
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed_verification),
                errorContent = "Checksum validation Failed"
            )
        }
        this.sendDeliveryStatus(id)

        return isValid
    }
    private fun findAndRemoveDuplicates(id: String){
        Log.i(_tag, "findAndRemoveDuplicate")
        val mapPkg = this.mapRepo.getById(id) ?: return
        val duplicates = this.mapRepo.getByBBox(mapPkg.bBox).toMutableList()
        duplicates.removeIf { it.id.toString() == id }

        Log.d(_tag, "findAndRemoveDuplicate - found ${duplicates.size} duplicates")
        duplicates.forEach {
            Log.d(_tag, "findAndRemoveDuplicate - remove: ${it.id}")
            try {
                this.deleteMap(it.id.toString())
            }catch (error: Exception){
                Log.e(_tag, "findAndRemoveDuplicates - failed to delete the map, error: ${error.message.toString()}", )
            }
        }
    }
    private fun sendDeliveryStatus(id: String, state: MapDeliveryState?=null) {
        val mapPkg = this.mapRepo.getById(id) ?: return
        val deliveryStatus = DeliveryStatus(
            state = state ?: mapPkg.state,
            reqId = mapPkg.reqId ?: "-1",
            progress = mapPkg.downloadProgress,
            start = mapPkg.downloadStart?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            stop = mapPkg.downloadStop?.let { OffsetDateTime.of(it, ZoneOffset.UTC) },
            done = mapPkg.downloadDone?.let { OffsetDateTime.of(it, ZoneOffset.UTC) }
        )

        Log.d(_tag, "sendDeliveryStatus: id: $id, state: ${deliveryStatus.state}, request id: ${deliveryStatus.reqId}")

        pushDeliveryStatus(deliveryStatus)
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
                    Thread{executeDeliveryFlow(id)}.start()
                }
            }
        }
    }

    override fun cancelDownload(id: String) {
        Log.d(_tag, "cancelDownload - for id: $id")
        Thread{
            try{
                this.mapRepo.setCancelDownload(id)
            }catch (e: Exception){
                Log.e(_tag, "cancelDownload - failed to candle, error: $e", )
            }
        }.start()
    }

    override fun deleteMap(id: String){
        Log.d(_tag, "deleteMap - id: $id")
        val mapPkg = this.mapRepo.getById(id)

        if (mapPkg == null ||
            mapPkg.state == MapDeliveryState.START ||
            mapPkg.state == MapDeliveryState.DOWNLOAD ||
            mapPkg.state == MapDeliveryState.CONTINUE){

            val errorMsg = "deleteMap: Unable to delete map when status is: ${mapPkg?.state}"
            Log.e(_tag,  errorMsg)
            throw Exception(errorMsg)
        }

        mapPkg.JDID?.let { downloader.cancelDownload(it) }
        mapPkg.MDID?.let { downloader.cancelDownload(it) }

        mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)
//        TODO send status after removed from the DB
        this.sendDeliveryStatus(id, MapDeliveryState.DELETED)

        this.mapRepo.remove(id)
    }

    override fun resumeDownload(id: String, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        Log.i(_tag, "resumeDownload for id: $id")
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

            executeDeliveryFlow(id)
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

        return qrManager.generateQrCode(json.toString(), width, height)
    }

    override fun processQrCodeData(data: String, downloadStatusHandler: (MapDownloadData) -> Unit): String{
        Log.i(_tag, "processQrCodeData")

        val jsonString = qrManager.processQrCodeData(data)
        val json = JSONObject(jsonString)

        val url = json.getString("downloadUrl")
        val reqId = json.getString("reqId")

        Log.d(_tag, "processQrCodeData - download url: $url, reqId: $reqId")
        var jsonName = FileUtils.changeFileExtensionToJson(FileUtils.getFileNameFromUri(url))
        jsonName = FileUtils.writeFile(config.downloadPath, jsonName, jsonString)
        Log.d(_tag, "processQrCodeData - fileName: $jsonName")

        val mapPkg = MapPkg(
                pId = json.getString("id"),
                bBox = json.getString("productBoundingBox"),
                reqId = reqId,
                jsonName = jsonName,
                url = url,
                metadata = DownloadMetadata(jsonDone = true),
                state = MapDeliveryState.CONTINUE,
                flowState = DeliveryFlowState.IMPORT_DELIVERY,
                statusMessage = appCtx.getString(R.string.delivery_status_continue))


        val id = this.mapRepo.save(mapPkg)
        this.mapRepo.setListener(id, downloadStatusHandler)
        this.mapRepo.invoke(id)

        if (isEnoughSpace(id, config.storagePath, config.minAvailableSpaceBytes)){
            Log.d(_tag, "processQrCodeData - execute the auth delivery process")
            Thread{executeDeliveryFlow(id)}.start()
        }

        return id
    }
    
    override fun fetchInventoryUpdates(): List<String> {
        Log.i(_tag, "fetchInventoryUpdates")
        return InventoryClientHelper.getUpdates(config, mapRepo, client, pref.deviceId)
    }

    override fun fetchConfigUpdates() {
        Log.i(_tag, "fetchConfigUpdates")
        ConfigClientHelper.fetchUpdates(config, client, pref.deviceId)
    }

    override fun setOnInventoryUpdatesListener(listener: (List<String>) -> Unit) {
        Log.i(_tag, "setOnInventoryUpdatesListener")
        MapRepo.onInventoryUpdatesListener = listener
    }
    override fun registerDownloadHandler(id: String, downloadStatusHandler: (MapDownloadData) -> Unit) {
        Log.i(_tag, "registerDownloadHandler, downloadId: $id")
        this.mapRepo.setListener(id, downloadStatusHandler)
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
                        Thread{executeDeliveryFlow(id)}.start()
                    }

                }
                DeliveryFlowState.MOVE_FILES, DeliveryFlowState.DOWNLOAD_DONE -> {
                    Log.d(_tag, "updateMapsStatusOnStart - map id: $id, is on ${map.flowState} continue the flow")
                    Thread{ executeDeliveryFlow(id)}.start()
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