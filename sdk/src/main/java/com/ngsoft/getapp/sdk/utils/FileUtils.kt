package com.ngsoft.getapp.sdk.utils

import android.os.StatFs
import java.io.File

object FileUtils {

    const val MAP_EXTENSION = ".gpkg"
    const val JSON_EXTENSION = ".json"
    const val JOURNAL_EXTENSION = ".gpkg-journal"

    fun changeFileExtensionToMap(url: String): String{
        return url.substring(0, url.lastIndexOf('.')) + MAP_EXTENSION
    }
    fun changeFileExtensionToJson(url: String): String{
        return url.substring(0, url.lastIndexOf('.')) + JSON_EXTENSION
    }
    fun changeFileExtensionToJournal(name: String): String{
        return name.substring(0, name.lastIndexOf('.')) + JOURNAL_EXTENSION
    }

    fun getFileNameFromUri(url: String): String {
        return url.substring( url.lastIndexOf('/') + 1, url.length)
    }

    fun getAvailableSpace(path: String): Long {
        val statFs = StatFs(path)
        val blockSize = statFs.blockSizeLong
        val availableBlocks = statFs.availableBlocksLong
        return blockSize * availableBlocks // bytes available
    }

    fun writeFile(path: String, fileName: String, data: String): String{
        var fileNumber = 0
        var uniqueFileName = fileName
        while (File(path, uniqueFileName).exists()){
           fileNumber++
           uniqueFileName = incrementFileName(fileName, fileNumber)
        }


        File(path, uniqueFileName).writeText(data)
        return uniqueFileName
    }

    private fun incrementFileName(fileName: String, number: Int): String {
        val dotIndex = fileName.lastIndexOf('.')
        val name = if (dotIndex != -1) {
            fileName.substring(0, dotIndex)
        } else {
            fileName
        }

        val extension = if (dotIndex != -1 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex)
        } else {
            ""
        }
        return "${name}-$number$extension"
    }
}