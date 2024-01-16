package com.ngsoft.getapp.sdk

import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import java.io.File
import java.io.IOException

internal class MapFileManager(private val appCtx: Context, private val downloader: PackageDownloader, private val downloadPath: String, private val storagePath: String) {
    private val _tag = "MapManager"


    fun moveFileToTargetDir(fileName: String): String {
        val downloadFile = File(downloadPath, fileName)

//        TODO fined better way to handle when file exist and have not been downloaded
        if (!downloadFile.exists()){
            if(File(storagePath, fileName).exists()){
                return fileName
            }
            throw IOException("File $downloadFile, doesn't exist")
        }

        if (FileUtils.getAvailableSpace(storagePath) <= downloadFile.length()){
            throw IOException(appCtx.getString(R.string.error_not_enough_space))
        }

        return FileUtils.moveFile(downloadPath, storagePath, fileName)
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
        for (path in arrayOf(downloadPath, storagePath)){
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
        val downloadFile = fileName?.let { File(downloadPath, it) }
        val targetFile = fileName?.let{ File(storagePath, it) }

        return if (targetFile?.exists() == true){
            true
        }else if(downloadFile?.exists() == true){
            downloader.isDownloadDone(downloadId)
        }else{
            false
        }


    }
    fun refreshMapState(mapPkg: MapPkg): MapPkg {
        val downloadMapFile = mapPkg.fileName?.let { File(downloadPath, it) }
        val downloadJsonFile = mapPkg.jsonName?.let { File(downloadPath, it) }

        val targetMapFile = mapPkg.fileName?.let { File(storagePath, it) }
        val targetJsonFile = mapPkg.jsonName?.let { File(storagePath, it) }

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
                    FileUtils.moveFile(storagePath, downloadPath, targetJsonFile.name)
                }catch (e: Exception){
                    Log.e(_tag, "refreshMapState - failed to move json file to download dir, json: ${targetJsonFile.name}, error: ${e.message.toString()}")
                }
                deleteFile(targetJsonFile)
            }else{
                deleteFile(targetJsonFile)
            }
        }

        val mapDone = isFileDownloadDone(mapPkg.MDID, mapPkg.fileName)
        val jsonDone = isFileDownloadDone(mapPkg.JDID, mapPkg.fileName)


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