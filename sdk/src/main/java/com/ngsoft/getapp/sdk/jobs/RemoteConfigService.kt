package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient

class RemoteConfigService: JobService() {

    private val _tag = "RemoteConfigService"
    private lateinit var pref: Pref
    private lateinit var client: GetAppClient

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(_tag, "onStartJob")

        pref = Pref.getInstance(this)
        client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))

        Thread{runJob(params)}.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(_tag, "onStopJob - start again later")
        return true
    }

    private fun runJob(params: JobParameters?){
        try {
            val config = client.getMapApi.getMapControllerGetMapConfig(pref.deviceId)
            Log.v(_tag, "runJob - config: $config")
        }catch (e: Exception){
            jobFinished(params, true)
        }
    }


}