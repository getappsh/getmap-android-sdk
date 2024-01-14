package com.ngsoft.getapp.sdk

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences

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
            var currentDeviceId = getString(DEVICE_ID)
            if (currentDeviceId.isNotEmpty()){
                currentDeviceId =  generateDeviceId()
            }
            return currentDeviceId
        }
        set(value) = setString(DEVICE_ID, value)


    var username: String
        get() = getString(USERNAME)
        set(value) = setString(USERNAME, value)

    var password: String
        get() = getString(PASSWORD)
        set(value) = setString(PASSWORD, value)

    var baseUrl: String
        get() = getString(BASE_URL)
        set(value) = setString(BASE_URL, value)
    private fun getString(key: String): String{
        return sharedPreferences.getString(key, "")!!
    }

    private fun setString(key: String, value: String){
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
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
