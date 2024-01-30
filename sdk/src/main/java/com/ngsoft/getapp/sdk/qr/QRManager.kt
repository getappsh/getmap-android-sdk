package com.ngsoft.getapp.sdk.qr

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import timber.log.Timber

import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.ngsoft.getapp.sdk.R

import com.ngsoft.getapp.sdk.utils.CompressionUtils
import com.ngsoft.getapp.sdk.utils.EncryptionUtils
import com.ngsoft.getapp.sdk.utils.HashUtils
import org.json.JSONObject
import java.io.IOException



internal class QRManager(private val appCtx: Context) {
    private val _tag = "QRManager"

    private val checksumAlgorithm = "sha256"
    private val maxBytesSize = 2953

    private fun compressAndHashJson(jsonString: String): String{
        Timber.i("compressAndHashJson")
        Timber.d("compressAndHashJson - Original size: ${jsonString.toByteArray().size}")

        val compressed = try{
            CompressionUtils.compress(jsonString)
        }catch (exception: Exception){
            Timber.e("compressAndHashJson - failed to compress the json: ${exception.message.toString()}", )
            throw exception
        }
        Timber.d("compressAndHashJson - Compressed size: ${compressed.size}")

        val (encrypted, iv) = EncryptionUtils.encrypt(compressed)
        val ivEncoded = Base64.encodeToString(iv, Base64.DEFAULT)
        Timber.d("compressAndHashJson - iv size: ${ivEncoded.length}")
        Timber.d("compressAndHashJson - encrypted size: ${encrypted.size}")

        val hash = HashUtils.getCheckSumFromByteArray(checksumAlgorithm, encrypted){}

        val encoded = Base64.encodeToString(encrypted, Base64.DEFAULT)
        val finalSize = encoded.toByteArray().size
        Timber.d("compressAndHashJson - Encoded size: $finalSize")

        if ( finalSize >= maxBytesSize){
            Timber.e("compressAndHashJson - Final size: $finalSize, is Higher then required: $maxBytesSize.", )
            throw Exception(appCtx.getString(R.string.error_qr_code_file_size_to_large, finalSize))
        }

        val jsonContainer = JSONObject()
        jsonContainer.put("data", encoded)
        jsonContainer.put("iv", ivEncoded)
        jsonContainer.put(checksumAlgorithm, hash)

        return jsonContainer.toString()
    }

    fun generateQrCode(jsonString: String, width: Int, height: Int): Bitmap{
        Timber.i("generateQrCode")
        val compressed = compressAndHashJson(jsonString)

        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(compressed, BarcodeFormat.QR_CODE, width, height)
        return bitmap
    }

    private fun decompressAndValidateJson(jsonString: String): String{
        Timber.i("decompressAndValidateJson")

        val jsonContainer = JSONObject(jsonString)
        val data = jsonContainer.getString("data")
        Timber.v("decompressAndValidateJson - data: $data")

        val expectedHash = jsonContainer.getString(checksumAlgorithm)

        val decoded = Base64.decode(data, Base64.DEFAULT)
        Timber.v("decompressAndValidateJson - decoded: $decoded")

        val actualHash = HashUtils.getCheckSumFromByteArray(checksumAlgorithm, decoded){
        }
        Timber.d("decompressAndValidateJson - actual hash: $actualHash")
        Timber.d("decompressAndValidateJson - expected hash: $expectedHash")

        if (actualHash != expectedHash){
            Timber.e("decompressAndValidateJson - Checksum failed", )
            throw Exception("Checksum failed")
        }
        val ivEncoded = jsonContainer.getString("iv")
        val iv = Base64.decode(ivEncoded, Base64.DEFAULT)
        val decrypted = EncryptionUtils.decrypt(decoded, iv)
        try {
            return CompressionUtils.decompress(decrypted)
        } catch (io: IOException) {
            Timber.e("decompressAndValidateJson - Failed to decompress the json: ${io.message.toString()}",)
            throw io
        }
    }


    fun processQrCodeData(data: String): String{
        Timber.i("scannedCode")
        return decompressAndValidateJson(data)
    }
}