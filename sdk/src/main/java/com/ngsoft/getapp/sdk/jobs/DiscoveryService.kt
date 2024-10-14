package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import com.ngsoft.getapp.sdk.helpers.DiscoveryManager
import timber.log.Timber

class DiscoveryService: JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Timber.i("onStartJob")
        DiscoveryManager(this).startDiscovery(false)
        return false

    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.i("onStopJob - start again later")
        return true
    }

}