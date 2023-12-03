package com.ngsoft.getapp.sdk.utils

import android.os.StatFs

object FileUtils {
    fun changeFileExtensionToJson(url: String): String{
        return url.substring(0, url.lastIndexOf('.')) + ".json"
    }
    fun changeFileExtensionToJournal(name: String): String{
        return name.substring(0, name.lastIndexOf('.')) + ".gpkg-journal"
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