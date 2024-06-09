package com.ngsoft.getapp.sdk.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ngsoft.getapp.sdk.BuildConfig
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.SystemTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


object SystemTestReceiver: BroadcastReceiver() {

    const val ACTION_RUN_SYSTEM_TEST = "com.example.getmap.ACTION_RUN_SYSTEM_TEST"
    const val ACTION_SYSTEM_TEST_RESULTS = "com.example.getmap.ACTION_SYSTEM_TEST_RESULTS"
    const val EXTRA_TEST_RESULTS = "com.example.getmap.EXTRA_TEST_RESULTS"
    override fun onReceive(context: Context, intent: Intent) {
        println("SystemTestReceiver")
        var url = Pref.getInstance(context).baseUrl
        if (url.isEmpty()) url =
            "https://api-asio-getapp-2.apps.okd4-stage-getapp.getappstage.link"

        if (intent.action == ACTION_RUN_SYSTEM_TEST) {
            val cfg = Configuration(
                url,
                BuildConfig.USERNAME,
                BuildConfig.PASSWORD,
                16,
                null
            )

            GlobalScope.launch(Dispatchers.IO) {
                val systemTest = SystemTest.getInstance(context, cfg)

                systemTest.run { testReport ->
                    // Broadcast the test results to the second app
                    val bundle = Bundle()
                    bundle.putSerializable(EXTRA_TEST_RESULTS, testReport)

                    val broadcastIntent = Intent(ACTION_SYSTEM_TEST_RESULTS)
                    broadcastIntent.putExtra("bundle", bundle)
                    context.sendBroadcast(broadcastIntent)
                }
            }

        }
    }
}