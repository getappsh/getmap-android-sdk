package com.ravtech.matomo

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.ngsoft.getapp.sdk.Pref
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder
import timber.log.Timber


class MatomoTracker private constructor(): ComponentCallbacks2{

    companion object {

        @Volatile private var tracker: Tracker? = null
        fun getTracker(context: Context) =
            tracker ?: synchronized(this) {
                context.registerComponentCallbacks(MatomoTracker())
                tracker ?: initTracker(context).also { tracker = it }
            }


        private fun initTracker(context: Context): Tracker{
            val pref = Pref.getInstance(context)
//            TODO pref.matomoSiteId.toInt() catch error
            val tracker = TrackerBuilder.createDefault(pref.matomoUrl, pref.matomoSiteId.toInt())
                .build(Matomo.getInstance(context))
            tracker.dispatchInterval =  1000L * 60 * pref.matomoUpdateIntervalMins
            tracker.dispatchTimeout = 1000 * 15
            Timber.d("Matomo dispatchInterval: ${tracker.dispatchInterval},timeout: ${tracker.dispatchTimeout}, sessionTimeout: ${tracker.sessionTimeout}")
            return tracker
        }

        fun rebuildTracker(context: Context){
            Timber.d("rebuildTracker")
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