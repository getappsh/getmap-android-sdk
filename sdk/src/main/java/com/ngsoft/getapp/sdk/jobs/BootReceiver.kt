package com.ngsoft.getapp.sdk.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ngsoft.getapp.sdk.downloader.FetchDownloader
import com.ngsoft.getapp.sdk.helpers.logger.TimberLogger
import com.tonyodev.fetch2.Fetch
import timber.log.Timber

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimberLogger.initTimber()
        Timber.i("On Receive")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            FetchDownloader.init(context)
            Fetch.Impl.getDefaultInstance()
        }
    }
}