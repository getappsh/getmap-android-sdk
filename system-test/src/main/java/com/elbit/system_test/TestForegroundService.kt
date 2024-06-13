package com.elbit.system_test

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.SystemTest
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver


class TestForegroundService: Service() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("SystemTestResReceiver", "onReceive")
            val bundle = intent.getBundleExtra("bundle")
            val testResults: HashMap<Int, SystemTest.TestResults?> = bundle?.getSerializable(
                SystemTestReceiver.EXTRA_TEST_RESULTS
            ) as? HashMap<Int, SystemTest.TestResults?> ?: HashMap()
            Log.d("SystemTestResReceiver", "myMap: $testResults")

            testResults.forEach { (key, value) ->
                Log.d("SystemTestResReceiver", "key: $key, value: $value")
            }
            // Update the LiveData with the received test results
            TestResultsLiveData.LiveDataManager.updateResults(testResults)


//              if all of the keys and values are not null or empty, stop the job
            if (testResults.all { it.value?.success != null }) {
                Log.d("SystemTestResReceiver", "Close Job")
                stopService()
            }else{
                reQueueTimeoutRunnable(10)
            }

        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        Log.d("TestJobService", "Timeout reached, stopping the job service")
        stopService()

    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val notificationId = 1

    private var jobNum = 0;


    companion object {

        private const val START = "START"
        private const val STOP = "STOP"

        fun start(context: Context): Boolean{
            if (!isServiceRunning(context, TestForegroundService::class.java)) {
                val serviceIntent = Intent(context, TestForegroundService::class.java)
                serviceIntent.action = START
                context.startForegroundService(serviceIntent)
                return true
            }
            return false
        }

        fun stop(context: Context){
            Log.d("TestForegroundService", "stop")
            val serviceIntent = Intent(context, TestForegroundService::class.java)
            serviceIntent.action = STOP
            context.startForegroundService(serviceIntent)

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
        startForeground(notificationId, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TestForegroundService", "onStartCommand")

        if (intent?.action == STOP) {
            stopService()
        } else {
            reQueueTimeoutRunnable(3)
            registerReceiver(receiver, IntentFilter(SystemTestReceiver.ACTION_SYSTEM_TEST_RESULTS))
        }
        return START_STICKY

    }

    private fun reQueueTimeoutRunnable(minutes: Long){
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, minutes * 60 * 1000)
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeoutRunnable)
    }

    private fun stopService(){
        Log.d("TestForegroundService", "stopService")
        handler.removeCallbacks(timeoutRunnable)
        unregisterReceiver(receiver)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

    }

    private fun createNotification(): Notification {
        notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("1", "System Test", importance).apply {
            description = "System Test"
        }
        notificationManager.createNotificationChannel(channel)

        notificationBuilder = NotificationCompat.Builder(this, "1")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("System Test")
            .setContentText("Listening to results of system-tests")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)


        return notificationBuilder.build()

    }

    private fun updateNotification(){
        notificationBuilder.setContentText("This is a system Test, job number $jobNum")
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}