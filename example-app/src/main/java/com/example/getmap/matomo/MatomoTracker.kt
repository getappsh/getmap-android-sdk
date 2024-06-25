package com.example.getmap.matomo

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.ngsoft.getapp.sdk.Pref
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder


class MatomoTracker private constructor(): ComponentCallbacks2{

    companion object {
        private val TAG = MatomoTracker::class.qualifiedName

        @Volatile private var tracker: Tracker? = null
        @Volatile private var siteId: String? = null
        @Volatile private lateinit var pref: Pref
        fun getTracker(context: Context): Tracker{
            Log.v(TAG, "getTracker")

            pref = Pref.getInstance(context)

            if ((siteId != null && siteId != pref.matomoSiteId) || (tracker != null && tracker?.apiUrl != pref.matomoUrl)){
                rebuildTracker(context)
            }
            if(tracker?.dispatchInterval != pref.matomoUpdateIntervalMins.toLong()){
                tracker?.dispatchInterval = pref.matomoUpdateIntervalMins.toLong()
            }

            return tracker ?: synchronized(this) {
                context.applicationContext.registerComponentCallbacks(MatomoTracker())
                tracker ?: initTracker(context).also { tracker = it }
            }
        }

        private fun initTracker(context: Context): Tracker{
            Log.d(TAG, "init tracker")
            val tracker = TrackerBuilder.createDefault(pref.matomoUrl, pref.matomoSiteId.toInt())
                .build(Matomo.getInstance(context))
            siteId = pref.matomoSiteId
            tracker.dispatchInterval =  pref.matomoUpdateIntervalMins.toLong()
            tracker.dispatchTimeout = 1000 * 15
            Log.d(TAG, "Matomo dispatchInterval: ${tracker.dispatchInterval},timeout: ${tracker.dispatchTimeout}, sessionTimeout: ${tracker.sessionTimeout}")
            return tracker
        }

        private fun rebuildTracker(context: Context){
            Log.d(TAG, "rebuild tracker")
            tracker?.dispatchInterval = -1
            tracker?.dispatch()
            synchronized(this){
                tracker = initTracker(context)
            }
        }
    }




    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged")
    }

    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        tracker?.dispatch()
    }

    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "onTrimMemory")
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            tracker?.dispatch()
        }
    }
}