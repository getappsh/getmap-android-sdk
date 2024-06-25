package com.elbit.system_test

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object JobSchedulerUtil {
    private const val JOB_ID = 123

    fun scheduleJobIfNotScheduled(context: Context): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobExists = jobScheduler.allPendingJobs.any { it.id == JOB_ID }

        if (!jobExists) {
            val componentName = ComponentName(context, TestJobService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(60 * 60 * 1000L) // 60 minutes interval
                .setPersisted(true) // persist across reboots
                .build()

            jobScheduler.schedule(jobInfo)
            return true
        } else {
            return false
        }
    }

    fun isJobScheduled(context: Context): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        return jobScheduler.allPendingJobs.any { it.id == JOB_ID }
    }

    fun cancelScheduledJob(context: Context){
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.cancel(JOB_ID)
    }
}
