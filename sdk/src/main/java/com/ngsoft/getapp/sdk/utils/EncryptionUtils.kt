package com.ngsoft.getapp.sdk.utils

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.ngsoft.getapp.sdk.BuildConfig

internal object EncryptionUtils {

    private const val TRANSFORMATION = "AES/CBC/PKCS5PADDING"
    fun encrypt(dataToEncrypt: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = generateKey()

        cipher.init(Cipher.ENCRYPT_MODE, key)
        return Pair(cipher.doFinal(dataToEncrypt), cipher.iv)
    }


    fun decrypt(dataToDecrypt: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = generateKey()
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(dataToDecrypt)
    }


    private fun generateKey(): SecretKeySpec {
        val bytes = BuildConfig.ENCRYPTION_KEY.toByteArray()
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes, 0, bytes.size)
        val key = digest.digest()
        return SecretKeySpec(key, "AES")
    }

}