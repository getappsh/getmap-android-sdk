package com.ngsoft.getapp.sdk.jobs

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import timber.log.Timber
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.helpers.NotificationHelper
import com.ngsoft.getapp.sdk.delivery.DeliveryManager
import com.ngsoft.getapp.sdk.models.MapDownloadData

class DeliveryForegroundService: Service() {

    companion object{
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
        Timber.i("onCreate")
        deliveryManager = DeliveryManager.getInstance(this)
        notificationHelper = NotificationHelper(this)


        val notification = notificationHelper.createNotification(this.getString(R.string.notification_delivery_service_title), this.getString(R.string.notification_delivery_service_description))
        startForeground(NotificationHelper.DELIVERY_SERVICE_NFT_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand")
        val id  = intent?.getStringExtra(MAP_ID_EXTRA) ?: return START_STICKY
        Timber.d("onStartCommand - mapId: $id")
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
        Timber.d("onDestroy")
//        TODO call function from delivery manager to stop all process
        super.onDestroy()
    }
    private fun startDelivery(id: String){
        Timber.i("startDelivery - for id: $id")
        Thread{deliveryManager.executeDeliveryFlow(id)}.start()
        waitToAllProcessToStop()
    }

    private fun stopDelivery(id: String){
        Timber.i("stopDelivery - for id: $id")
        deliveryManager.cancelDelivery(id)
        waitToAllProcessToStop()
    }

    private fun waitToAllProcessToStop(){
        if (mapsOnDownload == null){
            Timber.d("waitToAllProcessToStop")
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