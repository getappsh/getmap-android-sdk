package com.ngsoft.getapp.sdk.utils

import android.os.StatFs
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

internal object FileUtils {

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

    fun moveFile(from: String?, to: String, fileName: String): String{
        val uniqueFileName = getUniqueFileName(to, fileName)

        Files.move(
            Paths.get(from, fileName),
            Paths.get(to, uniqueFileName),
            StandardCopyOption.REPLACE_EXISTING)

        return uniqueFileName
    }
    fun writeFile(path: String, fileName: String, data: String): String{
        val uniqueFileName = getUniqueFileName(path, fileName)
        File(path, uniqueFileName).writeText(data)
        return uniqueFileName
    }

    fun getUniqueFilesName(path: String, fileName1: String, fileName2: String): Pair<String, String>{
        var fileNumber = 0
        var uniqueFileName1 = fileName1
        var uniqueFileName2 = fileName2

        while (File(path, uniqueFileName1).exists() || File(path, uniqueFileName2).exists()){
            fileNumber++
            uniqueFileName1 = incrementFileName(fileName1, fileNumber)
            uniqueFileName2 = incrementFileName(fileName2, fileNumber)
        }
        return Pair(uniqueFileName1, uniqueFileName2)
    }
    fun getUniqueFileName(path: String, fileName: String): String{
        var fileNumber = 0
        var uniqueFileName = fileName
        while (File(path, uniqueFileName).exists()){
            fileNumber++
            uniqueFileName = incrementFileName(fileName, fileNumber)
        }
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