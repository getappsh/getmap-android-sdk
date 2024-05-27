package com.ngsoft.getapp.sdk

import GetApp.Client.models.MapConfigDto
import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.content.Context.STORAGE_SERVICE
import android.os.Build
import com.ngsoft.getapp.sdk.jobs.DeliveryForegroundService
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.FootprintUtils
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.max

class MapFileManager(private val appCtx: Context) {

    val config: GetMapService.GeneralConfig = ServiceConfig.getInstance(appCtx)
    private val downloader =  PackageDownloader(appCtx, config.downloadPath)
    private val mapRepo = MapRepo(appCtx)

    private val storageManager = appCtx.getSystemService(STORAGE_SERVICE) as StorageManager

    val flashTargetDir: File
        get(){
            return File(getBaseStorageDir(true), config.flashStoragePath)
        }

    val sdTargetDir: File
        get() {
            return File(getBaseStorageDir(false) ?: getBaseStorageDir(true), config.sdStoragePath)
        }

    private fun getBaseStorageDir(flash: Boolean): File? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
//                TODO use the actual path
            Environment.getExternalStorageDirectory()
        }else{
            val storageList = storageManager.storageVolumes
            return if (flash){
                storageList.getOrNull(0)?.directory?.absoluteFile
            }else{
                storageList.getOrNull(1)?.directory?.absoluteFile
            }
        }
    }

    internal fun getJsonString(dirPath: String?, jsonName: String?): JSONObject?{
        jsonName ?: return null
        val targetFile = File(dirPath, jsonName)
        if (targetFile.exists()){
            return JsonUtils.readJson(targetFile.path)
        }
        val downloadFile = File(config.downloadPath, jsonName)
        if(downloadFile.exists()){
            return JsonUtils.readJson(downloadFile.path)
        }

        return null
    }

    fun getAvailableSpaceByPolicy(): Long {
        val flashRoot: File? = getBaseStorageDir(true)
        val sdRoot: File? = getBaseStorageDir(false) ?: flashRoot

        val flashSpace = flashRoot?.path?.let { FileUtils.getAvailableSpace(it) } ?: 0
        val sdSpace = sdRoot?.path?.let { FileUtils.getAvailableSpace(it) } ?: 0

        return when(config.targetStoragePolicy){
            MapConfigDto.TargetStoragePolicy.sDOnly -> sdSpace
            MapConfigDto.TargetStoragePolicy.flashThenSD -> flashSpace + sdSpace
            MapConfigDto.TargetStoragePolicy.sDThenFlash -> flashSpace + sdSpace
            MapConfigDto.TargetStoragePolicy.flashOnly -> flashSpace
        }
    }

    fun isInventorySizeExceedingPolicy(): Boolean {
        return when(config.targetStoragePolicy){
            MapConfigDto.TargetStoragePolicy.sDOnly ->
                isInventorySizeExceeded(false)
            MapConfigDto.TargetStoragePolicy.flashThenSD ->
                isInventorySizeExceeded(true) || isInventorySizeExceeded(false)
            MapConfigDto.TargetStoragePolicy.sDThenFlash ->
                isInventorySizeExceeded(false) || isInventorySizeExceeded(true)
            MapConfigDto.TargetStoragePolicy.flashOnly ->
                isInventorySizeExceeded(true)
        }
    }
    internal fun getInventorySize(flash: Boolean): Long {
        val baseDir = if (flash) flashTargetDir else sdTargetDir
        val files = this.mapRepo.getAll()
            .filter { it.path?.startsWith(baseDir.path) == true }
            .map { mapPkg ->
                listOfNotNull(
                    mapPkg.fileName?.let { Paths.get(mapPkg.path, it) },
                    mapPkg.fileName?.let { Paths.get(mapPkg.path, FileUtils.changeFileExtensionToJournal(it)) },
                    mapPkg.jsonName?.let { Paths.get(mapPkg.path, it) })
                .map { it.toString() }
            }
            .flatten()

        return FileUtils.sumFileSize(files)
    }

    internal fun getAndValidateStorageDirByPolicy(neededSpace: Long): File{
        val flashDir = flashTargetDir
        val sdDir = sdTargetDir

        flashDir.mkdirs()
        sdDir.mkdirs()

        return when(config.targetStoragePolicy){
            MapConfigDto.TargetStoragePolicy.sDOnly -> {
                validateSpace(sdDir, neededSpace)
                validateInventorySize(true)
                sdDir
            }
            MapConfigDto.TargetStoragePolicy.flashOnly -> {
                validateSpace(flashDir, neededSpace)
                validateInventorySize(false)
                flashDir
            }
            MapConfigDto.TargetStoragePolicy.flashThenSD -> {
             if(FileUtils.getAvailableSpace(flashDir.path) > max(config.minAvailableSpaceMB * 1024 * 1024, neededSpace) &&
                 getInventorySize(true) <= (config.flashInventoryMaxSizeMB * 1024 * 1024)) {
                 flashDir
             }else {
                 validateSpace(sdDir, neededSpace)
                 validateInventorySize(false)
                 Timber.i("Not enough space in Flash save to SD")
                 sdDir
             }
            }
            MapConfigDto.TargetStoragePolicy.sDThenFlash -> {
                if(FileUtils.getAvailableSpace(sdDir.path) > neededSpace &&
                    getInventorySize(false)  <= (config.sdInventoryMaxSizeMB * 1024 * 1024)) {
                    sdDir
                }else {
                    validateSpace(flashDir, neededSpace)
                    validateInventorySize(true)
                    Timber.i("Not enough space in SD save to Flash")
                    flashDir
                }
            }
        }
    }

    private fun validateSpace(directory: File, neededSpace: Long) {
        if (FileUtils.getAvailableSpace(directory.path) <= neededSpace) {
            Timber.e("Not enough space in ${directory.path}, needed: $neededSpace")
            throw IOException(appCtx.getString(R.string.error_not_enough_space))
        }
    }

    private fun validateInventorySize(flash: Boolean){
        if (isInventorySizeExceeded(flash)){
            Timber.e("Inventory size exceeded. (flash: $flash)")
            throw IOException(appCtx.getString(R.string.error_max_inventory_size))
        }
    }

    private fun isInventorySizeExceeded(flash: Boolean): Boolean {
        val size = if (flash) config.flashInventoryMaxSizeMB else config.sdInventoryMaxSizeMB
        return getInventorySize(flash) > (size * 1024 * 1024)
    }

