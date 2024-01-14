package com.ngsoft.getapp.sdk.jobs

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context


internal class JobScheduler {
    companion object {
        private const val INVENTORY_OFFERING_JOB_ID = 1
    }
    fun scheduleInventoryOfferingJob(context: Context) {
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
            .setPeriodic(24 * 60 * 60 * 1000L)
            .setBackoffCriteria(30 * 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setPersisted(true)
            .build()

        jobScheduler.schedule(jobInfo)
    }


    private fun isJobScheduled(context: Context, jobId: Int): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        return jobScheduler.getPendingJob(jobId) != null
    }
}