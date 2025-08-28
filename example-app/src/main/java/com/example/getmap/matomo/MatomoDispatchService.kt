package com.example.getmap.matomo

import android.app.job.JobParameters
import android.app.job.JobService
import org.matomo.sdk.Tracker
import org.matomo.sdk.dispatcher.DefaultDispatcher
import org.matomo.sdk.dispatcher.DispatchMode
import org.matomo.sdk.dispatcher.EventCache
import timber.log.Timber
import java.lang.reflect.Field

class MatomoDispatchService: JobService() {

    companion object{
        const val MATOMO_DISPATCH_SERVICE_ID = 32
        private val TAG = MatomoDispatchService::class.qualifiedName
    }
    override fun onStartJob(params: JobParameters?): Boolean {
        Timber.d("onStartJob")
        val tracker = MatomoTracker.getTracker(this)
        tracker.dispatchMode = DispatchMode.ALWAYS
//        TODO what happens when dispatcher failed, how to catch the error for reschedule
        tracker.dispatchBlocking()
        val reschedule = !isQueueEmpty(tracker)
        jobFinished(params, reschedule)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.d("onStopJob")
        return true

    }


    private fun isQueueEmpty(tracker: Tracker): Boolean {
//        Using reflection BAD PRACTICE
        val dispatcherField: Field = Tracker::class.java.getDeclaredField("mDispatcher")
        dispatcherField.isAccessible = true
        val mDispatcher = dispatcherField.get(tracker) as DefaultDispatcher

        val eventCacheField: Field = DefaultDispatcher::class.java.getDeclaredField("mEventCache")
        eventCacheField.isAccessible = true
        val mEventCache = eventCacheField.get(mDispatcher) as EventCache

        val empty = mEventCache.isEmpty
        Timber.d("isQueueEmpty: $empty")
        return empty
    }
}