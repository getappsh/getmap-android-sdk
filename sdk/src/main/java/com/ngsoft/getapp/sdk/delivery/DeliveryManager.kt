package com.ngsoft.getapp.sdk.delivery

import android.app.Application
import android.content.Context
import android.os.Environment
import timber.log.Timber
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.ngsoft.getapp.sdk.MapFileManager
import com.ngsoft.getapp.sdk.PackageDownloader
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.ServiceConfig
import com.ngsoft.getapp.sdk.delivery.flow.DownloadImportFlow
import com.ngsoft.getapp.sdk.delivery.flow.ImportCreateFlow
import com.ngsoft.getapp.sdk.delivery.flow.ImportDeliveryFlow
import com.ngsoft.getapp.sdk.delivery.flow.ImportStatusFlow
import com.ngsoft.getapp.sdk.delivery.flow.MoveImportFilesFlow
import com.ngsoft.getapp.sdk.delivery.flow.ValidateImportFlow
import com.ngsoft.getapp.sdk.delivery.flow.WatchDownloadImportFlow
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class DeliveryManager private constructor(appCtx: Context){

    private var config = ServiceConfig.getInstance(appCtx)
    private var mapRepo = MapRepo(appCtx)
    private var downloader = PackageDownloader(appCtx, Environment.DIRECTORY_DOWNLOADS)
    private var mapFileManager =  MapFileManager(appCtx, downloader)
    private var pref = Pref.getInstance(appCtx)
    private var client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
    private val app = appCtx as Application
    private val dlvContext = DeliveryContext(config=config, mapRepo=mapRepo, downloader=downloader, mapFileManager=mapFileManager, pref=pref, client=client, app=app)

    fun executeDeliveryFlow(id: String){
        Timber.d("executeDeliveryFlow - for id: $id")
        val mapPkg = this.mapRepo.getById(id)
        Timber.d("executeDeliveryFlow - id: &id Flow State: ${mapPkg?.flowState}")

        try{
            val toContinue = when(mapPkg?.flowState){
                DeliveryFlowState.START -> ImportCreateFlow(dlvContext).execute(id)
                DeliveryFlowState.IMPORT_CREATE -> ImportStatusFlow(dlvContext).execute(id)
                DeliveryFlowState.IMPORT_STATUS -> ImportDeliveryFlow(dlvContext).execute(id)
                DeliveryFlowState.IMPORT_DELIVERY -> DownloadImportFlow(dlvContext).execute(id)
                DeliveryFlowState.DOWNLOAD ->  WatchDownloadImportFlow(dlvContext).execute(id)
                DeliveryFlowState.DOWNLOAD_DONE -> MoveImportFilesFlow(dlvContext).execute(id)
                DeliveryFlowState.MOVE_FILES -> ValidateImportFlow(dlvContext).execute(id)
                DeliveryFlowState.DONE -> false
                else -> false
            }
            Timber.d("executeDeliveryFlow - to continue: $toContinue")

            if (toContinue) {
                executeDeliveryFlow(id)
            }
        }catch (e: IOException){
            var attempt = mapPkg?.metadata?.connectionAttempt ?: 5
            if (attempt < 5){
                Timber.e("executeDeliveryFlow - IOException try again, attempt: $attempt, Error: ${e.message.toString()}")

                this.mapRepo.update(
                    id = id,
                    statusMessage = app.getString(R.string.delivery_status_connection_issue_try_again),
                    errorContent = e.message.toString(),
                    connectionAttempt = ++attempt
                )
                TimeUnit.SECONDS.sleep(2)
                executeDeliveryFlow(id)
            }else{
                Timber.e("executeDeliveryFlow - exception:  ${e.message.toString()}")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMessage = app.getString(R.string.delivery_status_failed),
                    errorContent = e.message.toString()
                )
                this.sendDeliveryStatus(id)
            }
        }catch (e: Exception){
            Timber.e("executeDeliveryFlow - exception:  ${e.message.toString()}")
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed),
                errorContent = e.message.toString()
            )
            this.sendDeliveryStatus(id)
        }

    }

    private fun sendDeliveryStatus(id: String, state: MapDeliveryState?=null) {
        MapDeliveryClient.sendDeliveryStatus(client, mapRepo, id, pref.deviceId, state)
    }

    fun getMapsOnDownload(): LiveData<List<MapData>>{
        return Transformations.map(this.mapRepo.getAllMapsLiveData()){ maps ->
            val onDownloadList = maps.filter {
                it.deliveryState == MapDeliveryState.DOWNLOAD
                        || it.deliveryState == MapDeliveryState.CONTINUE
                        || it.deliveryState == MapDeliveryState.START
            }
            onDownloadList
        }
    }

    fun cancelDelivery(id: String){
        Timber.i("cancelDelivery - for id $id")
        Thread{
            try{
                this.mapRepo.setCancelDownload(id)
//                Force cancel
                TimeUnit.SECONDS.sleep(7)
                if (this.mapRepo.isDownloadCanceled(id)){
                    Timber.e("cancelDelivery - Download $id, was not canceled after 6 sec, force cancel")
                    mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
                    this.sendDeliveryStatus(id)
                }

            }catch (e: Exception){
                Timber.e("cancelDownload - failed to candle, error: $e", )
            }
        }.start()
    }

    companion object {
        @Volatile
        private var instance: DeliveryManager? = null

        fun getInstance(appContext: Context): DeliveryManager {
            return instance ?: synchronized(this){
                instance ?: DeliveryManager(appContext.applicationContext).also { instance = it }
            }
        }
    }

}