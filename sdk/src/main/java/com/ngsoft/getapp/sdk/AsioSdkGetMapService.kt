package com.ngsoft.getapp.sdk

import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.PrepareDeliveryResDto
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.utils.HashUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


internal class AsioSdkGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioSdkGetMapService"

    private var deliveryTimeoutMinutes: Int = 5
    private var downloadTimeoutMinutes: Int = 5
    private var downloadRetryAttempts: Int = 2

    private val checksumAlgorithm = "sha256"

    private lateinit var storagePath: String

    private lateinit var mapRepo: MapRepo

    private val completionHandler: (Long) -> Unit = {
        Log.d(_tag, "downloadImport - completionHandler: processing download ID=$it completion event...")
    }

    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        deliveryTimeoutMinutes = configuration.deliveryTimeout
        downloadTimeoutMinutes = configuration.downloadTimeout
        downloadRetryAttempts = configuration.downloadRetry

        storagePath = configuration.storagePath

        mapRepo = MapRepo(appCtx)
        Thread{updateMapsStatusOnStart()}.start()
        return true
    }

    override fun getDownloadedMap(id: String): MapDownloadData? {
        Log.i(_tag, "getDownloadedMap - map id: $id")
        return this.mapRepo.getDownloadData(id)
    }

    override fun getDownloadedMaps(): List<MapDownloadData> {
        Log.i(_tag, "getDownloadedMaps")
        return this.mapRepo.getAllMapsDownloadData()
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

        Thread{
            try {
                executeDeliveryFlow(id)
            } catch (e: Exception) {
                Log.e(_tag, "downloadMap: exception:  ${e.message.toString()}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = e.message.toString()
                )
                this.sendDeliveryStatus(id)
            }

        }.start()
        return id
    }

    private fun executeDeliveryFlow(id: String){
        val currentState = this.mapRepo.getDeliveryFlowState(id)
        val toContinue = when(currentState){
            DeliveryFlowState.START -> importCreate(id)
            DeliveryFlowState.IMPORT_CREATE -> checkImportStatue(id)
            DeliveryFlowState.IMPORT_STATUS -> importDelivery(id)
            DeliveryFlowState.IMPORT_DELIVERY -> checkDeliveryStatus(id)
            DeliveryFlowState.IMPORT_DELIVERY_STATUS -> downloadImport(id)
            DeliveryFlowState.DOWNLOAD -> startProgressWatcher(id)
            DeliveryFlowState.DOWNLOAD_DONE -> validateImport(id)
            DeliveryFlowState.DONE -> false
            else -> false
        }
        if (toContinue) {
            executeDeliveryFlow(id)
        }
    }

    private fun importCreate(id: String): Boolean{
        Log.i(_tag, "importCreate")

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "importCreate - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL)
            this.sendDeliveryStatus(id)
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!
        val mapProperties = MapProperties(productId = mapPkg.pId, boundingBox = mapPkg.bBox, isBest = false)
        val retCreate = createMapImport(mapProperties)

        Log.d(_tag, "importCreate - import request Id: ${retCreate?.importRequestId}")
        when(retCreate?.state){
            MapImportState.START, MapImportState.DONE ->{
                Log.d(_tag,"deliverTile - createMapImport: OK: ${retCreate.state} ")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.START,
                    flowState = DeliveryFlowState.IMPORT_CREATE,
                    statusMessage = appCtx.getString(R.string.delivery_status_req_in_progress)
                )
                this.sendDeliveryStatus(id)
                return true
            }
            MapImportState.CANCEL -> {
                Log.w(_tag,"getDownloadData - createMapImport: CANCEL")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.CANCEL,
                    statusMessage = appCtx.getString(R.string.delivery_status_canceled)
                )
                this.sendDeliveryStatus(id)
                return false
            }
            else -> {
                Log.e(_tag,"getDownloadData - createMapImport failed: ${retCreate?.state}, error: ${retCreate?.importRequestId}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate?.importRequestId,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = retCreate?.importRequestId
                )
                this.sendDeliveryStatus(id)
                return false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatue(id: String): Boolean {
        Log.i(_tag, "checkImportStatue")

        val reqId = this.mapRepo.getReqId(id)!!;

        var stat = getCreateMapImportStatus(reqId)

        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state!! != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(2)

            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "checkImportStatue: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL)
                this.sendDeliveryStatus(id)
                return false
            }

            stat = getCreateMapImportStatus(reqId)

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(_tag,"checkImportStatus - MapImportState.ERROR")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed)
                    )
