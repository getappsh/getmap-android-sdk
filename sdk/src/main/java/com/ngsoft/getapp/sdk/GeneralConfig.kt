package com.ngsoft.getapp.sdk

import android.content.Context

class GeneralConfig private constructor(appContext: Context){

    companion object{
        @Volatile private var instance: GeneralConfig? = null

        fun getInstance(appContext: Context): GeneralConfig{
            if (instance == null){
                synchronized(this){
                    if (instance == null){
                        instance = GeneralConfig(appContext)
                    }
                }
            }
            return instance!!
        }
    }
    private var pref = Pref.getInstance(appContext)

    private var _baseUrl: String = pref.baseUrl
    private var _storagePath: String = pref.storagePath
    private var _downloadPath: String = pref.downloadPath
    private var _deliveryTimeout: Int = pref.deliveryTimeout
    private var _downloadTimeout: Int = pref.downloadTimeout
    private var _downloadRetry: Int = pref.downloadRetry
    private var _maxMapSizeInMB: Long = pref.maxMapSizeInMB
    private var _maxParallelDownloads: Int = pref.maxParallelDownloads
    private var _periodicForInventoryJob: Int = pref.periodicForInventoryJob
    private var _periodicForMapConf: Int = pref.periodicForMapConf
    private var _minAvailableSpace: Long = pref.minAvailableSpace


    val baseUrl: String
        get() = _baseUrl

    val storagePath: String
        get() = _storagePath

    val downloadPath: String
        get() = _downloadPath

    val deliveryTimeout: Int
        get() = _deliveryTimeout

    val downloadTimeout: Int
        get() = _downloadTimeout

    val downloadRetry: Int
        get() = _downloadRetry

    val maxMapSizeInMB: Long
        get() = _maxMapSizeInMB

    val maxParallelDownloads: Int
        get() = _maxParallelDownloads

    val periodicForInventoryJob: Int
        get() = _periodicForInventoryJob

    val periodicForMapConf: Int
        get() = _periodicForMapConf

    val minAvailableSpace: Long
        get() = _minAvailableSpace


    internal fun syncData() {
        _baseUrl = pref.baseUrl
        _storagePath = pref.storagePath
        _downloadPath = pref.downloadPath
        _deliveryTimeout = pref.deliveryTimeout
        _downloadTimeout = pref.downloadTimeout
        _downloadRetry = pref.downloadRetry
        _maxMapSizeInMB = pref.maxMapSizeInMB
        _maxParallelDownloads = pref.maxParallelDownloads
        _periodicForInventoryJob = pref.periodicForInventoryJob
        _periodicForMapConf = pref.periodicForMapConf
        _minAvailableSpace = pref.minAvailableSpace
    }
}

