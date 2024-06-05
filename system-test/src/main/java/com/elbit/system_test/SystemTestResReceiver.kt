package com.elbit.system_test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ngsoft.getapp.sdk.SystemTest
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver.EXTRA_TEST_RESULTS

object SystemTestResReceiver: BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SystemTestResReceiver", "onReceive")
        val bundle = intent.getBundleExtra("bundle")
        val myMap: HashMap<Int, SystemTest.TestResults?> = bundle?.getSerializable(EXTRA_TEST_RESULTS) as? HashMap<Int, com.ngsoft.getapp.sdk.SystemTest.TestResults?> ?: HashMap()
        Log.d("SystemTestResReceiver", "myMap: $myMap")

        myMap.forEach { (key, value) ->
            Log.d("SystemTestResReceiver", "key: $key, value: $value")
        }

        val localIntent = Intent("ACTION_UPDATE_UI")

        localIntent.putExtra("bundle", bundle)
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
    }
}