package com.ngsoft.getapp.sdk.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest


internal object HashUtils {

    private const val STREAM_BUFFER_LENGTH = 1024
    private const val UPDATE_INTERVAL = 1024 * 1024 * 5

    fun getCheckSumFromByteArray(algorithm: String, data: ByteArray, progressCallback: ((Int) -> Unit)? = null): String{
        val digest = MessageDigest.getInstance(algorithm)
        val bs = data.inputStream()
        val byteArray = updateDigest(digest, bs, progressCallback).digest()
        bs.close()
        val hexCode = StringUtils.encodeHex(byteArray, true)
        return String(hexCode)
    }

    fun getCheckSumFromFile(algorithm: String, filePath: String, progressCallback: ((Int) -> Unit)? = null): String {
        val file = File(filePath)
        return getCheckSumFromFile(algorithm, file, progressCallback)
    }

    fun getCheckSumFromFile(algorithm: String, file: File, progressCallback: ((Int) -> Unit)? = null): String {
        val digest = MessageDigest.getInstance(algorithm)
        val fis = FileInputStream(file)
        val byteArray = updateDigest(digest, fis, progressCallback).digest()
        fis.close()
        val hexCode = StringUtils.encodeHex(byteArray, true)
        return String(hexCode)
    }

    /**
     * Reads through an InputStream and updates the digest for the data
     *
     * @param digest The MessageDigest to use (e.g. sha256)
     * @param data Data to digest
     * @param progressCallback Callback function to receive progress updates. It takes an integer parameter
     * representing the progress percentage.
     * Example: { progress -> println("Progress: $progress%") }
     * @return the digest
     */
    private fun updateDigest(digest: MessageDigest, data: InputStream, progressCallback: ((Int) -> Unit)?): MessageDigest {
        val buffer = ByteArray(STREAM_BUFFER_LENGTH)
        val fileSize = data.available().toLong()
        var read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        var totalBytesRead = 0L
        var lastUpdate = 0L


        while (read > -1) {
            digest.update(buffer, 0, read)
            totalBytesRead += read

            if (totalBytesRead - lastUpdate >= UPDATE_INTERVAL) {
                val progress = ((totalBytesRead * 100) / fileSize).toInt()
                progressCallback?.invoke(progress)
                lastUpdate = totalBytesRead
            }

            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        }
        val progress = ((totalBytesRead * 100) / fileSize).toInt()
        progressCallback?.invoke(progress)

        return digest
    }

}