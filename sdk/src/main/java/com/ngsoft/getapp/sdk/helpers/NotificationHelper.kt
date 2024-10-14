package com.ngsoft.getapp.sdk.helpers

import android.app.Notification
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
        const val DELIVERY_SERVICE_NFT_ID = 4
        const val DISCOVERY_JOB_NEW_OFFERING_NTF_ID = 5
        const val DISCOVERY_JOB_FAILED_NTF_ID = 6

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

    fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    fun sendNotification(title: String, content: String, notificationId: Int) {
        val notification = createNotification(title, content)
        notificationManager.notify(notificationId, notification)
    }
}