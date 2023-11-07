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
            var currentDeviceId =  sharedPreferences.getString(DEVICE_ID, null)
            if (currentDeviceId == null){
                currentDeviceId =  generateDeviceId()
            }
            return currentDeviceId
        }
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putString(DEVICE_ID, value)
            editor.apply()
        }

    @SuppressLint("HardwareIds")
    private fun generateDeviceId():String {
        val newDeviceId = Secure.getString(contentResolver, Secure.ANDROID_ID).toString()
        this.deviceId = newDeviceId
        return newDeviceId
    }


    companion object {

        private const val DEVICE_ID = "device_id"

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
