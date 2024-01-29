package com.ngsoft.getapp.sdk.delivery

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.ngsoft.getapp.sdk.MapFileManager
import com.ngsoft.getapp.sdk.PackageDownloader
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.ServiceConfig
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.helpers.client.MapImportClient
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DeliveryStatus
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.HashUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class DeliveryManager private constructor(appCtx: Context){

    private val _tag = "DeliveryManager"
    private val checksumAlgorithm = "sha256"

    private var config = ServiceConfig.getInstance(appCtx)
    private var mapRepo = MapRepo(appCtx)
    private var downloader = PackageDownloader(appCtx, Environment.DIRECTORY_DOWNLOADS)
    private var mapFileManager =  MapFileManager(appCtx, downloader)
    private var pref = Pref.getInstance(appCtx)
    private var client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
    private val app = appCtx as Application

    fun executeDeliveryFlow(id: String){
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
                    statusMessage = app.getString(R.string.delivery_status_connection_issue_try_again),
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
                    statusMessage = app.getString(R.string.delivery_status_failed),
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
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
            this.sendDeliveryStatus(id)
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!
        val mapProperties = MapProperties(productId = mapPkg.pId, boundingBox = mapPkg.bBox, isBest = false)
        val retCreate = MapImportClient.createMapImport(client, mapProperties, pref.deviceId)

        Log.d(_tag, "importCreate - import request Id: ${retCreate?.importRequestId}")
        when(retCreate?.state){
            MapImportState.START, MapImportState.IN_PROGRESS, MapImportState.DONE,  ->{
                Log.d(_tag,"deliverTile - createMapImport -> OK, state: ${retCreate.state} message: ${retCreate.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.START,
                    flowState = DeliveryFlowState.IMPORT_CREATE,
                    statusMessage = app.getString(R.string.delivery_status_req_in_progress),
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
                    statusMessage = app.getString(R.string.delivery_status_canceled),
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
                    statusMessage = app.getString(R.string.delivery_status_failed),
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
                    statusMessage = app.getString(R.string.delivery_status_failed),
                    errorContent = "checkImportStatus - timed out"
                )
                this.sendDeliveryStatus(id)
                return false

            }

            TimeUnit.SECONDS.sleep(2)
            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "checkImportStatue: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(
                    R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }


            try {
                stat = MapImportClient.getCreateMapImportStatus(client, reqId)
            }catch (e: IOException){
                Log.e(_tag, "checkImportStatue - SocketException, try again. error: ${e.message.toString()}" )
                this.mapRepo.update(
                    id = id,
                    statusMessage = app.getString(R.string.delivery_status_connection_issue_try_again),
                    errorContent = e.message.toString()
                )
                continue
            }

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(_tag,"checkImportStatus - MapImportState -> ERROR, error:  ${stat.statusCode?.messageLog}")
                    if (stat.statusCode?.statusCode == StatusCode.REQUEST_ID_NOT_FOUND){
                        Log.e(_tag, "checkImportStatus - status code is REQUEST_ID_NOT_FOUND, set as obsolete")
                        handleMapNotExistsOnServer(id)
                    }else{
                        this.mapRepo.update(
                            id = id,
                            state = MapDeliveryState.ERROR,
                            statusMessage = app.getString(R.string.delivery_status_failed),
                            errorContent = stat.statusCode?.messageLog
                        )
                    }
                    this.sendDeliveryStatus(id)
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState -> CANCEL, message: ${stat.statusCode?.messageLog}")
                    this.mapRepo.update(
                        id = id,
                        state = MapDeliveryState.CANCEL,
                        statusMessage = app.getString(R.string.delivery_status_canceled),
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
                        statusMessage = app.getString(R.string.delivery_status_req_in_progress),
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

        var retDelivery = MapDeliveryClient.setMapImportDeliveryStart(client, reqId, pref.deviceId)
        val timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        while (retDelivery?.state != MapDeliveryState.DONE) {
            when (retDelivery?.state) {
                MapDeliveryState.DONE, MapDeliveryState.START, MapDeliveryState.DOWNLOAD, MapDeliveryState.CONTINUE -> {
                    this.mapRepo.update(id = id, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                        statusMessage = app.getString(R.string.delivery_status_prepare_to_download), errorContent = "")

                }

                MapDeliveryState.CANCEL, MapDeliveryState.PAUSE -> {
                    Log.w(_tag, "importDelivery - setMapImportDeliveryStart => CANCEL")
                    this.mapRepo.update(id = id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(
                        R.string.delivery_status_canceled))
                    this.sendDeliveryStatus(id)
                    return false
                }
                else -> {
                    Log.e(_tag, "importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                    this.mapRepo.update(id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = app.getString(R.string.delivery_status_failed),
                        errorContent = "importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}"
                    )
                    this.sendDeliveryStatus(id)
                    return false
                }
            }
            if(timeoutTime.hasPassedNow()){
                Log.e(_tag,"importDelivery - timed out")
                this.mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = app.getString(
                    R.string.delivery_status_failed), errorContent = "ImportDelivery- timed out")
                this.sendDeliveryStatus(id)
                return false
            }

            if (this.mapRepo.isDownloadCanceled(id)){
                Log.d(_tag, "importDelivery: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(
                    R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }
            TimeUnit.SECONDS.sleep(2)
            retDelivery = MapDeliveryClient.getMapImportDeliveryStatus(client, reqId)
        }

        if (retDelivery.url == null){
            Log.e(_tag, "importDelivery- download url is null", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed),
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

        val statusMessage = if(mapPkg.metadata.validationAttempt <= 0) app.getString(R.string.delivery_status_download) else app.getString(
            R.string.delivery_status_failed_verification_try_again)
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
        val downloadId = downloader.downloadFile(url){
            Log.d(_tag, "downloadImport - completionHandler: processing download ID=$it completion event...")
        }
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
                    mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMessage = app.getString(
                        R.string.delivery_status_canceled),)
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
                            statusMessage = app.getString(R.string.delivery_status_download),
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
                        Log.e(_tag, "watchDownloadProgress - download status is ${statusInfo.reasonCode}")
                        handleMapNotExistsOnServer(id)

                    }else if (downloadAttempts < config.downloadRetry) {
                        Log.d(_tag, "downloadFile - retry download")
                        downloader.cancelDownload(downloadId)

                        mapRepo.update(id, statusMessage = app.getString(R.string.delivery_status_failed_verification_try_again),
                            errorContent = statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file")
                        handelDownloadRetry(id, url, isJson, downloadAttempts)
                    }else{
                        mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = app.getString(
                            R.string.delivery_status_failed),
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

    private fun handleMapNotExistsOnServer(id: String){
        Log.i(_tag, "handleMapNotExistsOnServer")
        val mapPkg = this.mapRepo.getById(id) ?: return
        mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)
        this.mapRepo.update(id=id, flowState = DeliveryFlowState.START, state = MapDeliveryState.ERROR,
            statusMessage = app.getString(R.string.delivery_status_error), errorContent = app.getString(
                R.string.delivery_status_description_failed_not_exists_on_server),
            jsonName = "", fileName = "", url = "", reqId = "", downloadProgress = 0, mapDone = false,
            jsonDone = false, mapAttempt = 0, jsonAttempt = 0, connectionAttempt = 0, validationAttempt = 0,)
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

                val statusMessage = app.getString(R.string.delivery_status_failed_try_again)

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
                        mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMessage = app.getString(
                            R.string.delivery_status_canceled))
                        this.cancel()
                    }
                }
                if (!isJson) {
                    val secToFinish = TimeUnit.MILLISECONDS.toSeconds(waitTime - diff)
                    mapRepo.update(id, statusMessage = app.getString(R.string.delivery_status_failed_try_again_in, secToFinish))
                }
            }
        }
    }
    private fun moveImportFiles(id: String): Boolean{
        Log.i(_tag, "moveImportFiles - id: $id")

        val mapPkg = this.mapRepo.getById(id)!!

        return try {
            Log.d(_tag, "moveImportFiles - fileName ${mapPkg.fileName} jsonName: ${mapPkg.jsonName}")
            val (fileName, jsonName) = mapFileManager.moveFilesToTargetDir(mapPkg.fileName!!, mapPkg.jsonName!!)
//            fileName = mapFileManager.moveFileToTargetDir(fileName)
//            jsonName = mapFileManager.moveFileToTargetDir(jsonName)
            this.mapRepo.update(id, flowState = DeliveryFlowState.MOVE_FILES, fileName = fileName, jsonName = jsonName)
            true
        }catch (e: Exception){
            Log.e(_tag, "moveImportFiles - move file failed: ${e.message.toString()}", )
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed),
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
            statusMessage = app.getString(R.string.delivery_status_in_verification),
            downloadProgress = 0, errorContent = ""
        )

        if (this.mapRepo.isDownloadCanceled(id)){
            Log.d(_tag, "validateImport - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
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
                this.mapRepo.update(id, downloadProgress = it, statusMessage = app.getString(R.string.delivery_status_in_verification), errorContent = "")
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
                statusMessage = app.getString(R.string.delivery_status_done),
                errorContent = ""
            )
        }else{
            if (mapPkg.metadata.validationAttempt < 1){
                mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)

                this.mapRepo.update(
                    id = id,
                    flowState = DeliveryFlowState.IMPORT_DELIVERY,
                    validationAttempt = ++mapPkg.metadata.validationAttempt,
                    statusMessage = app.getString(R.string.delivery_status_failed_verification_try_again),
                    errorContent = "Checksum validation Failed try downloading again",
                )
                Log.d(_tag, "validateImport - Failed downloading again")
                return true
            }
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed_verification),
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
                mapFileManager.deleteMap(it)
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

        MapDeliveryClient.pushDeliveryStatus(client, deliveryStatus, pref.deviceId)
    }

    fun getMapsOnDownload(): LiveData<List<MapDownloadData>>{
        return Transformations.map(this.mapRepo.getAllMapsLiveData()){ maps ->
            val onDownloadList = maps.filter {
                it.deliveryStatus == MapDeliveryState.DOWNLOAD
                        || it.deliveryStatus == MapDeliveryState.CONTINUE
                        || it.deliveryStatus == MapDeliveryState.START
            }
            onDownloadList
        }
    }

    fun cancelDelivery(id: String){
        Log.i(_tag, "cancelDelivery - for id $id")
        Thread{
            try{
                this.mapRepo.setCancelDownload(id)
            }catch (e: Exception){
                Log.e(_tag, "cancelDownload - failed to candle, error: $e", )
            }
        }.start()
    }


    companion object {
        @Volatile
        private var instance: DeliveryManager? = null

        fun getInstance(appContext: Context): DeliveryManager {
            return instance ?: synchronized(this){
                instance ?: DeliveryManager(appContext.applicationContext).also { instance = it }
            }
        }
    }

}