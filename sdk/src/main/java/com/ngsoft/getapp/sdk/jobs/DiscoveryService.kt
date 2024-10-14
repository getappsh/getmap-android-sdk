package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import com.ngsoft.getapp.sdk.helpers.DiscoveryManager
import com.ngsoft.getapp.sdk.helpers.logger.TimberLogger
import timber.log.Timber

class DiscoveryService: JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        TimberLogger.initTimber()
        Timber.i("onStartJob")
        Thread{runJob(params)}.start()
        return true

    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.i("onStopJob - start again later")
        return true
    }


    private fun runJob(params: JobParameters?){
        Timber.i("runJob")
        DiscoveryManager(this).startDiscovery(false)
        jobFinished(params, false)
    }
}