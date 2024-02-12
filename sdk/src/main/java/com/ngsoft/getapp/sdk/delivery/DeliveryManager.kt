package com.ngsoft.getapp.sdk.delivery

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import timber.log.Timber
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
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.getapp.sdk.utils.HashUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class DeliveryManager private constructor(appCtx: Context){

    private val checksumAlgorithm = "sha256"

    private var config = ServiceConfig.getInstance(appCtx)
    private var mapRepo = MapRepo(appCtx)
    private var downloader = PackageDownloader(appCtx, Environment.DIRECTORY_DOWNLOADS)
    private var mapFileManager =  MapFileManager(appCtx, downloader)
    private var pref = Pref.getInstance(appCtx)
    private var client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
    private val app = appCtx as Application

    fun executeDeliveryFlow(id: String){
        Timber.d("executeDeliveryFlow - for id: $id")
        val mapPkg = this.mapRepo.getById(id)
        Timber.d("executeDeliveryFlow - id: &id Flow State: ${mapPkg?.flowState}")

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
            Timber.d("executeDeliveryFlow - to continue: $toContinue")

            if (toContinue) {
                executeDeliveryFlow(id)
            }
        }catch (e: IOException){
            var attempt = mapPkg?.metadata?.connectionAttempt ?: 5
            if (attempt < 5){
                Timber.e("executeDeliveryFlow - IOException try again, attempt: $attempt, Error: ${e.message.toString()}")

                this.mapRepo.update(
                    id = id,
                    statusMessage = app.getString(R.string.delivery_status_connection_issue_try_again),
                    errorContent = e.message.toString(),
                    connectionAttempt = ++attempt
                )
                TimeUnit.SECONDS.sleep(2)
                executeDeliveryFlow(id)
            }else{
                Timber.e("executeDeliveryFlow - exception:  ${e.message.toString()}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = app.getString(R.string.delivery_status_failed),
                    errorContent = e.message.toString()
                )
                this.sendDeliveryStatus(id)
            }
        }catch (e: Exception){
            Timber.e("executeDeliveryFlow - exception:  ${e.message.toString()}")
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed),
                errorContent = e.message.toString()
            )
            this.sendDeliveryStatus(id)
        }

    }

    private fun importCreate(id: String): Boolean{
        Timber.i("importCreate")

        if (this.mapRepo.isDownloadCanceled(id)){
            Timber.d("importCreate - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
            this.sendDeliveryStatus(id)
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!
        val mapProperties = MapProperties(productId = mapPkg.pId, boundingBox = mapPkg.bBox, isBest = false)
        val retCreate = MapImportClient.createMapImport(client, mapProperties, pref.deviceId)

        Timber.d("importCreate - import request Id: ${retCreate?.importRequestId}")
        when(retCreate?.state){
            MapImportState.START, MapImportState.IN_PROGRESS, MapImportState.DONE,  ->{
                Timber.d("deliverTile - createMapImport -> OK, state: ${retCreate.state} message: ${retCreate.statusCode?.messageLog}")
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
                Timber.w("getDownloadData - createMapImport -> CANCEL, message: ${retCreate.statusCode?.messageLog}")
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
                Timber.e("getDownloadData - createMapImport failed: ${retCreate?.state}, error: ${retCreate?.statusCode?.messageLog}")
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
        Timber.i("checkImportStatue")

        val reqId = this.mapRepo.getReqId(id)!!;
        var timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        var stat : CreateMapImportStatus? = null
        var lastProgress : Int? = null
        do{
            if(timeoutTime.hasPassedNow()){
                Timber.w("checkImportStatus - timed out")
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
                Timber.d("checkImportStatue: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(
                    R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }

            try {
                stat = MapImportClient.getCreateMapImportStatus(client, reqId)
            }catch (e: IOException){
                Timber.e("checkImportStatue - SocketException, try again. error: ${e.message.toString()}" )
                this.mapRepo.update(id = id,
                    statusMessage = app.getString(R.string.delivery_status_connection_issue_try_again), errorContent = e.message.toString())
                continue
            }

            when(stat?.state){
                MapImportState.ERROR -> {
                    Timber.e("checkImportStatus - MapImportState -> ERROR, error:  ${stat.statusCode?.messageLog}")
                    if (stat.statusCode?.statusCode == StatusCode.REQUEST_ID_NOT_FOUND){
                        Timber.e("checkImportStatus - status code is REQUEST_ID_NOT_FOUND, set as obsolete")
                        handleMapNotExistsOnServer(id)
                    }else{
                        this.mapRepo.update(id = id, state = MapDeliveryState.ERROR,
                            statusMessage = app.getString(R.string.delivery_status_failed), errorContent = stat.statusCode?.messageLog)
                    }
                    this.sendDeliveryStatus(id)
                    return false
                }
                MapImportState.CANCEL -> {
                    Timber.w("checkImportStatus - MapImportState -> CANCEL, message: ${stat.statusCode?.messageLog}")
                    this.mapRepo.update(id = id, state = MapDeliveryState.CANCEL,
                        statusMessage = app.getString(R.string.delivery_status_canceled), errorContent = stat.statusCode?.messageLog)
                    this.sendDeliveryStatus(id)
                    return false

                }
                MapImportState.IN_PROGRESS -> {
                    Timber.w("checkImportStatus - MapImportState -> IN_PROGRESS, progress: ${stat.progress}")
                    this.mapRepo.update(id = id, downloadProgress = stat.progress,
                        statusMessage = app.getString(R.string.delivery_status_req_in_progress), errorContent = "")
                    if (lastProgress != stat.progress){
                        timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes
                    }
                    lastProgress = stat.progress
                }
                else -> {}
            }

        }while (stat == null || stat.state!! != MapImportState.DONE)

        Timber.d("checkImportStatue: MapImportState.Done")
        this.mapRepo.update(
            id = id,
            downloadProgress = 100,
            flowState = DeliveryFlowState.IMPORT_STATUS,
            errorContent = "")
        return true
    }

    @OptIn(ExperimentalTime::class)
    private fun importDelivery(id: String): Boolean{
        Timber.i("importDelivery")

        val reqId = this.mapRepo.getReqId(id)!!;

        var retDelivery: MapImportDeliveryStatus? =  MapImportDeliveryStatus()
        for (i in 0 until 3){
            try {
                retDelivery = MapDeliveryClient.setMapImportDeliveryStart(client, reqId, pref.deviceId)
                break
            }catch (e: Exception){
                Timber.e("importDelivery - error: ${e.message.toString()}")
                if (i == 2){
                    this.mapRepo.update(id = id,
                        state = MapDeliveryState.ERROR,
                        statusMessage = app.getString(R.string.delivery_status_failed),
                        errorContent = "importDelivery - setMapImportDeliveryStart failed: ${e.message.toString()}")
                    return false
                }
                TimeUnit.SECONDS.sleep(1)
            }
        }
        val timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        while (retDelivery?.state != MapDeliveryState.DONE) {
            when (retDelivery?.state) {
                MapDeliveryState.DONE, MapDeliveryState.START, MapDeliveryState.DOWNLOAD, MapDeliveryState.CONTINUE -> {
                    this.mapRepo.update(id = id, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                        statusMessage = app.getString(R.string.delivery_status_prepare_to_download), errorContent = "")

                }

                MapDeliveryState.CANCEL, MapDeliveryState.PAUSE -> {
                    Timber.w("importDelivery - setMapImportDeliveryStart => CANCEL")
                    this.mapRepo.update(id = id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(
                        R.string.delivery_status_canceled))
                    this.sendDeliveryStatus(id)
                    return false
                }
                else -> {
                    Timber.e("importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}")
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
                Timber.e("importDelivery - timed out")
                this.mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = app.getString(
                    R.string.delivery_status_failed), errorContent = "ImportDelivery- timed out")
                this.sendDeliveryStatus(id)
                return false
            }

            if (this.mapRepo.isDownloadCanceled(id)){
                Timber.d("importDelivery: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(
                    R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }
            TimeUnit.SECONDS.sleep(2)
            retDelivery = MapDeliveryClient.getMapImportDeliveryStatus(client, reqId)
        }

        if (retDelivery.url == null){
            Timber.e("importDelivery- download url is null", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed),
                errorContent = "importDelivery - download url is null"
            )
            this.sendDeliveryStatus(id)
            return false

        }

        Timber.d("importDelivery - delivery is ready, download url: ${retDelivery.url} ")
        this.mapRepo.update(id = id, url = retDelivery.url, flowState = DeliveryFlowState.IMPORT_DELIVERY, errorContent = "")
        return true
    }
    private fun downloadImport(id: String): Boolean{
        Timber.i("downloadImport")

        val mapPkg = this.mapRepo.getById(id)!!
        val pkgUrl = mapPkg.url!!
        val jsonUrl = FileUtils.changeFileExtensionToJson(pkgUrl)

        val jsonDownloadId = if(!mapPkg.metadata.jsonDone) downloadFile(id, jsonUrl, true) else null
        val pkgDownloadId = if(!mapPkg.metadata.mapDone) downloadFile(id, pkgUrl, false) else null
        Timber.d("downloadImport - jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")

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
        Timber.i("downloadFile")
        val fileName = FileUtils.getUniqueFileName(config.storagePath, FileUtils.getFileNameFromUri(url))
        val downloadId = downloader.downloadFile(url, fileName){
            Timber.d("downloadImport - completionHandler: processing download ID=$it completion event...")
        }
        watchDownloadProgress(downloadId, id, url, isJson)
        return downloadId
    }

    private fun watchDownloadImport(id: String): Boolean{
        Timber.i("watchDownloadImport")
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
        Timber.i("watchDownloadProgress, isJson: $isJson")
        this.mapRepo.update(id, flowState = DeliveryFlowState.DOWNLOAD, errorContent = "", statusMessage = app.getString(R.string.delivery_status_download))

        timer(initialDelay = 100, period = 2000) {
//            todo check what happen when cancel the download in the download manager
            val mapPkg = mapRepo.getById(id)
            if (mapPkg == null){
                this.cancel()
                return@timer
            }
            if (mapPkg.state == MapDeliveryState.ERROR){
                Timber.e("watchDownloadProgress download status for $id, isJson: $isJson, is ERROR. abort the process" )
                downloader.cancelDownload(downloadId)
                this.cancel()
                return@timer
            }
            if (mapPkg.cancelDownload){
                Timber.d("downloadFile - Download $id, canceled by user")
                downloader.cancelDownload(downloadId)
                if (!isJson){
                    mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMessage = app.getString(
                        R.string.delivery_status_canceled),)
                }
                this.cancel()
                return@timer
            }

//            TODO edge case of
            val statusInfo = downloader.queryStatus(downloadId)
            when(statusInfo?.status){
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                    mapRepo.update(id = id, errorContent = statusInfo.reason)
                }
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_SUCCESSFUL -> {
                    val progress = (statusInfo.downloadBytes * 100 / statusInfo.totalBytes).toInt()
                    Timber.d("downloadFile - DownloadId: $downloadId -> process: $progress ")

                    if (!isJson && !mapPkg.metadata.mapDone){
                        mapRepo.update(
                            id = id, downloadProgress = progress, fileName = statusInfo.fileName,
                            state = MapDeliveryState.DOWNLOAD, statusMessage = app.getString(R.string.delivery_status_download), errorContent = "")
                        sendDeliveryStatus(id)
                    }

                    if (progress >= 100 || statusInfo.status == DownloadManager.STATUS_SUCCESSFUL){
                        val updatedMapPkg  = if (isJson){
                            handleJsonDone(id, statusInfo.fileName) ?: mapPkg
                        }else{
                            mapRepo.updateAndReturn(id, mapDone = true)
                        }
                        Timber.i("downloadFile - id: $id, Map Done: ${updatedMapPkg?.metadata?.mapDone}, Json Done: ${updatedMapPkg?.metadata?.jsonDone}, state: ${updatedMapPkg?.state} ")
                        if (updatedMapPkg?.metadata?.mapDone == true && updatedMapPkg.metadata.jsonDone && updatedMapPkg.state != MapDeliveryState.ERROR){
                            Timber.d("downloadFile - downloading Done")
                            mapRepo.update(id = id, flowState = DeliveryFlowState.DOWNLOAD_DONE,
                                downloadProgress = 100, errorContent = "")
                            sendDeliveryStatus(id)
                            Thread{executeDeliveryFlow(id)}.start()
//                            make sure json did not done
                        }else if(updatedMapPkg?.metadata?.mapDone == true && !updatedMapPkg.metadata.jsonDone && updatedMapPkg.state != MapDeliveryState.ERROR){
                           updatedMapPkg.JDID?.let{
                               val info = downloader.queryStatus(it)
                               if (info?.status == DownloadManager.STATUS_SUCCESSFUL){
                                   Timber.d("Download file - json is actually done")
                                   if (handleJsonDone(id, info.fileName)?.metadata?.jsonDone == false){
                                       this.cancel()
                                       return@timer
                                   }
                                   Timber.d("downloadFile - downloading Done")
                                   mapRepo.update(id = id, flowState = DeliveryFlowState.DOWNLOAD_DONE,
                                       downloadProgress = 100, errorContent = "")
                                   sendDeliveryStatus(id)
                                   Thread{executeDeliveryFlow(id)}.start()
                               }
                           }
                        }
                        this.cancel()
                    }
                }
                else -> {
                    Timber.e("downloadFile -  DownloadManager failed to download file. id: $downloadId, reason: ${statusInfo?.reason}")

                    val downloadAttempts = if (isJson) mapPkg.metadata.jsonAttempt else mapPkg.metadata.mapAttempt
                    val isStatusError = mapRepo.getById(id)?.state == MapDeliveryState.ERROR
                    if (statusInfo?.status == DownloadManager.STATUS_FAILED && (statusInfo.reasonCode == 403 || statusInfo.reasonCode == 404)){
                        Timber.e("watchDownloadProgress - download status is ${statusInfo.reasonCode}")
                        handleMapNotExistsOnServer(id)

                    }else if (downloadAttempts < config.downloadRetry && !isStatusError) {
                        Timber.d("downloadFile - retry download")
                        downloader.cancelDownload(downloadId)

                        mapRepo.update(id, statusMessage = app.getString(R.string.delivery_status_failed_verification_try_again),
                            errorContent = statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file")
                        handelDownloadRetry(id, url, isJson, downloadAttempts)
                    }else{
                        val error = if (isStatusError) null else statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file"
                        mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMessage = app.getString(
                            R.string.delivery_status_failed),
                            errorContent = error
                        )
                    }
                    sendDeliveryStatus(id)
                    this.cancel()
                }
            }
        }
        return downloadId
    }

    private fun handleJsonDone(id: String, jsonName: String?): MapPkg?{
        val mapPkg = this.mapRepo.getById(id) ?: return null

        return try {
            val json = JsonUtils.readJson(Paths.get(config.downloadPath, jsonName).toString())
            val footprint = FootprintUtils.toString(json.getJSONObject("footprint"))
            this.mapRepo.getByBBox(mapPkg.bBox, footprint).forEach{ pkg ->
                if (pkg.id.toString() != id && pkg.isUpdated){
                    Timber.e("handleJsonDone - map already exists, set to error", )
                    this.mapRepo.update(id, state = MapDeliveryState.ERROR,
                        statusMessage = app.getString(R.string.error_map_already_exists),
                        jsonName = "fail.json", fileName ="fail.gpkg")
                    mapPkg.JDID?.let { downloader.cancelDownload(it) }
                    mapPkg.MDID?.let { downloader.cancelDownload(it) }

                    return null
                }
            }
            Timber.d("handleJsonDone - for id: $id")
            mapRepo.setFootprint(id, footprint)
            mapRepo.updateAndReturn(id, jsonDone = true, jsonName = jsonName)

        }catch (e: Exception){
            Timber.e("Failed to get footprint from json, error: ${e.message.toString()}")
            null
        }

    }
    private fun handleMapNotExistsOnServer(id: String){
        Timber.i("handleMapNotExistsOnServer")
        val mapPkg = this.mapRepo.getById(id) ?: return
        mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)
        this.mapRepo.update(id=id, flowState = DeliveryFlowState.START, state = MapDeliveryState.ERROR,
            statusMessage = app.getString(R.string.delivery_status_error), errorContent = app.getString(
                R.string.delivery_status_description_failed_not_exists_on_server),
            downloadProgress = 0, mapDone = false,
            jsonDone = false, mapAttempt = 0, jsonAttempt = 0, connectionAttempt = 0, validationAttempt = 0)
        this.mapRepo.setMapUpdated(id, false)
        mapPkg.JDID?.let { downloader.cancelDownload(it) }
        mapPkg.MDID?.let { downloader.cancelDownload(it) }

    }

    private fun handelDownloadRetry(id: String, url: String, isJson: Boolean, downloadAttempts: Int) {
        Timber.i("handelDownloadRetry, id: $id, isJson: $isJson ")
        val waitTime = TimeUnit.MINUTES.toMillis(if (downloadAttempts == 1) 5 else 10)
        val startTime = System.currentTimeMillis()

        Timber.d("handelDownloadRetry try again in: ${TimeUnit.MILLISECONDS.toMinutes(waitTime)} minutes")
        timer(initialDelay = 100, period = 2000) {
            val diff = System.currentTimeMillis() - startTime
            if (diff > waitTime){
                Timber.d("handelDownloadRetry - try again id: $id, isJson: $isJson")
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
                    Timber.d("handelDownloadRetry - Download $id, canceled by user")
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
        Timber.i("moveImportFiles - id: $id")

        val mapPkg = this.mapRepo.getById(id)!!

        return try {
            Timber.d("moveImportFiles - fileName ${mapPkg.fileName} jsonName: ${mapPkg.jsonName}")
            val (fileName, jsonName) = mapFileManager.moveFilesToTargetDir(mapPkg.fileName!!, mapPkg.jsonName!!)
//            fileName = mapFileManager.moveFileToTargetDir(fileName)
//            jsonName = mapFileManager.moveFileToTargetDir(jsonName)
            this.mapRepo.update(id, flowState = DeliveryFlowState.MOVE_FILES, fileName = fileName, jsonName = jsonName)
            true
        }catch (e: Exception){
            Timber.e("moveImportFiles - move file failed: ${e.message.toString()}", )
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
        Timber.i("validateImport - id: $id")
        this.mapRepo.update(
            id = id,
            statusMessage = app.getString(R.string.delivery_status_in_verification),
            downloadProgress = 0, errorContent = ""
        )

        if (this.mapRepo.isDownloadCanceled(id)){
            Timber.d("validateImport - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!

        val isValid = try{
            Timber.d("validateImport - fileName ${mapPkg.fileName}, jsonName ${mapPkg.jsonName}")
            val mapFile = File(config.storagePath, mapPkg.fileName!!)
            val jsonFile = File(config.storagePath, mapPkg.jsonName!!)

            val expectedHash = JsonUtils.getStringOrThrow(checksumAlgorithm, jsonFile.path)
            val actualHash = HashUtils.getCheckSumFromFile(checksumAlgorithm, mapFile) {
                Timber.d("validateImport - progress: $it")
                this.mapRepo.update(id, downloadProgress = it, statusMessage = app.getString(R.string.delivery_status_in_verification), errorContent = "")
            }
            Timber.d("validateImport - expectedHash: $expectedHash, actualHash: $actualHash")

            val isValid = expectedHash == actualHash
            Timber.d("validateImport - validation result for id: $id is: $isValid")

            isValid

        }catch (e: Exception){
            Timber.e("validateImport - Failed to validate map, error: ${e.message.toString()} ")
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
                Timber.d("validateImport - Failed downloading again")
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
        Timber.i("findAndRemoveDuplicate")
        val mapPkg = this.mapRepo.getById(id) ?: return
        val duplicates = this.mapRepo.getByBBox(mapPkg.bBox, mapPkg.footprint).toMutableList()
        duplicates.removeIf { it.id.toString() == id }

        Timber.d("findAndRemoveDuplicate - found ${duplicates.size} duplicates")
        duplicates.forEach {
            Timber.d("findAndRemoveDuplicate - remove: ${it.id}")
            try {
                mapFileManager.deleteMap(it)
                MapDeliveryClient.sendDeliveryStatus(client, mapRepo, it.id.toString(), pref.deviceId, MapDeliveryState.DELETED)
                this.mapRepo.remove(it.id.toString())
            }catch (error: Exception){
                Timber.e("findAndRemoveDuplicates - failed to delete the map, error: ${error.message.toString()}", )
            }
        }
    }
    private fun sendDeliveryStatus(id: String, state: MapDeliveryState?=null) {
        MapDeliveryClient.sendDeliveryStatus(client, mapRepo, id, pref.deviceId, state)
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
        Timber.i("cancelDelivery - for id $id")
        Thread{
            try{
                this.mapRepo.setCancelDownload(id)
//                Force cancel
                TimeUnit.SECONDS.sleep(7)
                if (this.mapRepo.isDownloadCanceled(id)){
                    Timber.e("cancelDelivery - Download $id, was not canceled after 6 sec, force cancel")
                    mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
                    this.sendDeliveryStatus(id)
                }

            }catch (e: Exception){
                Timber.e("cancelDownload - failed to candle, error: $e", )
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