package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.Log
import java.lang.IllegalArgumentException

//TODO send notification after X request failed
internal class JobScheduler {
    companion object {
        private val _tag = "JobScheduler"

        private const val INVENTORY_OFFERING_JOB_ID = 1
        private const val REMOTE_CONFIG_JOB_ID = 2
    }
    fun scheduleInventoryOfferingJob(context: Context, intervalMins: Int) {
        Log.i(_tag, "scheduleInventoryOfferingJob")
        if(isJobScheduled(context, INVENTORY_OFFERING_JOB_ID)){
            return
        }
        // Calculate the initial delay until midnight
//        val calendar: Calendar = Calendar.getInstance()
//        calendar.timeInMillis = System.currentTimeMillis()
//        calendar.set(Calendar.HOUR_OF_DAY, 0)
//        calendar.set(Calendar.MINUTE, 0)
//        val initialDelay: Long = calendar.timeInMillis - System.currentTimeMillis()

        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(INVENTORY_OFFERING_JOB_ID, ComponentName(context, InventoryUpdatesService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresCharging(false)
//            TODO make periodic configurable
            .setPeriodic(minutes2millis(intervalMins))
            .setBackoffCriteria(30 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build()

        try{
            jobScheduler.schedule(jobInfo)
        }catch (e: IllegalArgumentException){
            Log.e(_tag, "scheduleInventoryOfferingJob - failed to schedule the service, error: ${e.message.toString()}")
        }
    }


    fun scheduleRemoteConfigJob(context: Context, intervalMins: Int){
        Log.i(_tag, "scheduleRemoteConfigJob")
        if(isJobScheduled(context, REMOTE_CONFIG_JOB_ID)){
            return
        }
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(REMOTE_CONFIG_JOB_ID, ComponentName(context, RemoteConfigService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresCharging(false)
//            TODO make periodic configurable
            .setPeriodic(minutes2millis(intervalMins))
            .setBackoffCriteria(30 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build()

        try{
            jobScheduler.schedule(jobInfo)
        }catch (e: IllegalArgumentException){
            Log.e(_tag, "scheduleInventoryOfferingJob - failed to schedule the service, error: ${e.message.toString()}")
        }
    }

    fun updateInventoryOfferingJob(context: Context, intervalMins: Int){
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = jobScheduler.getPendingJob(INVENTORY_OFFERING_JOB_ID) ?: return
        if (jobInfo.intervalMillis != minutes2millis(intervalMins)){
            Log.i(_tag, "updateInventoryOfferingJob - Update job interval to: $intervalMins minutes ")
            jobScheduler.cancel(INVENTORY_OFFERING_JOB_ID)
            scheduleInventoryOfferingJob(context, intervalMins)
        }
    }
    fun updateRemoteConfigJob(context: Context, intervalMins: Int){
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = jobScheduler.getPendingJob(REMOTE_CONFIG_JOB_ID) ?: return

        if (jobInfo.intervalMillis != minutes2millis(intervalMins)){
            Log.i(_tag, "updateRemoteConfigJob - Update job interval to: $intervalMins minutes")
            jobScheduler.cancel(REMOTE_CONFIG_JOB_ID)
            scheduleRemoteConfigJob(context, intervalMins)
        }
    }
    private fun isJobScheduled(context: Context, jobId: Int): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        return jobScheduler.getPendingJob(jobId) != null
    }

    private fun minutes2millis(minutes: Int): Long{
        return minutes * 60 * 1000L
    }
}