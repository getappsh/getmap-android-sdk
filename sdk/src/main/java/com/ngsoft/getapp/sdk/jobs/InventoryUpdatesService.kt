package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobParameters
import android.app.job.JobService

import android.util.Log
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.ServiceConfig
import com.ngsoft.getapp.sdk.helpers.InventoryClientHelper
import com.ngsoft.getapp.sdk.helpers.NotificationHelper
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import java.time.OffsetDateTime


class InventoryUpdatesService: JobService() {

    private val _tag = "InventoryUpdatesService"

    private lateinit var mapRepo: MapRepo
    private lateinit var pref: Pref
    private lateinit var client: GetAppClient
    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(_tag, "onStartJob")

        mapRepo = MapRepo(this)
        pref = Pref.getInstance(this)
        client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
        Thread{ runJob(params) }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(_tag, "onStopJob - start again later")
        return true
    }


    private fun runJob(params: JobParameters?){
        try{
            val mapsToUpdate = InventoryClientHelper.getUpdates(ServiceConfig.getInstance(this), mapRepo, client, pref.deviceId)

            if (mapsToUpdate.isNotEmpty()){
                Log.d(_tag, "run - send notification")
                NotificationHelper(this)
                    .sendNotification(
                        getString(R.string.notification_update_map_title),
                        getString(R.string.notification_update_map_description, mapsToUpdate.size),
                        1)
            }
            jobFinished(params, false)

        }catch (e: Exception){
            Log.e(_tag, "runJob - Failed to get inventory updates, error: ${e.message.toString()}")
            jobFinished(params, true)
        }
    }
}