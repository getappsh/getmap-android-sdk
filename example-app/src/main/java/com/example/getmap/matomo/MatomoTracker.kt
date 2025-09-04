package com.example.getmap.matomo

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import com.ngsoft.getapp.sdk.Pref
import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.dispatcher.DispatchMode
import timber.log.Timber
import java.lang.IllegalArgumentException


class MatomoTracker private constructor(): ComponentCallbacks2{

    companion object {
        private val TAG = MatomoTracker::class.qualifiedName

        @Volatile
        private var tracker: Tracker? = null
        @Volatile
        private var siteId: String? = null
        @Volatile
        private var dispatchInterval: Long = 2000L
        @Volatile
        private lateinit var pref: Pref
        @Volatile
        private lateinit var jobScheduler: JobScheduler
        fun getTracker(context: Context): Tracker {
            Timber.v("getTracker")

            pref = Pref.getInstance(context)
            dispatchInterval = 1000L * 60 * pref.matomoUpdateIntervalMins

            if ((siteId != null && siteId != pref.matomoSiteId) || (tracker != null && tracker?.apiUrl != pref.matomoUrl)) {
                rebuildTracker(context)
            }
            return tracker ?: synchronized(this) {
                jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                context.applicationContext.registerComponentCallbacks(MatomoTracker())
                tracker ?: initTracker(context).also { tracker = it }
            }
        }

        private fun initTracker(context: Context): Tracker{
            Timber.d("init tracker")
            val tracker = TrackerBuilder
                .createDefault(pref.matomoUrl, pref.matomoSiteId.toInt())
                .build(Matomo.getInstance(context))

            siteId = pref.matomoSiteId
            tracker.dispatchInterval = -1 // manually
            tracker.dispatchTimeout = 1000 * 15 // 15 seconds
            tracker.offlineCacheSize = 10 * 1024 * 1024 // 10 mb
            tracker.offlineCacheAge = 86400000 * 3 // 3 days
//            tracker.setDispatchGzipped(true)
            tracker.userId = pref.deviceId

            Timber.d("Matomo dispatchInterval: ${dispatchInterval}, timeout: ${tracker.dispatchTimeout}, sessionTimeout: ${tracker.sessionTimeout}")

            tracker.addTrackingCallback { trackMe: TrackMe? ->
                scheduleDispatchJob(context, dispatchInterval)
                printTrackMe(trackMe)
                trackMe
            }
            return tracker
        }

        private fun printTrackMe(trackMe: TrackMe?){
            trackMe ?: return
            val sb = StringBuilder()
            sb.append("TrackMe{");
            val mQueryParams = trackMe.toMap()

            for (entry in mQueryParams.entries) {
                sb.append(entry.key).append("=").append(entry.value).append(", ");
            }
            // Remove the last comma and space
            if (sb.length > 8) {
                sb.setLength(sb.length - 2);
            }
            sb.append("}");
            Timber.v(sb.toString())
        }



        private fun rebuildTracker(context: Context){
            Timber.d("rebuild tracker")
            tracker?.dispatch()
            synchronized(this){
                tracker = initTracker(context)
            }
        }

        private fun scheduleDispatchJob(context: Context, delay: Long){
            val minLatency = jobScheduler.getPendingJob(MatomoDispatchService.MATOMO_DISPATCH_SERVICE_ID)?.minLatencyMillis

            if (minLatency == delay){
                return
            } else if(minLatency != null) {
                jobScheduler.cancel(MatomoDispatchService.MATOMO_DISPATCH_SERVICE_ID)
                Timber.d("scheduleDispatchJob - Job is cancelled")
            }
            Timber.d("scheduleDispatchJob - Job is scheduled")

            val jobInfo = JobInfo.Builder(MatomoDispatchService.MATOMO_DISPATCH_SERVICE_ID, ComponentName(context, MatomoDispatchService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
                .setBackoffCriteria(5* 60 * 1000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setMinimumLatency(delay)
                .setPersisted(true)
                .build()

            try{
                jobScheduler.schedule(jobInfo)
            }catch (e: IllegalArgumentException){
                Timber.e("scheduleDispatchJob: ")
            }

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged")
    }

    override fun onLowMemory() {
        Timber.d("onLowMemory")
        cacheEvents()
    }

    override fun onTrimMemory(level: Int) {
        Timber.d("onTrimMemory")
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            cacheEvents()
        }
    }

    private fun cacheEvents(){
        tracker?.dispatchMode = DispatchMode.EXCEPTION
        tracker?.dispatchBlocking()
        tracker?.dispatchMode = DispatchMode.ALWAYS
    }
}