package com.ngsoft.getapp.sdk.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ngsoft.getapp.sdk.R

internal class NotificationHelper(private val appCtx: Context) {

    companion object {
        private const val CHANNEL_ID = "GetApp-1"
        private const val CHANNEL_NAME = "GetApp Notification"
        private const val CHANNEL_DESCRIPTION = "GetApp Notification Channel"

        const val INVENTORY_UPDATES_NTF_ID = 1
        const val INVENTORY_JOB_FAILED_NTF_ID = 2
        const val CONFIG_JOB_FAILED_NTF_ID = 3

    }

    private val notificationManager: NotificationManager =
        appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNotification(title: String, content: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}