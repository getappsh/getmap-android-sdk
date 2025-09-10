package com.ngsoft.getapp.sdk

import GetApp.Client.models.MapConfigDto
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment

import android.provider.Settings.Secure;
import com.ngsoft.getapp.sdk.exceptions.MissingIMEIException
import com.ngsoft.getapp.sdk.utils.DateHelper
import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import androidx.core.content.edit


class Pref private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences
    private val contentResolver: ContentResolver

    init {
        sharedPreferences = context.getSharedPreferences("GetApp", Context.MODE_PRIVATE)
        contentResolver = context.contentResolver
    }

    val deviceId: String
        get(){
//            var currentDeviceId = getString(DEVICE_ID, "")
            var currentDeviceId = serialNumber;
            if (currentDeviceId.isEmpty()){
                currentDeviceId = generateDeviceId()
            }
            return currentDeviceId
        }
//        set(value) = setString(DEVICE_ID, value)

    var serialNumber: String
        get() = getString(SERIAL_NUMBER, "")
        set(value) = setString(SERIAL_NUMBER, value)

    var username: String
        get() = getString(USERNAME, "")
        set(value) = setString(USERNAME, value)

    var password: String
        get() = getString(PASSWORD, "")
        set(value) = setString(PASSWORD, value)

    var baseUrl: String
        get() = getString(BASE_URL, BuildConfig.BASE_URL)
        set(value) = setString(BASE_URL, value)
    var matomoUrl: String
        get() = getString(MATOMO_URL, "https://matomo-matomo.apps.okd4-stage-getapp.getappstage.link/matomo.php")
        set(value) = setString(MATOMO_URL, value)

    var matomoDimensionId: String
        get() = getString(MATOMO_DIMENSION_ID, "1")
        set(value) = setString(MATOMO_DIMENSION_ID, value)

    var matomoSiteId: String
        get() = getString(MATOMO_SITE_ID, "1")
        set(value) = setString(MATOMO_SITE_ID, value)

    var matomoUpdateIntervalMins: Int
        get() = getInt(MATOMO_UPDATE_INTERVAL, 60)
        set(value) = setInt(MATOMO_UPDATE_INTERVAL, value)

    var sdStoragePath: String
        get() = getString(SD_STORAGE_PATH, "com.asio.gis/gis/maps/raster/מיפוי ענן")
        set(value) = setString(SD_STORAGE_PATH, value)


    var flashStoragePath: String
        get() = getString(FLASH_STORAGE_PATH, "com.asio.gis/gis/maps/raster/מיפוי ענן")
        set(value) = setString(FLASH_STORAGE_PATH, value)

    var targetStoragePolicy: MapConfigDto.TargetStoragePolicy
        get() = getEnum(TARGET_STORAGE_POLICY, MapConfigDto.TargetStoragePolicy.SDOnly)
        set(value) = setEnum(TARGET_STORAGE_POLICY, value)

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

    var maxMapAreaSqKm: Long
        get() = getLong(MAX_MAP_AREA_SQ_KM, 100)
        set(value) = setLong(MAX_MAP_AREA_SQ_KM, value)

    var maxParallelDownloads: Int
        get() = getInt(MAX_PARALLEL_DOWNLOADS, 1)
        set(value) = setInt(MAX_PARALLEL_DOWNLOADS, value)

    var periodicInventoryIntervalJob: Int
        get() = getInt(PERIODIC_INVENTORY_INTERVAL_JOB, 1440)
        set(value) = setInt(PERIODIC_INVENTORY_INTERVAL_JOB, value)

    var lastInventoryCheck: OffsetDateTime?
        get() = getOffsetDateTime(LAST_INVENTORY_CHECK)
        set(value) = setOffsetDateTime(LAST_INVENTORY_CHECK, value)

    var periodicConfIntervalJob: Int
        get() = getInt(PERIODIC_CONF_INTERVAL_JOB, 1440)
        set(value) = setInt(PERIODIC_CONF_INTERVAL_JOB, value)

    var lastConfigCheck: OffsetDateTime?
        get() = getOffsetDateTime(LAST_CONFIG_CHECK)
        set(value) = setOffsetDateTime(LAST_CONFIG_CHECK, value)

    var lastServerConfigUpdate: OffsetDateTime?
        get() = getOffsetDateTime(LAST_SERVER_CONFIG_UPDATED)
        set(value) = setOffsetDateTime(LAST_SERVER_CONFIG_UPDATED, value)

    var lastServerInventoryJob: OffsetDateTime?
        get() = getOffsetDateTime(LAST_SERVER_INVENTORY_JOB)
        set(value) = setOffsetDateTime(LAST_SERVER_INVENTORY_JOB, value)

    var applyServerConfig: Boolean
        get() = getBoolean(APPLY_SERVER_CONFIG, true)
        set(value) = setBoolean(APPLY_SERVER_CONFIG, value)

    var minAvailableSpaceMB: Long
        get() = getLong(MIN_AVAILABLE_SPACE, 500)
        set(value) = setLong(MIN_AVAILABLE_SPACE, value)

    var mapMinInclusionPct: Int
        get() = getInt(MAP_MIN_INCLUSION, 60)
        set(value) = setInt(MAP_MIN_INCLUSION, value)

    var flashInventoryMaxSizeMB: Long
        get() = getLong(FLASH_MAX_INVENTORY_SIZE_IN_MB, 2000)
        set(value) = setLong(FLASH_MAX_INVENTORY_SIZE_IN_MB, value)

    var sdInventoryMaxSizeMB: Long
        get() = getLong(SD_MAX_INVENTORY_SIZE_IN_MB, 1000)
        set(value) = setLong(SD_MAX_INVENTORY_SIZE_IN_MB, value)

    var ortophotoMapPath: String
        get() = getString(ORTHOPHOTO_MAP_PATH, "com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg")
        set(value) = setString(ORTHOPHOTO_MAP_PATH, value)


    var controlMapPath: String
        get() = getString(CONTROL_MAP_PATH, "com.asio.gis/gis/maps/orthophoto/מפת שליטה.gpkg")
        set(value) = setString(CONTROL_MAP_PATH, value)

    var ortophotoMapPattern: String
        get() = getString(ORTOPHOTO_MAP_PATTERN, "אורתופוטו")
        set(value) {
            if (value.isEmpty()){
                removeValue(ORTOPHOTO_MAP_PATTERN)
            }else {
                setString(ORTOPHOTO_MAP_PATTERN, value)
            }

        }

    var controlMapPattern: String
        get() = getString(CONTROL_MAP_PATTERN, "שליטה")
        set(value)  {
            if (value.isEmpty()){
                removeValue(CONTROL_MAP_PATTERN)
            }else{
                setString(CONTROL_MAP_PATTERN, value)
            }
        }

    private fun getString(key: String, defValue: String): String{
        return sharedPreferences.getString(key, defValue) ?: defValue
    }

    private fun setString(key: String, value: String){
        sharedPreferences.edit { putString(key, value) }
    }

    private fun removeValue(key: String){
        sharedPreferences.edit { remove(key) }
    }

    private fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    private fun setInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    private fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    private fun setLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    private fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    private fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun setOffsetDateTime(key: String, value: OffsetDateTime?){
        val valueString = value?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: return
        setString(key, valueString)
    }

    private fun getOffsetDateTime(key: String): OffsetDateTime?{
        val valueString = getString(key, "")
        return DateHelper.parse(valueString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun <T : Enum<T>> setEnum(key: String, enumValue: T) {
        sharedPreferences.edit().putString(key, enumValue.name).apply()
    }

    private inline fun <reified T : Enum<T>> getEnum(key: String, default: T): T {
        val value = sharedPreferences.getString(key, null)
        return if (value != null) {
            enumValues<T>().firstOrNull { it.name == value } ?: default
        } else {
            default
        }
    }

    @SuppressLint("HardwareIds")
    fun generateDeviceId():String {
        val newDeviceId = Secure.getString(contentResolver, Secure.ANDROID_ID).toString()
        return newDeviceId
    }

    @Throws(MissingIMEIException::class)
    fun checkDeviceIdAvailability() {
        Timber.d("checkDeviceIdAvailability")
        val pubEnv = BuildConfig.DEPLOY_ENV == "pub"

        if (serialNumber.isEmpty()) {
//            Cancel this validation, until we make sure we have permission to read the SerialNumber
//            if (!pubEnv) {
//                Timber.w("Missing DeviceID (IMEI)")
//                throw MissingIMEIException()
//            }
        }
        Timber.d("DeviceID: $deviceId")
    }

    companion object {
        private const val DEVICE_ID = "device_id"
        private const val SERIAL_NUMBER = "serial_number"
        private const val BASE_URL = "base_url"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"

        private const val MATOMO_URL = "matomo_url"
        private const val MATOMO_DIMENSION_ID = "matomoDimensionId"
        private const val MATOMO_SITE_ID = "matomoSiteId"
        private const val MATOMO_UPDATE_INTERVAL = "matomoUpdateInterval"
        private const val SD_STORAGE_PATH = "sdStoragePath"
        private const val FLASH_STORAGE_PATH = "flashStoragePath"
        private const val TARGET_STORAGE_POLICY = "targetStoragePolicy"
        private const val DOWNLOAD_PATH = "downloadPath"
        private const val DELIVERY_TIMEOUT = "deliveryTimeout"
        private const val DOWNLOAD_TIMEOUT = "downloadTimeout"
        private const val DOWNLOAD_RETRY = "downloadRetry"
        private const val MAX_MAP_SIZE_IN_MB = "maxMapSizeInMB"
        private const val MAX_MAP_AREA_SQ_KM = "maxMapSizeInMeter"
        private const val MAX_PARALLEL_DOWNLOADS = "maxParallelDownloads"
        private const val PERIODIC_INVENTORY_INTERVAL_JOB = "periodicInventorIntervalJob"
        private const val LAST_CONFIG_CHECK = "lastConfigCheck"
        private const val LAST_INVENTORY_CHECK = "lastInventoryCheck"
        private const val APPLY_SERVER_CONFIG = "applyServerConfig"
        private const val PERIODIC_CONF_INTERVAL_JOB = "periodicConfIntervalJob"
        private const val LAST_SERVER_CONFIG_UPDATED = "lastServerConfigUpdate"
        private const val LAST_SERVER_INVENTORY_JOB = "lastServerInventoryJob"
        private const val MIN_AVAILABLE_SPACE = "minAvailableSpace"
        private const val MAP_MIN_INCLUSION= "mapMinInclusionPct"
        private const val FLASH_MAX_INVENTORY_SIZE_IN_MB = "flashMaxInventorySizeInMB"
        private const val SD_MAX_INVENTORY_SIZE_IN_MB = "sdMaxInventorySizeInMB"
        private const val ORTHOPHOTO_MAP_PATH = "orthophotoMapPath"
        private const val CONTROL_MAP_PATH = "controlMapPath"
        private const val ORTOPHOTO_MAP_PATTERN = "ortophotoMapPattern"
        private const val CONTROL_MAP_PATTERN = "controlMapPattern"

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
