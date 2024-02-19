package com.ravtech.matomo

import android.content.Context
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.ServiceConfig
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder
import timber.log.Timber


class MatomoTracker private constructor(){

    companion object {

        @Volatile private var tracker: Tracker? = null
        fun getTracker(context: Context) =
            tracker ?: synchronized(this) {
                tracker ?: initTracker(context).also { tracker = it }
            }


        private fun initTracker(context: Context): Tracker{
            val pref = Pref.getInstance(context)
//            TODO pref.matomoSiteId.toInt() catch error
            val tracker = TrackerBuilder.createDefault(pref.matomoUrl, pref.matomoSiteId.toInt())
                .build(Matomo.getInstance(context))
            tracker.dispatchInterval =  1000L * 60 * pref.matomoUpdateIntervalMins
            Timber.d("1 Matomo dispatchInterval: ${tracker.dispatchInterval}")
            return tracker
        }

        fun rebuildTracker(context: Context){
            Timber.d("rebuildTracker")
            tracker?.dispatchInterval = -1
            synchronized(this){
                tracker = initTracker(context)
            }
        }
    }


}