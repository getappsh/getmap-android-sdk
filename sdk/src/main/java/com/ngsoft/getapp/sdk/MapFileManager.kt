package com.ngsoft.getapp.sdk

import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

internal class MapFileManager(private val appCtx: Context, private val downloader: PackageDownloader) {
    private val _tag = "MapManager"

    val config: GetMapService.GeneralConfig = ServiceConfig.getInstance(appCtx)


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
        val pkgFile = File(config.downloadPath, pkgName)
        if (!pkgFile.exists() && !File(config.storagePath, pkgName).exists()){
            throw IOException("File $pkgName, doesn't exist")
        }
        val jsonFile = File(config.downloadPath, jsonName)
        if (!jsonFile.exists() && !File(config.storagePath, jsonName).exists()){
            throw IOException("File $jsonName, doesn't exist")
        }

        val newJsonName = FileUtils.changeFileExtensionToJson(pkgName)
        val names = FileUtils.getUniqueFilesName(config.storagePath, pkgName, newJsonName)

        if (!(File(config.storagePath, pkgName).exists() && names.first == pkgName)){
            if (FileUtils.getAvailableSpace(config.storagePath) <= pkgFile.length()){
                throw IOException(appCtx.getString(R.string.error_not_enough_space))
            }
            Files.move(
                Paths.get(config.downloadPath, pkgName),
                Paths.get(config.storagePath, names.first),
                StandardCopyOption.REPLACE_EXISTING)
        }


        if (!(File(config.storagePath, jsonName).exists() && names.second == jsonName)) {
            if (FileUtils.getAvailableSpace(config.storagePath) <= jsonFile.length()){
                throw IOException(appCtx.getString(R.string.error_not_enough_space))
            }
            Files.move(
                Paths.get(config.downloadPath, jsonName),
                Paths.get(config.storagePath, names.second),
                StandardCopyOption.REPLACE_EXISTING
            )

        }
        return names
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
        Log.i(_tag, "deleteFile - fileName: $fileName")
        for (path in arrayOf(config.downloadPath, config.storagePath)){
            val file = File(path, fileName)
            Log.d(_tag, "deleteFile - File path: ${file.path}")

            if (!file.exists()){
                Log.d(_tag, "deleteFile - File dose not exist. $fileName")
                continue
            }
            if (file.delete()) {
                Log.d(_tag, "deleteFile - File deleted successfully. $fileName")
            } else {
                Log.d(_tag, "deleteFile - Failed to delete the file. $fileName")
            }
        }
    }

    private fun deleteFile(file: File){
        try {
            file.delete()
        }catch (e: Exception){
            Log.e(_tag, "refreshMapState - failed to delete file: ${file.path}", )
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

        if (targetMapFile?.exists() == true && targetJsonFile?.exists() == true){
            if (mapPkg.state == MapDeliveryState.DONE){
                mapPkg.state = MapDeliveryState.DONE
                mapPkg.flowState = DeliveryFlowState.DONE
                mapPkg.statusMessage = appCtx.getString(R.string.delivery_status_done)
            }else{
                mapPkg.flowState = DeliveryFlowState.MOVE_FILES
            }
            return mapPkg
        }

        if(targetJsonFile?.exists() == true && targetMapFile?.exists() != true){
            if (downloadJsonFile?.exists() == false){
                try {
                    FileUtils.moveFile(config.storagePath, config.downloadPath, targetJsonFile.name)
                }catch (e: Exception){
                    Log.e(_tag, "refreshMapState - failed to move json file to download dir, json: ${targetJsonFile.name}, error: ${e.message.toString()}")
                }
                deleteFile(targetJsonFile)
            }else{
                deleteFile(targetJsonFile)
            }
        }

        val mapDone = isFileDownloadDone(mapPkg.MDID, mapPkg.fileName)
        val jsonDone = isFileDownloadDone(mapPkg.JDID, mapPkg.jsonName)


        mapPkg.flowState = if (mapDone && jsonDone){
            DeliveryFlowState.DOWNLOAD_DONE
        }else if (!downloader.isDownloadFailed(mapPkg.MDID) && !downloader.isDownloadFailed(mapPkg.JDID)){
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
            mapPkg.statusMessage = appCtx.getString(R.string.delivery_status_failed)
        }

        mapPkg.metadata.mapDone = mapDone
        mapPkg.metadata.jsonDone = jsonDone

        return mapPkg
    }
}