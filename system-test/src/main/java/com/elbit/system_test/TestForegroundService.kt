package com.elbit.system_test

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver


class TestForegroundService: Service() {

    companion object {
        fun start(context: Context){
            if (!isServiceRunning(context, TestForegroundService::class.java)) {
                val serviceIntent = Intent(context, TestForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }

        fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TestForegroundService", "onCreate")
        val notification = createNotification()
        startForeground(1, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TestForegroundService", "onStartCommand")


        Thread{
            while (true) {
                runJob()
                Thread.sleep(1000 * 60 * 60)
            }
        }.start()

        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun stopService(){
        Log.d("TestForegroundService", "stopService")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

    }


    private fun runJob(){
        Log.d("TestForegroundService", "runJob")
        registerReceiver(SystemTestResReceiver, IntentFilter(SystemTestReceiver.ACTION_SYSTEM_TEST_RESULTS))
        val runSystemTestIntent = Intent(SystemTestReceiver.ACTION_RUN_SYSTEM_TEST)
        sendBroadcast(runSystemTestIntent)

        Thread.sleep(1000 * 60)
        Log.d("TestForegroundService", "jobFinished")
        unregisterReceiver(SystemTestResReceiver)
//        stopService()
    }

    private fun createNotification(): Notification {
        val notificationManager: NotificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("1", "System Test", importance).apply {
            description = "System Test"
        }
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, "1")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("System Test")
            .setContentText("This is a system Test")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

    }
}