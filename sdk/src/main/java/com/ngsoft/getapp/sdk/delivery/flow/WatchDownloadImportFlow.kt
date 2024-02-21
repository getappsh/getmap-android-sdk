package com.ngsoft.getapp.sdk.delivery.flow

import android.app.DownloadManager
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.FootprintUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import timber.log.Timber
import java.nio.file.Paths
import java.util.TimerTask
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

internal class WatchDownloadImportFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx) {

    private var toContinue = false
    private val latch = CountDownLatch(1)

    override fun execute(id: String): Boolean {
        Timber.i("watchDownloadImport")
        toContinue = false
        val mapPkg = this.mapRepo.getById(id) ?: return false

        val pkgUrl = mapPkg.url ?: return false
        val jsonUrl = FileUtils.changeFileExtensionToJson(pkgUrl)

        mapPkg.MDID?.let {
            watchDownloadProgress(it, id, pkgUrl, false)
        }
        mapPkg.JDID?.let{
            watchDownloadProgress(it, id, jsonUrl, true)
        }

        latch.await()
        Timber.i("watchDownloadImport - toContinue: $toContinue")
        return toContinue
    }

    private fun watchDownloadProgress(downloadId: Long, id: String, url: String, isJson: Boolean): Long{
        Timber.i("watchDownloadProgress, isJson: $isJson")
        this.mapRepo.update(id, flowState = DeliveryFlowState.DOWNLOAD, statusDescr = "", statusMsg = app.getString(
            R.string.delivery_status_download))

        var dId = downloadId
        timer(initialDelay = 100, period = 2000) {
            val mapPkg = getMapAndValidate(id, dId, isJson)
            if (mapPkg == null){
                cancelAndLatch(this@timer, latch)
                return@timer
            }


//            TODO edge case of
            val statusInfo = downloader.queryStatus(dId)
            when(statusInfo?.status){
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                    mapRepo.update(id = id, statusDescr = statusInfo.reason)
                }
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_SUCCESSFUL -> {
                    val progress = (statusInfo.downloadBytes * 100 / statusInfo.totalBytes).toInt()
                    Timber.d("downloadFile - DownloadId: $dId -> process: $progress ")

                    if (!isJson && !mapPkg.metadata.mapDone){
                        mapRepo.update(id = id, downloadProgress = progress, fileName = statusInfo.fileName,
                            state = MapDeliveryState.DOWNLOAD, statusMsg = app.getString(R.string.delivery_status_download), statusDescr = "")
                        sendDeliveryStatus(id)
                    }

                    if (progress >= 100 || statusInfo.status == DownloadManager.STATUS_SUCCESSFUL){
                        val updatedMapPkg  = if (isJson){
                            handleJsonDone(id, statusInfo.fileName) ?: mapPkg
                        }else{
                            mapRepo.updateAndReturn(id, mapDone = true)
                        }
                        Timber.i("downloadFile - id: $id, Map Done: ${updatedMapPkg?.metadata?.mapDone}, Json Done: ${updatedMapPkg?.metadata?.jsonDone}, state: ${updatedMapPkg?.state} ")

                        if (updatedMapPkg?.metadata?.mapDone == true && updatedMapPkg.state != MapDeliveryState.ERROR){
                            val JDID = updatedMapPkg.JDID

                            if (updatedMapPkg.metadata.jsonDone){
                                handleDownloadDone(id)

                            }else if(JDID != null){
                                val info = downloader.queryStatus(JDID)

                                if (info?.status == DownloadManager.STATUS_SUCCESSFUL){
                                    Timber.d("Download file - json is actually done")
//                                    TODO Check again the logic
                                    if (handleJsonDone(id, info.fileName)?.metadata?.jsonDone == true){
                                        handleDownloadDone(id)
                                    }
                                }
                            }
                        }
                        this.cancel()
                    }
                }
                else -> {
                    Timber.e("downloadFile -  DownloadManager failed to download file. id: $dId, reason: ${statusInfo?.reason}")

                    val downloadAttempts = if (isJson) mapPkg.metadata.jsonAttempt else mapPkg.metadata.mapAttempt
                    val isStatusError = mapRepo.getById(id)?.state == MapDeliveryState.ERROR

                    if (statusInfo?.status == DownloadManager.STATUS_FAILED && (statusInfo.reasonCode == 403 || statusInfo.reasonCode == 404)){
                        Timber.e("watchDownloadProgress - download status is ${statusInfo.reasonCode}")
                        handleMapNotExistsOnServer(id)

                    }else if (downloadAttempts < config.downloadRetry && !isStatusError) {
                        Timber.d("downloadFile - retry download")
                        downloader.cancelDownload(dId)
                        mapRepo.update(id, statusMsg = app.getString(R.string.delivery_status_failed_verification_try_again),
                            statusDescr = statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file")
//                        When download retry dose not return null, keep timer on and do not cancel
                        handelDownloadRetry(id, url, isJson, downloadAttempts)?.let {
                            dId = it
                            return@timer
                        }
                    }else{
                        val error = if (isStatusError) null else statusInfo?.reason ?: "downloadFile - DownloadManager failed to download file"
                        mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMsg = app.getString(
                            R.string.delivery_status_failed), statusDescr = error)
                    }
                    sendDeliveryStatus(id)
                    cancelAndLatch(this@timer, latch)
                }
            }
        }
        return dId
    }


    private fun cancelAndLatch(timerTask: TimerTask, cd: CountDownLatch){
        timerTask.cancel()
        cd.countDown()
    }
    private fun handleDownloadDone(id: String){
        Timber.d("handleDownloadDone - downloading Done")
        mapRepo.update(id = id, flowState = DeliveryFlowState.DOWNLOAD_DONE, downloadProgress = 100, statusDescr = "")
        sendDeliveryStatus(id)
        toContinue = true
        latch.countDown()
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
                        statusMsg = app.getString(R.string.error_map_already_exists),
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
    private fun handelDownloadRetry(id: String, url: String, isJson: Boolean, downloadAttempts: Int): Long? {
        Timber.i("handelDownloadRetry, id: $id, isJson: $isJson ")
        val waitTime = TimeUnit.MINUTES.toMillis(if (downloadAttempts == 0) 5 else 10)
        val startTime = System.currentTimeMillis()

        Timber.d("handelDownloadRetry try again in: ${TimeUnit.MILLISECONDS.toMinutes(waitTime)} minutes")
        val cd = CountDownLatch(1)
        var dId: Long? = null
        timer(initialDelay = 100, period = 2000) {
            val diff = System.currentTimeMillis() - startTime
            if (diff > waitTime){
                Timber.d("handelDownloadRetry - try again id: $id, isJson: $isJson")
                val mapPkg = mapRepo.getById(id) ?: return@timer

                val statusMessage = app.getString(R.string.delivery_status_failed_try_again)

                dId = if (isJson) {
                    val downloadId = downloadFile(url)
                    val attempts = ++mapPkg.metadata.jsonAttempt
                    mapRepo.update(id, JDID = downloadId, jsonAttempt = attempts, statusMsg = statusMessage)
                    downloadId
                } else {
                    val downloadId = downloadFile(url)
                    val attempts = ++mapPkg.metadata.mapAttempt
                    mapRepo.update(id, MDID = downloadId, mapAttempt = attempts, statusMsg = statusMessage)
                    downloadId
                }
                cancelAndLatch(this@timer, cd)
            }else {
                if (mapRepo.isDownloadCanceled(id)) {
                    Timber.d("handelDownloadRetry - Download $id, canceled by user")
                    if (!isJson) {
                        mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                            statusMsg = app.getString(R.string.delivery_status_canceled))
                        cancelAndLatch(this@timer, cd)
                    }
                }
                if (!isJson) {
                    val secToFinish = TimeUnit.MILLISECONDS.toSeconds(waitTime - diff)
                    mapRepo.update(id, statusMsg = app.getString(R.string.delivery_status_failed_try_again_in, secToFinish))
                }
            }
        }
        cd.await()
        return dId
    }

    private fun getMapAndValidate(id: String, downloadId: Long, isJson: Boolean): MapPkg?{
        val mapPkg = mapRepo.getById(id)

        if (mapPkg?.state == MapDeliveryState.ERROR){
            Timber.e("watchDownloadProgress download status for $id, isJson: $isJson, is ERROR. abort the process" )
            downloader.cancelDownload(downloadId)
            return null
        }
//         todo check what happen when cancel the download in the download manager
        if (mapPkg?.cancelDownload == true){
            Timber.d("downloadFile - Download $id, canceled by user")
            downloader.cancelDownload(downloadId)

            if (!isJson){
                mapRepo.update(id, state = MapDeliveryState.CANCEL, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusMsg = app.getString(
                    R.string.delivery_status_canceled),)
            }
            return null
        }
        return mapPkg
    }
}