package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.ngsoft.getapp.sdk.ServiceConfig
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.helpers.client.ConfigClient
import com.ngsoft.getapp.sdk.helpers.NotificationHelper
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import java.util.concurrent.TimeUnit

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

        repeat(3){index->
            try {
                Log.d(_tag, "runJob - retry $index")
                ConfigClient.fetchUpdates(client, ServiceConfig.getInstance(this), pref.deviceId)
                jobFinished(params, false)
                return
            }catch (e: Exception){
                Log.e(_tag, "runJob - Failed to get config updates, error ${e.message.toString()}")
                TimeUnit.SECONDS.sleep(5L  * (index + 1))
            }
        }
        Log.e(_tag, "runJob - job failed, abort")

        NotificationHelper(this)
            .sendNotification(
                this.getString(R.string.notification_error_title),
                this.getString(R.string.notification_config_job_failed),
                NotificationHelper.CONFIG_JOB_FAILED_NTF_ID
            )
        jobFinished(params, false)
    }
}