package com.example.example_app.matomo

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
        fun getTracker(context: Context): Tracker{
            val pref = Pref.getInstance(context)
            if (siteId != pref.matomoSiteId || tracker?.apiUrl != pref.matomoUrl){
                rebuildTracker(context)
            }
            if(tracker?.dispatchInterval != 1000L * 60 * pref.matomoUpdateIntervalMins){
                tracker?.dispatchInterval = 1000L * 60 * pref.matomoUpdateIntervalMins
            }

            return tracker ?: synchronized(this) {
                context.registerComponentCallbacks(MatomoTracker())
                tracker ?: initTracker(context).also { tracker = it }
            }
        }

        private fun initTracker(context: Context): Tracker{
            val pref = Pref.getInstance(context)
            val tracker = TrackerBuilder.createDefault(pref.matomoUrl, pref.matomoSiteId.toInt())
                .build(Matomo.getInstance(context))
            tracker.dispatchInterval =  1000L * 60 * pref.matomoUpdateIntervalMins
            tracker.dispatchTimeout = 1000 * 15
            Log.d(TAG, "Matomo dispatchInterval: ${tracker.dispatchInterval},timeout: ${tracker.dispatchTimeout}, sessionTimeout: ${tracker.sessionTimeout}")
            return tracker
        }

        private fun rebuildTracker(context: Context){
            tracker?.dispatchInterval = -1
            tracker?.dispatch()
            synchronized(this){
                tracker = initTracker(context)
            }
        }
    }




    override fun onConfigurationChanged(newConfig: Configuration) {
    }

    override fun onLowMemory() {
        tracker?.dispatch()
    }

    override fun onTrimMemory(level: Int) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            tracker?.dispatch()
        }
    }
}