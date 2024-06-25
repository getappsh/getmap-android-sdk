package com.elbit.system_test

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {

    private const val PREFS_NAME = "prefs"
    private const val START_TEST_TIME_KEY = "start_test_time"

    fun writeStartTestTime(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val currentTime = System.currentTimeMillis()
        editor.putLong(START_TEST_TIME_KEY, currentTime)
        editor.apply()
    }

    fun readCurrentTime(context: Context): Long {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(START_TEST_TIME_KEY, 0)
    }
}