//                    todo error message
                    this.sendDeliveryStatus(id)
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState.CANCEL")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.CANCEL,
                        statusMessage = appCtx.getString(R.string.delivery_status_canceled)
                    )
                    this.sendDeliveryStatus(id)
                    return false

                }
                else -> {}
            }

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
        }

        Log.d(_tag, "checkImportStatue: MapImportState.Done")
        this.mapRepo.updateFlowState(id, DeliveryFlowState.IMPORT_STATUS)
        return true
    }

    private fun importDelivery(id: String): Boolean{
        Log.i(_tag, "importDelivery")

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "importDelivery: Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL)
            this.sendDeliveryStatus(id)
            return false
        }
        val reqId = this.mapRepo.getReqId(id)!!;

        val retDelivery = setMapImportDeliveryStart(reqId)

        when(retDelivery?.state){
            MapDeliveryState.DONE,
            MapDeliveryState.START,
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE ->  {
                Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
                this.mapRepo.updateFlowState(id, DeliveryFlowState.IMPORT_DELIVERY)
                return true
            }
            MapDeliveryState.CANCEL,  MapDeliveryState.PAUSE -> {
                Log.w(_tag,"getDownloadData - setMapImportDeliveryStart => CANCEL")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.CANCEL,
                    statusMessage = appCtx.getString(R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }
            else -> {
                Log.e(_tag,"getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = "getDownloadData - setMapImportDeliveryStart failed: ${retDelivery?.state}"
                )
                this.sendDeliveryStatus(id)
                return false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun checkDeliveryStatus(id: String): Boolean {
        Log.i(_tag, "checkDeliveryStatus")
        val reqId = this.mapRepo.getReqId(id)!!;

        var deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(reqId)

        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (deliveryStatus.status != PrepareDeliveryResDto.Status.done){
            TimeUnit.SECONDS.sleep(2)

            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "checkDeliveryStatus: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL)
                this.sendDeliveryStatus(id)
                return false
            }
            deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(reqId)

            when(deliveryStatus.status){
                PrepareDeliveryResDto.Status.error -> {
                    Log.e(_tag,"getDownloadData - prepared delivery status: Error")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = appCtx.getString(R.string.delivery_status_failed),
                        errorContent = "getDownloadData - prepared delivery status: Error"
                    )
                    this.sendDeliveryStatus(id)
                    return false
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkDeliveryStatus - timed out")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed),
                    errorContent = "checkDeliveryStatus - timed out"
                )
                this.sendDeliveryStatus(id)
                return false
            }
        }

        if (deliveryStatus.url == null){
            Log.e(_tag, "getDownloadData - download url is null", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                errorContent = "getDownloadData - download url is null"
            )
            this.sendDeliveryStatus(id)
            return false

        }

        Log.d(_tag, "checkDeliveryStatus - delivery is ready, download url: ${deliveryStatus.url} ")
        this.mapRepo.update(id = id, url = deliveryStatus.url, flowState = DeliveryFlowState.IMPORT_DELIVERY_STATUS)
        return true
    }

    private fun downloadImport(id: String): Boolean{
        Log.i(_tag, "downloadImport")

        val mapPkg = this.mapRepo.getById(id)!!
        val pkgUrl = mapPkg.url!!
        val jsonUrl = PackageDownloader.changeFileExtensionToJson(pkgUrl)

        val jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
        val pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)

        Log.d(_tag, "downloadImport - jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")
        val statusMessage = if(mapPkg.metadata.validationAttempt <= 0) appCtx.getString(R.string.delivery_status_download) else appCtx.getString(R.string.delivery_status_failed_verification_try_again)
        this.mapRepo.update(
            id = id,
            JDID = jsonDownloadId,
            MDID = pkgDownloadId,
            state =  MapDeliveryState.DOWNLOAD,
            flowState = DeliveryFlowState.DOWNLOAD,
            statusMessage = statusMessage
        )
        this.sendDeliveryStatus(id)

        return true
    }

    private fun startProgressWatcher(id: String): Boolean{
//        TODO remove the download retry logic
        val pkgData = this.mapRepo.getById(id) ?: return false;

        var pkgDownloadId = pkgData.MDID ?: return false;
        var jsonDownloadId = pkgData.JDID ?: return false;

        val pkgUrl = pkgData.url ?: return false
        val jsonUrl = PackageDownloader.changeFileExtensionToJson(pkgUrl)
        var pkgCompleted = false
        var jsonCompleted = false

        var res = true
        var pkgReqRetry = 1
        var jsonReqRetry = 1

        val latch = CountDownLatch(1)

        timer(initialDelay = 100, period = 2000) {
            if (mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "ProgressWatcher - Download $id, canceled by user")
                downloader.cancelDownload(jsonDownloadId, pkgDownloadId)
                mapRepo.update(id, state = MapDeliveryState.CANCEL)
                res = false
                latch.countDown()
                this.cancel()
            }

            val pkgStatus = downloader.queryStatus(pkgDownloadId)
            val jsonStatus = downloader.queryStatus(jsonDownloadId)


            when(pkgStatus?.status){
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING ->{
                    mapRepo.update(
                        id = id,
                        errorContent = pkgStatus.reason
                    )
                }
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_SUCCESSFUL -> {
                    val progress = (pkgStatus.downloadBytes * 100 / pkgStatus.totalBytes).toInt()
                    Log.d(_tag, "ProgressWatcher - pkgDownloadId: $pkgDownloadId -> process: $progress ")
                    if (!pkgCompleted){
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.DOWNLOAD,
                            downloadProgress = progress,
                            fileName = pkgStatus.fileName
                        )
                        sendDeliveryStatus(id)
                    }

                    if (progress >= 100 || pkgStatus.status == DownloadManager.STATUS_SUCCESSFUL){
                        pkgCompleted = true
                    }

                }
                else -> {
                    Log.e(_tag, "ProgressWatcher -  DownloadManager failed to download pkg, reason: ${pkgStatus?.reason}")
                    if (pkgReqRetry < downloadRetryAttempts){
                        Log.d(_tag, "ProgressWatcher - retry download")
                        pkgReqRetry ++
                        pkgDownloadId = downloader.downloadFile(pkgUrl, completionHandler)
                        mapRepo.update(
                            id = id,
                            MDID = pkgDownloadId,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed_try_again),
                            errorContent = pkgStatus?.reason ?: "ProgressWatcher - DownloadManager failed to download file"
                        )

                    }else{
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.ERROR,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed),
                            errorContent = pkgStatus?.reason ?: "ProgressWatcher - DownloadManager failed to download file"
                        )
                        res = false
                        latch.countDown()
                        this.cancel()
                    }

                }
            }
            when(jsonStatus?.status){
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING ->{
                    mapRepo.update(
                        id = id,
                        errorContent = pkgStatus?.reason
                    )
                }
                DownloadManager.STATUS_RUNNING -> {}
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (!jsonCompleted){
                        Log.d(_tag, "ProgressWatcher - download json Done!")
                        mapRepo.update(id = id, jsonName = jsonStatus.fileName)
                        jsonCompleted = true
                    }
                }
                else -> {
                    Log.e(_tag, "ProgressWatcher -  DownloadManager failed to download json, reason: ${jsonStatus?.reason}")
                    if (jsonReqRetry < downloadRetryAttempts){
                        Log.d(_tag, "ProgressWatcher - retry download")
                        jsonReqRetry ++
                        jsonDownloadId = downloader.downloadFile(jsonUrl, completionHandler)
                        mapRepo.update(
                            id = id,
                            JDID = jsonDownloadId,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed_try_again),
                            errorContent = pkgStatus?.reason ?: "ProgressWatcher - DownloadManager failed to download json"
                        )
                    }else{
                        mapRepo.update(
                            id = id,
                            state = MapDeliveryState.ERROR,
                            statusMessage = appCtx.getString(R.string.delivery_status_failed),
                            errorContent = pkgStatus?.reason ?: "ProgressWatcher - DownloadManager failed to download file"
                        )
                        res = false
                        latch.countDown()
                        this.cancel()
                    }
                }
            }

            if (pkgCompleted && jsonCompleted) {
                Log.d(_tag, "ProgressWatcher - downloading Done")
                Log.d(_tag, "ProgressWatcher - stopping progress watcher...")
                mapRepo.update(
                    id = id,
                    flowState = DeliveryFlowState.DOWNLOAD_DONE,
                    downloadProgress = 100,
                )
                latch.countDown()
                res = true
                this.cancel()
            }
        }
        latch.await()
        sendDeliveryStatus(id)
        return res
    }

    private fun validateImport(id: String): Boolean{
        Log.i(_tag, "validateImport - id: $id")
        this.mapRepo.update(
            id = id,
            statusMessage = appCtx.getString(R.string.delivery_status_in_verification),
            downloadProgress = 0
        )

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "validateImport - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL)
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!

        val isValid =  try{
            val dirPath = Environment.getExternalStoragePublicDirectory(storagePath)
            Log.d(_tag, "validateImport - fileName ${mapPkg.fileName}, jsonName ${mapPkg.jsonName}")
            val mapFile = File(dirPath, mapPkg.fileName!!)
            val jsonFile = File(dirPath, mapPkg.jsonName!!)

            val expectedHash = JsonUtils.getValueFromJson(checksumAlgorithm, jsonFile.path)
            val actualHash = HashUtils.getCheckSumFromFile(checksumAlgorithm, mapFile) {
                Log.d(_tag, "validateImport - progress: $it")
                this.mapRepo.update(id, downloadProgress = it)
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
            this.mapRepo.update(
                id = id,
                state =  MapDeliveryState.DONE,
                flowState = DeliveryFlowState.DONE,
                statusMessage = appCtx.getString(R.string.delivery_status_done),
                errorContent = ""
            )
        }else{
            if (mapPkg.metadata.validationAttempt < 1){
                this.deleteFile(mapPkg.fileName)
                this.deleteFile(mapPkg.jsonName)

                this.mapRepo.update(
                    id = id,
                    flowState = DeliveryFlowState.IMPORT_DELIVERY_STATUS,
                    validationAttempt = ++mapPkg.metadata.validationAttempt,
                    statusMessage = appCtx.getString(R.string.delivery_status_failed_verification_try_again),
                    errorContent = "Checksum validation Failed try downloading again",
                )
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
    private fun sendDeliveryStatus(id: String) {
//        TODO check why send delivery status done twice
        val dlv = this.mapRepo.getDeliveryStatus(id, pref.deviceId) ?: return

        Thread{
            Log.d(_tag, "sendDeliveryStatus - id: $id status: $dlv")
            try{
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            }catch (exc: Exception){
                Log.e(_tag, "sendDeliveryStatus failed error: ${exc.message.toString()}", )
                exc.printStackTrace()
            }
        }.start()
    }
    override fun cancelDownload(id: String) {
        Log.d(_tag, "cancelDownload - for id: $id")
        this.mapRepo.setCancelDownload(id)
    }

    override fun deleteMap(id: String){
        Log.d(_tag, "deleteMap - id: $id")
        val downloadData = this.mapRepo.getDownloadData(id)

        if (downloadData == null ||
            downloadData.deliveryStatus == MapDeliveryState.START ||
            downloadData.deliveryStatus == MapDeliveryState.DOWNLOAD ||
            downloadData.deliveryStatus == MapDeliveryState.CONTINUE){

            val errorMsg = "deleteMap: Unable to delete map when status is: ${downloadData?.deliveryStatus}"
            Log.e(_tag,  errorMsg)
            throw Exception(errorMsg)
        }

        this.deleteFile(downloadData.fileName)
        this.deleteFile(downloadData.jsonName)

        val delivery = this.mapRepo.getDeliveryStatus(id, pref.deviceId)

        this.mapRepo.remove(id)

//        Send delivery status to server
        Thread{
            var dlv = delivery
            if (dlv != null){
                dlv = dlv.copy(deliveryStatus = DeliveryStatusDto.DeliveryStatus.deleted)
                try{
                    Log.d(_tag, "sendDeliveryStatus: id: $id status: $dlv.deliveryStatus, catalog id: ${dlv.catalogId}")
                    client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
                }catch (exc: Exception){
                    Log.e(_tag, "sendDeliveryStatus failed error: $exc", )
                }
            }
        }.start()
    }
    private fun deleteFile(fileName: String?){
        if (fileName == null){
            return
        }
        val dirPath = Environment.getExternalStoragePublicDirectory(storagePath)
        val file = File(dirPath, fileName)

        Log.d(_tag, "deleteFile - File path ${file.path}")

        if (file.exists()){
            if (file.delete() ) {
                Log.d(_tag, "deleteFile - File deleted successfully. $fileName")
            } else {
                Log.d(_tag, "deleteFile -Failed to delete the file. $fileName")
            }
        }else{
            Log.d(_tag, "deleteFile - File dose not exist. $fileName")
        }
    }

    private fun updateMapsStatusOnStart(){
        Log.d(_tag, "updateMapsStatusOnStart")
        val mapsData = this.mapRepo.getAll().filter { it.state == MapDeliveryState.START || it.state == MapDeliveryState.CONTINUE || it.state == MapDeliveryState.DOWNLOAD }
        Log.d(_tag, "updateMapsStatusOnStart - Found: ${mapsData.size} maps on delivery process")

        for (map in mapsData){
            val id = map.id.toString()
            when(map.flowState){
                DeliveryFlowState.DOWNLOAD_DONE -> {}
                DeliveryFlowState.DOWNLOAD -> {
                    Thread{
                        try {
                            Log.d(_tag, "updateMapsStatusOnStar - map id: $id, is on download continue the flow")
                            executeDeliveryFlow(id)
                        } catch (e: Exception) {
                            Log.e(_tag, "updateMapsStatusOnStart: exception:  ${e.message.toString()}")
                            this.mapRepo.update(
                                id = id,
                                state = MapDeliveryState.ERROR,
                                statusMessage = appCtx.getString(R.string.delivery_status_failed),
                                errorContent = e.message.toString()
                            )
                            this.sendDeliveryStatus(id)
                        }}.start()

                }
                else ->{
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