//    TODO clean this
    internal fun moveFilesToTargetDir(pkgName: String, jsonName: String): Pair<File, File>{
        //        TODO fined better way to handle when file exist and have not been downloaded
        val pkgFileD = File(config.downloadPath, pkgName)
        val jsonFileD = File(config.downloadPath, jsonName)

        val targetDir = getAndValidateStorageDirByPolicy(pkgFileD.length() + jsonFileD.length())
        Timber.d("Storage dir ${targetDir.path}")

        val pkgFileT = File(targetDir, pkgName)
        val jsonFileT = File(targetDir, jsonName)

        val pkgPath = if (pkgFileD.exists()) pkgFileD.path else {
            if (!pkgFileT.exists()) throw IOException("File $pkgName doesn't exist")
            pkgFileT.path
        }

        val jsonPath = if (jsonFileD.exists()) jsonFileD.path else {
            if (!jsonFileT.exists()) throw IOException("File $jsonName doesn't exist")
            jsonFileT.path
        }

        val jsonNameT = FileUtils.changeFileExtensionToJson(pkgName)
        val names = FileUtils.getUniqueFilesName(targetDir.path, pkgName, jsonNameT)

        moveFileIfRequired(pkgPath, pkgFileT, names.first)
        moveFileIfRequired(jsonPath, jsonFileT, names.second)

        return Pair(File(pkgFileT.parent, names.first), File(jsonFileT, names.second))
    }
    private fun moveFileIfRequired(sourcePath: String, targetFile: File, newName: String) {
        if (!(targetFile.exists() && targetFile.name == newName)) {
//            TODO `targetFile.parent` == null
            validateSpace(File(targetFile.parent), File(sourcePath).length())
            Files.move(
                Paths.get(sourcePath),
                Paths.get(targetFile.parent, newName),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
//    fun moveFileToTargetDir(fileName: String): String {
//        val downloadFile = File(config.downloadPath, fileName)
//
////        TODO fined better way to handle when file exist and have not been downloaded
//        if (!downloadFile.exists()){
//            if(File(config.storagePath, fileName).exists()){
//                return fileName
//            }
//            throw IOException("File $downloadFile, doesn't exist")
//        }
//
//        if (FileUtils.getAvailableSpace(config.storagePath) <= downloadFile.length()){
//            throw IOException(appCtx.getString(R.string.error_not_enough_space))
//        }
//
//        return FileUtils.moveFile(config.downloadPath, config.storagePath, fileName)
//    }

    @Throws(Exception::class)
    internal fun deleteMap(mapPkg: MapPkg?){
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
    internal fun deleteMapFiles(mapName: String?, jsonName: String?){
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
        for (path in arrayOf(config.downloadPath, flashTargetDir.path, sdTargetDir.path)){
            val file = File(path, fileName)
            if (!file.exists()){
                Timber.v("deleteFile - File dose not exist. ${file.path}")
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

    internal fun isFileDownloadDone(downloadId: Long?, downloadFile: File?, targetFile: File?): Boolean{
        return if (targetFile?.exists() == true){
            true
        }else if(downloadFile?.exists() == true){
            downloader.isDownloadDone(downloadId)
        }else{
            false
        }
    }
    internal fun refreshMapState(mapPkg: MapPkg): MapPkg {
        val downloadMapFile = mapPkg.fileName?.let { File(config.downloadPath, it) }
        val downloadJsonFile = mapPkg.jsonName?.let { File(config.downloadPath, it) }


//        TODO Use only the mapPkg.path if exists?
        val possibleTargetDirs =  listOf(mapPkg.path, flashTargetDir.path, sdTargetDir.path)
        val targetMapFile = mapPkg.fileName?.let { fileName ->
            possibleTargetDirs.mapNotNull { File(it, fileName) }.firstOrNull(File::exists)
        }
        val targetJsonFile = mapPkg.jsonName?.let {fileName ->
            targetMapFile?.let { File(it.parent, fileName) }
                ?: possibleTargetDirs.mapNotNull { File(it, fileName) }.firstOrNull(File::exists)
        }

        if (targetJsonFile?.exists() == true) {
            val json = JsonUtils.readJson(targetJsonFile.path)
            mapPkg.footprint = FootprintUtils.toString(json.getJSONObject("footprint"))
        }

        if (targetMapFile?.exists() == true && targetJsonFile?.exists() == true) {
            mapPkg.path = targetMapFile.parent
            mapPkg.metadata.mapDone = true
            mapPkg.metadata.jsonDone = true

            val lastModified = targetMapFile.lastModified().coerceAtLeast(targetJsonFile.lastModified())
            mapPkg.downloadDone = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneOffset.UTC)
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
                    FileUtils.moveFile(targetMapFile.parent, config.downloadPath, targetMapFile.name)
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
                    FileUtils.moveFile(targetJsonFile.parent, config.downloadPath, targetJsonFile.name)
                }catch (e: Exception){
                    Timber.e("refreshMapState - failed to move json file to download dir, json: ${targetJsonFile.name}, error: ${e.message.toString()}")
                    deleteFile(targetJsonFile)
                }
            }else{
                deleteFile(targetJsonFile)
            }
        }

        val mapDone = isFileDownloadDone(mapPkg.MDID, downloadMapFile, targetMapFile)
        val jsonDone = isFileDownloadDone(mapPkg.JDID, downloadJsonFile, targetJsonFile)

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


    internal fun synchronizeMapData(){
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
                statusMsg = rMap.statusMsg, mapDone = rMap.metadata.mapDone, jsonDone = rMap.metadata.jsonDone, path=rMap.path,
                downloadDone = rMap.downloadDone)
            if (map.footprint != rMap.footprint){
                rMap.footprint?.let { this.mapRepo.setFootprint(map.id.toString(), it) }
            }
        }
    }

    private fun syncStorage(){
        Timber.i("syncStorage")
        val mapFiles = (flashTargetDir.listFiles { _, name -> name.endsWith(FileUtils.MAP_EXTENSION) } ?: arrayOf<File>()) +
                (sdTargetDir.listFiles { _, name -> name.endsWith(FileUtils.MAP_EXTENSION) } ?: arrayOf<File>())

        val jsonFiles = (flashTargetDir.listFiles { _, name -> name.endsWith(FileUtils.JSON_EXTENSION) } ?: arrayOf<File>()) +
                (sdTargetDir.listFiles { _, name -> name.endsWith(FileUtils.JSON_EXTENSION) } ?: arrayOf<File>())

        val journalFiles = (flashTargetDir.listFiles { _, name -> name.endsWith(FileUtils.JOURNAL_EXTENSION) } ?: arrayOf<File>()) +
                (sdTargetDir.listFiles { _, name -> name.endsWith(FileUtils.JOURNAL_EXTENSION) } ?: arrayOf<File>())

//        delete map file when there is no corresponding json file and no record in the DB
        mapFiles.forEach { file ->
            val correspondingJsonFile = File(FileUtils.changeFileExtensionToJson(file.absolutePath))
            if (!this.mapRepo.doesMapFileExist(file.name) && !correspondingJsonFile.exists()) {
                Timber.d("syncStorage - Not found corresponding json file for mapFile: ${file.name}, delete it.")
                file.delete()
            }
        }
//        delete journal file when there is no corresponding map file
        journalFiles.forEach { file ->
            val correspondingMapFile = File(FileUtils.changeFileExtensionToMap(file.absolutePath))
            if (!correspondingMapFile.exists()) {
                Timber.d("syncStorage - Not found corresponding map file for journalFile: ${file.name}, delete it.")
                file.delete()
            }
        }

        jsonFiles.forEach { file ->
//            TODO dose json file exist, query also for path
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
                    path = file.path,
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