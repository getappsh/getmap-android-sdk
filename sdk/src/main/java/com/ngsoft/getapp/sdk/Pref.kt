package com.ngsoft.getapp.sdk

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment

import android.provider.Settings.Secure;


class Pref private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences
    private val contentResolver: ContentResolver

    init {
        sharedPreferences = context.getSharedPreferences("GetApp", Context.MODE_PRIVATE)
        contentResolver = context.contentResolver
    }

    var deviceId: String
        get(){
            var currentDeviceId = getString(DEVICE_ID, "")
            if (currentDeviceId.isNotEmpty()){
                currentDeviceId =  generateDeviceId()
            }
            return currentDeviceId
        }
        set(value) = setString(DEVICE_ID, value)


    var username: String
        get() = getString(USERNAME, "")
        set(value) = setString(USERNAME, value)

    var password: String
        get() = getString(PASSWORD, "")
        set(value) = setString(PASSWORD, value)

    var baseUrl: String
        get() = getString(BASE_URL, "")
        set(value) = setString(BASE_URL, value)

    var storagePath: String
        get() = getString(STORAGE_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path)
        set(value) = setString(STORAGE_PATH, value)

    var downloadPath: String
        get() = getString(DOWNLOAD_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)
        set(value) = setString(DOWNLOAD_PATH, value)

    var deliveryTimeout: Int
        get() = getInt(DELIVERY_TIMEOUT, 30)
        set(value) = setInt(DELIVERY_TIMEOUT, value)

    var downloadTimeout: Int
        get() = getInt(DOWNLOAD_TIMEOUT, 30)
        set(value) = setInt(DOWNLOAD_TIMEOUT, value)

    var downloadRetry: Int
        get() = getInt(DOWNLOAD_RETRY, 2)
        set(value) = setInt(DOWNLOAD_RETRY, value)

    var maxMapSizeInMB: Long
        get() = getLong(MAX_MAP_SIZE_IN_MB, 500)
        set(value) = setLong(MAX_MAP_SIZE_IN_MB, value)

    var maxParallelDownloads: Int
        get() = getInt(MAX_PARALLEL_DOWNLOADS, 1)
        set(value) = setInt(MAX_PARALLEL_DOWNLOADS, value)

    var periodicForInventoryJob: Int
        get() = getInt(PERIODIC_FOR_INVENTORY_JOB, 1440)
        set(value) = setInt(PERIODIC_FOR_INVENTORY_JOB, value)

    var periodicForMapConf: Int
        get() = getInt(PERIODIC_FOR_MAP_CONF, 1440)
        set(value) = setInt(PERIODIC_FOR_MAP_CONF, value)

    var minAvailableSpace: Long
        get() = getLong(MIN_AVAILABLE_SPACE, 250 * 1024L * 1024L)
        set(value) = setLong(MIN_AVAILABLE_SPACE, value)

    private fun getString(key: String, defValue: String): String{
        return sharedPreferences.getString(key, defValue) ?: defValue
    }

    private fun setString(key: String, value: String){
        sharedPreferences.edit().putString(key, value).apply()
    }


    private fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    private fun setInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    private fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    private fun setLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    @SuppressLint("HardwareIds")
    fun generateDeviceId():String {
        val newDeviceId = Secure.getString(contentResolver, Secure.ANDROID_ID).toString()
        this.deviceId = newDeviceId
        return newDeviceId
    }


    companion object {
        private const val DEVICE_ID = "device_id"
        private const val BASE_URL = "base_url"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"

        const val STORAGE_PATH = "storagePath"
        const val DOWNLOAD_PATH = "downloadPath"
        const val DELIVERY_TIMEOUT = "deliveryTimeout"
        const val DOWNLOAD_TIMEOUT = "downloadTimeout"
        const val DOWNLOAD_RETRY = "downloadRetry"
        const val MAX_MAP_SIZE_IN_MB = "maxMapSizeInMB"
        const val MAX_PARALLEL_DOWNLOADS = "maxParallelDownloads"
        const val PERIODIC_FOR_INVENTORY_JOB = "periodicForInventoryJob"
        const val PERIODIC_FOR_MAP_CONF = "periodicForMapConf"
        const val MIN_AVAILABLE_SPACE = "minAvailableSpace"


        private var instance: Pref? = null
        @Synchronized
        fun getInstance(context: Context): Pref {
            if (instance == null) {
                instance = Pref(context.applicationContext)
            }
            return instance!!
        }
    }
}
