package com.ngsoft.getapp.sdk.qr

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log

import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

import com.ngsoft.getapp.sdk.utils.CompressionUtils
import com.ngsoft.getapp.sdk.utils.HashUtils
import org.json.JSONObject
import java.io.IOException



internal class QRManager {
    private val _tag = "QRManager"

    private val checksumAlgorithm = "sha256"

    private fun compressAndHashJson(jsonString: String): String{
        Log.i(_tag, "compressAndHashJson")
        Log.d(_tag, "compressAndHashJson - Original size: ${jsonString.toByteArray().size}")

        val compressed = try{
            CompressionUtils.compress(jsonString)
        }catch (exception: Exception){
            Log.e(_tag, "compressAndHashJson - failed to compress the json: ${exception.message.toString()}", )
            throw exception
        }
        Log.d(_tag, "compressAndHashJson - Compressed size: ${compressed.size}")

        val encoded =  Base64.encodeToString(compressed, Base64.DEFAULT)
        Log.d(_tag, "compressAndHashJson - Encoded size: ${encoded.toByteArray().size}")

        val hash = HashUtils.getCheckSumFromByteArray(checksumAlgorithm, compressed){}

        val jsonContainer = JSONObject()
        jsonContainer.put("data", encoded)
        jsonContainer.put(checksumAlgorithm, hash)

        return jsonContainer.toString()
    }

    fun generateQrCode(jsonString: String, width: Int, height: Int): Bitmap{
        Log.i(_tag, "generateQrCode")
        val compressed = compressAndHashJson(jsonString)

        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(compressed, BarcodeFormat.QR_CODE, width, height)
        return bitmap
    }

    private fun decompressAndValidateJson(jsonString: String): String{
        Log.i(_tag, "decompressAndValidateJson")

        val jsonContainer = JSONObject(jsonString)
        val data = jsonContainer.getString("data")
        Log.v(_tag, "decompressAndValidateJson - data: $data")

        val expectedHash = jsonContainer.getString(checksumAlgorithm)

        val decoded =  Base64.decode(data, Base64.DEFAULT)
        Log.v(_tag, "decompressAndValidateJson - decoded: $decoded")

        val actualHash = HashUtils.getCheckSumFromByteArray(checksumAlgorithm, decoded){
        }
        Log.d(_tag, "decompressAndValidateJson - actual hash: $actualHash")
        Log.d(_tag, "decompressAndValidateJson - expected hash: $expectedHash")

        if (actualHash != expectedHash){
            Log.e(_tag, "decompressAndValidateJson - Checksum failed", )
            throw Exception("Checksum failed")
        }
        try {
            return CompressionUtils.decompress(decoded)
        } catch (io: IOException) {
            Log.e(
                _tag,
                "decompressAndValidateJson - Failed to decompress the json: ${io.message.toString()}",
            )
            throw io
        }
    }
}