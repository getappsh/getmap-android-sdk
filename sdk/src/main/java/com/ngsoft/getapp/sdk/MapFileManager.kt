package com.ngsoft.getapp.sdk

import android.content.Context
import com.ngsoft.getapp.sdk.jobs.DeliveryForegroundService
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

internal class MapFileManager(private val appCtx: Context) {
    private val _tag = "MapManager"

    val config: GetMapService.GeneralConfig = ServiceConfig.getInstance(appCtx)
    private val downloader =  PackageDownloader(appCtx, config.downloadPath)
    private val mapRepo = MapRepo(appCtx)


    fun getJsonString(jsonName: String?): JSONObject?{
        jsonName ?: return null
        val targetFile = File(config.storagePath, jsonName)
        if (targetFile.exists()){
            return JsonUtils.readJson(targetFile.path)
        }
        val downloadFile = File(config.downloadPath, jsonName)
        if(downloadFile.exists()){
            return JsonUtils.readJson(downloadFile.path)
        }

        return null
    }

//    TODO clean this
    fun moveFilesToTargetDir(pkgName: String, jsonName: String): Pair<String, String>{
        //        TODO fined better way to handle when file exist and have not been downloaded
        val pkgFileD = File(config.downloadPath, pkgName)
        val pkgFileT = File(config.storagePath, pkgName)

        val pkgPath = if (pkgFileD.exists()) pkgFileD.path else {
            if (!pkgFileT.exists()) throw IOException("File $pkgName doesn't exist")
            pkgFileT.path
        }

        val jsonFileD = File(config.downloadPath, jsonName)
        val jsonFileT = File(config.storagePath, jsonName)

        val jsonPath = if (jsonFileD.exists()) jsonFileD.path else {
            if (!jsonFileT.exists()) throw IOException("File $jsonName doesn't exist")
            jsonFileT.path
        }

        val newJsonName = FileUtils.changeFileExtensionToJson(pkgName)
        val names = FileUtils.getUniqueFilesName(config.storagePath, pkgName, newJsonName)

        moveFileIfRequired(pkgPath, pkgFileT, names.first)
        moveFileIfRequired(jsonPath, jsonFileT, names.second)

        return names
    }
    private fun moveFileIfRequired(filePath: String, targetFile: File, newName: String) {
        if (!(targetFile.exists() && targetFile.name == newName)) {
            if (FileUtils.getAvailableSpace(config.storagePath) <= File(filePath).length()) {
                throw IOException(appCtx.getString(R.string.error_not_enough_space))
            }
            Files.move(
                Paths.get(filePath),
                Paths.get(config.storagePath, newName),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
    fun moveFileToTargetDir(fileName: String): String {
        val downloadFile = File(config.downloadPath, fileName)

//        TODO fined better way to handle when file exist and have not been downloaded
        if (!downloadFile.exists()){
            if(File(config.storagePath, fileName).exists()){
                return fileName
            }
            throw IOException("File $downloadFile, doesn't exist")
        }

        if (FileUtils.getAvailableSpace(config.storagePath) <= downloadFile.length()){
            throw IOException(appCtx.getString(R.string.error_not_enough_space))
        }

        return FileUtils.moveFile(config.downloadPath, config.storagePath, fileName)
    }

    @Throws(Exception::class)
    fun deleteMap(mapPkg: MapPkg?){
        if (mapPkg == null ||
            mapPkg.state == MapDeliveryState.START ||
            mapPkg.state == MapDeliveryState.DOWNLOAD ||
            mapPkg.state == MapDeliveryState.CONTINUE){

            val errorMsg = "deleteMap: Unable to delete map when status is: ${mapPkg?.state}"
            Timber.e( errorMsg)
            throw Exception(errorMsg)
        }

        mapPkg.JDID?.let { downloader.cancelDownload(it) }
        mapPkg.MDID?.let { downloader.cancelDownload(it) }

        this.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)
    }
    fun deleteMapFiles(mapName: String?, jsonName: String?){
        if (mapName != null){
            deleteFileFromAllLocations(mapName)
            val journalName = FileUtils.changeFileExtensionToJournal(mapName)
            deleteFileFromAllLocations(journalName)
        }

        if (jsonName != null){
            deleteFileFromAllLocations(jsonName)
        }
    }

    private fun deleteFileFromAllLocations(fileName: String){
        Timber.i("deleteFile - fileName: $fileName")
        for (path in arrayOf(config.downloadPath, config.storagePath)){
            val file = File(path, fileName)
            if (!file.exists()){
                Timber.d("deleteFile - File dose not exist. ${file.path}")
                continue
            }
            if (file.delete()) {
                Timber.d("deleteFile - File deleted successfully. ${file.path}")
            } else {
                Timber.d("deleteFile - Failed to delete the file. ${file.path}")
            }
        }
    }

    private fun deleteFile(file: File){
        try {
            file.delete()
        }catch (e: Exception){
            Timber.e("refreshMapState - failed to delete file: ${file.path}", )
        }
    }

    fun isFileDownloadDone(downloadId: Long?, fileName: String?): Boolean{
        val downloadFile = fileName?.let { File(config.downloadPath, it) }
        val targetFile = fileName?.let{ File(config.storagePath, it) }

        return if (targetFile?.exists() == true){
            true
        }else if(downloadFile?.exists() == true){
            downloader.isDownloadDone(downloadId)
        }else{
            false
        }


    }
    fun refreshMapState(mapPkg: MapPkg): MapPkg {
        val downloadMapFile = mapPkg.fileName?.let { File(config.downloadPath, it) }
        val downloadJsonFile = mapPkg.jsonName?.let { File(config.downloadPath, it) }

        val targetMapFile = mapPkg.fileName?.let { File(config.storagePath, it) }
        val targetJsonFile = mapPkg.jsonName?.let { File(config.storagePath, it) }

        if (targetMapFile?.exists() == true && targetJsonFile?.exists() == true) {
            if (mapPkg.state == MapDeliveryState.DONE) {
                mapPkg.state = MapDeliveryState.DONE
                mapPkg.flowState = DeliveryFlowState.DONE
                mapPkg.statusMsg = appCtx.getString(R.string.delivery_status_done)
            } else {
                mapPkg.flowState = DeliveryFlowState.MOVE_FILES
            }
            return mapPkg
        }

        if(targetJsonFile?.exists() != true && targetMapFile?.exists() == true){
            if (downloadMapFile?.exists() == false){
                try {
                    FileUtils.moveFile(config.storagePath, config.downloadPath, targetMapFile.name)
                }catch (e: Exception){
                    Timber.e("refreshMapState - failed to move gpkg file to download dir, file: ${targetMapFile.name}, error: ${e.message.toString()}")
                    deleteFile(targetMapFile)
                }
            }else{
                deleteFile(targetMapFile)

            }
        }
        if(targetJsonFile?.exists() == true && targetMapFile?.exists() != true){
            if (downloadJsonFile?.exists() == false){
                try {
                    FileUtils.moveFile(config.storagePath, config.downloadPath, targetJsonFile.name)
                }catch (e: Exception){
                    Timber.e("refreshMapState - failed to move json file to download dir, json: ${targetJsonFile.name}, error: ${e.message.toString()}")
                    deleteFile(targetJsonFile)
                }
            }else{
                deleteFile(targetJsonFile)
            }
        }

        val mapDone = isFileDownloadDone(mapPkg.MDID, mapPkg.fileName)
        val jsonDone = isFileDownloadDone(mapPkg.JDID, mapPkg.jsonName)


        mapPkg.flowState = if (mapDone && jsonDone){
            DeliveryFlowState.DOWNLOAD_DONE
        }else if (!downloader.isDownloadFailed(mapPkg.MDID) && !downloader.isDownloadFailed(mapPkg.JDID) &&
            downloadJsonFile?.exists() == true && downloadMapFile?.exists() == true){
            DeliveryFlowState.DOWNLOAD
        } else if(mapPkg.url != null) {
            DeliveryFlowState.IMPORT_DELIVERY
        }else if(mapPkg.reqId != null){
            DeliveryFlowState.IMPORT_CREATE
        }else{
            DeliveryFlowState.START
        }

        if (mapPkg.state != MapDeliveryState.CANCEL && mapPkg.state != MapDeliveryState.PAUSE){
            mapPkg.state = MapDeliveryState.ERROR
            mapPkg.statusMsg = appCtx.getString(R.string.delivery_status_failed)
        }

        mapPkg.metadata.mapDone = mapDone
        mapPkg.metadata.jsonDone = jsonDone

        return mapPkg
    }


    fun synchronizeMapData(){
        Timber.d("synchronizeMapData")
        syncDatabase ()
        syncStorage()
    }
    private fun syncDatabase (){
        Timber.i("syncDatabase")
        val mapsData = this.mapRepo.getAll().filter { it.state == MapDeliveryState.DONE ||
                it.state == MapDeliveryState.ERROR || it.state == MapDeliveryState.CANCEL ||
                it.state == MapDeliveryState.PAUSE || it.state == MapDeliveryState.DELETED ||
                it.state == MapDeliveryState.DOWNLOAD}

        for (map in mapsData) {
            Timber.d("syncDatabase  - map id: ${map.id}, state: ${map.state}")

            if (map.state == MapDeliveryState.DELETED) {
                this.deleteMapFiles(map.fileName, map.jsonName)
                continue
            }

            val rMap = this.refreshMapState(map.copy())
            if(map.state == MapDeliveryState.DOWNLOAD || map.state == MapDeliveryState.CONTINUE){
                if ((rMap.metadata.mapDone ||!downloader.isDownloadFailed(map.MDID)) &&
                    (rMap.metadata.jsonDone || !downloader.isDownloadFailed(map.JDID))){
                    continue
                }
            }

            this.mapRepo.update(map.id.toString(), state = rMap.state, flowState = rMap.flowState, statusDescr = rMap.statusDescr,
                statusMsg = rMap.statusMsg, mapDone = rMap.metadata.mapDone, jsonDone = rMap.metadata.jsonDone)
        }
    }
    private fun syncStorage(){
        Timber.i("syncStorage")
        val dir =  File(config.storagePath)
        val mapFiles = dir.listFiles { _, name -> name.endsWith(FileUtils.MAP_EXTENSION) }
        val jsonFiles = dir.listFiles { _, name -> name.endsWith(FileUtils.JSON_EXTENSION) }
        val journalFiles = dir.listFiles { _, name -> name.endsWith(FileUtils.JOURNAL_EXTENSION) }

//        delete map file when there is no corresponding json file and no record in the DB
        mapFiles?.forEach { file ->
            val correspondingJsonFile = File(FileUtils.changeFileExtensionToJson(file.absolutePath))
            if (!this.mapRepo.doesMapFileExist(file.name) && !correspondingJsonFile.exists()) {
                Timber.d("syncStorage - Not found corresponding json file for mapFile: ${file.name}, delete it.")
                file.delete()
            }
        }
//        delete journal file when there is no corresponding map file
        journalFiles?.forEach { file ->
            val correspondingMapFile = File(FileUtils.changeFileExtensionToMap(file.absolutePath))
            if (!correspondingMapFile.exists()) {
                Timber.d("syncStorage - Not found corresponding map file for journalFile: ${file.name}, delete it.")
                file.delete()
            }
        }

        jsonFiles?.forEach { file ->
            if (!this.mapRepo.doesJsonFileExist(file.name)) {
                Timber.d("syncStorage - found json file not in the inventory, fileName: ${file.name}. insert it.")

                val pId: String; val bBox: String; val url: String?;
                try{
                    val json = JsonUtils.readJson(file.path)
                    pId = json.getString("id")
                    bBox = json.getString("productBoundingBox")
                    url =  if (json.has("downloadUrl")) json.getString("downloadUrl") else null
                }catch (e: JSONException){
                    Timber.e("syncStorage - not valid json object: ${e.message.toString()}")
                    Timber.d("syncStorage - delete json file: ${file.name}")
                    file.delete()
                    return@forEach
                }

                val mapPkg = this.refreshMapState(MapPkg(
                    pId = pId,
                    bBox = bBox,
                    state = MapDeliveryState.ERROR,
                    flowState = DeliveryFlowState.MOVE_FILES,
                    url = url,
                    fileName = FileUtils.changeFileExtensionToMap(file.name),
                    jsonName = file.name,
                    statusMsg = appCtx.getString(R.string.delivery_status_in_verification)
                ))

                val id = this.mapRepo.save(mapPkg)
                Timber.d("syncStorage - new map id: $id, deliveryFlowState: ${mapPkg.flowState} ")
                if (mapPkg.flowState == DeliveryFlowState.MOVE_FILES){
                    Timber.i("syncStorage - execute delivery flow for map id: $id")
                    try {
                        DeliveryForegroundService.startForId(appCtx, id)
                    }catch (e: Exception){
                        Timber.e("Failed to start the service, error: ${e.message}")

                    }
                }
            }
        }
    }
}