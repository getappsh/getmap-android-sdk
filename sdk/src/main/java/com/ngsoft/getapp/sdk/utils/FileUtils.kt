package com.ngsoft.getapp.sdk.utils

import android.os.StatFs

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
}