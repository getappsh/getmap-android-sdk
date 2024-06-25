package com.elbit.system_test

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver

class TestJobService : JobService() {

    private lateinit var jobParams: JobParameters

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("TestJobService", "Job started")

        if (params != null) {
            jobParams = params
        }

        TestForegroundService.start(this)
        runJob()
        return true
    }

    private fun runJob(){
        Log.d("TestJobService", "runJob")
        SharedPreferencesHelper.writeStartTestTime(this)
        val runSystemTestIntent = Intent(SystemTestReceiver.ACTION_RUN_SYSTEM_TEST)
        sendBroadcast(runSystemTestIntent)
        jobFinished(jobParams, false)
    }
    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("TestJobService", "Job stopped")
        return false
    }

    override fun onDestroy() {
        Log.d("TestJobService", "Job destroyed")
        super.onDestroy()
    }
}
