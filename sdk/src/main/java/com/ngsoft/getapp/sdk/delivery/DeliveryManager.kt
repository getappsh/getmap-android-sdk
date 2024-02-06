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
import com.ngsoft.getapp.sdk.delivery.flow.ImportCreateFlow
import com.ngsoft.getapp.sdk.delivery.flow.ImportDeliveryFlow
import com.ngsoft.getapp.sdk.delivery.flow.ImportStatusFlow
import com.ngsoft.getapp.sdk.delivery.flow.MoveImportFilesFlow
import com.ngsoft.getapp.sdk.delivery.flow.ValidateImportFlow
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
internal class DeliveryManager private constructor(appCtx: Context){

    private var config = ServiceConfig.getInstance(appCtx)
    private var mapRepo = MapRepo(appCtx)
    private var downloader = PackageDownloader(appCtx, Environment.DIRECTORY_DOWNLOADS)
    private var mapFileManager =  MapFileManager(appCtx, downloader)
    private var pref = Pref.getInstance(appCtx)
    private var client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
    private val app = appCtx as Application
    private val dlvContext = DeliveryContext(config=config, mapRepo=mapRepo, downloader=downloader, mapFileManager=mapFileManager, pref=pref, client=client, app=app)

    fun executeDeliveryFlow(id: String){
        Timber.d("executeDeliveryFlow - for id: $id")
        val mapPkg = this.mapRepo.getById(id)
        Timber.d("executeDeliveryFlow - id: &id Flow State: ${mapPkg?.flowState}")

        try{
            val toContinue = when(mapPkg?.flowState){
                DeliveryFlowState.START -> ImportCreateFlow(dlvContext).execute(id)
                DeliveryFlowState.IMPORT_CREATE -> ImportStatusFlow(dlvContext).execute(id)
                DeliveryFlowState.IMPORT_STATUS -> ImportDeliveryFlow(dlvContext).execute(id)
                DeliveryFlowState.IMPORT_DELIVERY -> downloadImport(id)
                DeliveryFlowState.DOWNLOAD ->  watchDownloadImport(id)
                DeliveryFlowState.DOWNLOAD_DONE -> MoveImportFilesFlow(dlvContext).execute(id)
                DeliveryFlowState.MOVE_FILES -> ValidateImportFlow(dlvContext).execute(id)
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