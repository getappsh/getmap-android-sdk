package com.ngsoft.getapp.sdk.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


internal object CompressionUtils {

    fun compress(string: String): ByteArray {
        val data = string.toByteArray(charset("UTF-8"))

        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
        gzipOutputStream.write(data)
        gzipOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun decompress(compressedData: ByteArray): String {
        val byteArrayInputStream = ByteArrayInputStream(compressedData)
        val gzipInputStream = GZIPInputStream(byteArrayInputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (gzipInputStream.read(buffer).also { bytesRead = it } > 0) {
            byteArrayOutputStream.write(buffer, 0, bytesRead)
        }
        gzipInputStream.close()
        return byteArrayOutputStream.toString("UTF-8")
    }
}