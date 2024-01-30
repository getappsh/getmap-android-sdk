package com.ngsoft.getapp.sdk.jobs

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.helpers.NotificationHelper
import com.ngsoft.getapp.sdk.delivery.DeliveryManager
import com.ngsoft.getapp.sdk.models.MapDownloadData

class DeliveryForegroundService: Service() {

    companion object{
        private const val _tag = "DeliveryForegroundService"
        const val START_DELIVERY = "startDelivery"
        const val CANCEL_DELIVERY = "cancelDelivery"

        const val MAP_ID_EXTRA = "mapId"


        fun startForId(context: Context, id: String){
            val serviceIntent = Intent(context, DeliveryForegroundService::class.java)
            serviceIntent.action = START_DELIVERY
            serviceIntent.putExtra(MAP_ID_EXTRA, id)
            context.startForegroundService(serviceIntent)
        }

//        TODO maybe cancel delivery need to be directly from Delivery manager
        fun cancelForId(context: Context, id: String){
            val serviceIntent = Intent(context, DeliveryForegroundService::class.java)
            serviceIntent.action = CANCEL_DELIVERY
            serviceIntent.putExtra(MAP_ID_EXTRA, id)
            context.startForegroundService(serviceIntent)
        }

    }
    private lateinit var deliveryManager: DeliveryManager
    private lateinit var notificationHelper: NotificationHelper
    private var mapsOnDownload: LiveData<List<MapDownloadData>>? = null
    override fun onCreate() {
        super.onCreate()
        Log.i(_tag, "onCreate")
        deliveryManager = DeliveryManager.getInstance(this)
        notificationHelper = NotificationHelper(this)


        val notification = notificationHelper.createNotification(this.getString(R.string.notification_delivery_service_title), this.getString(R.string.notification_delivery_service_description))
        startForeground(NotificationHelper.DELIVERY_SERVICE_NFT_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(_tag, "onStartCommand")
        val id  = intent?.getStringExtra(MAP_ID_EXTRA) ?: return START_STICKY
        Log.d(_tag, "onStartCommand - mapId: $id")
        when(intent.action){
            START_DELIVERY ->{
                startDelivery(id)
            }
            CANCEL_DELIVERY ->{
                stopDelivery(id)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(_tag, "onDestroy")
        super.onDestroy()
    }
    private fun startDelivery(id: String){
        Log.i(_tag, "startDelivery - for id: $id")
        Thread{deliveryManager.executeDeliveryFlow(id)}.start()
        waitToAllProcessToStop()
    }

    private fun stopDelivery(id: String){
        Log.i(_tag, "stopDelivery - for id: $id")
        deliveryManager.cancelDelivery(id)
        waitToAllProcessToStop()
    }

    private fun waitToAllProcessToStop(){
        if (mapsOnDownload == null){
            Log.d(_tag, "waitToAllProcessToStop")
            mapsOnDownload = deliveryManager.getMapsOnDownload()

            val observer = Observer<List<MapDownloadData>> {data ->
                if (data.isEmpty()){
                    stopService()
                }
            }
            mapsOnDownload!!.observeForever(observer)
        }
    }

    private fun stopService(){
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}