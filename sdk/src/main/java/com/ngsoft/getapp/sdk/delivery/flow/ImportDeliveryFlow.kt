package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class ImportDeliveryFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx) {
    @OptIn(ExperimentalTime::class)
    override fun execute(id: String): Boolean {
        Timber.i("importDelivery")

        val reqId = this.mapRepo.getReqId(id)!!;

        var retDelivery: MapImportDeliveryStatus? =  MapImportDeliveryStatus()
        for (i in 0 until 3){
            try {
                retDelivery = MapDeliveryClient.setMapImportDeliveryStart(client, reqId, pref.deviceId)
                break
            }catch (e: Exception){
                Timber.e("importDelivery - error: ${e.message.toString()}")
                if (i == 2){
                    this.mapRepo.update(id = id,
                        state = MapDeliveryState.ERROR,
                        statusMsg = app.getString(R.string.delivery_status_failed),
                        statusDescr = "importDelivery - setMapImportDeliveryStart failed: ${e.message.toString()}")
                    return false
                }
                TimeUnit.SECONDS.sleep(1)
            }
        }
        val timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        while (retDelivery?.state != MapDeliveryState.DONE) {
            when (retDelivery?.state) {
                MapDeliveryState.DONE, MapDeliveryState.START, MapDeliveryState.DOWNLOAD, MapDeliveryState.CONTINUE -> {
                    this.mapRepo.update(id = id, flowState = DeliveryFlowState.IMPORT_DELIVERY,
                        statusMsg = app.getString(R.string.delivery_status_prepare_to_download), statusDescr = "")

                }

                MapDeliveryState.CANCEL, MapDeliveryState.PAUSE -> {
                    Timber.w("importDelivery - setMapImportDeliveryStart => CANCEL")
                    this.mapRepo.update(id = id, state = MapDeliveryState.CANCEL, statusMsg = app.getString(
                        R.string.delivery_status_canceled))
                    this.sendDeliveryStatus(id)
                    return false
                }
                else -> {
                    Timber.e("importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                    this.mapRepo.update(id = id,
                        state = MapDeliveryState.ERROR,
                        statusMsg = app.getString(R.string.delivery_status_failed),
                        statusDescr = "importDelivery - setMapImportDeliveryStart failed: ${retDelivery?.state}"
                    )
                    this.sendDeliveryStatus(id)
                    return false
                }
            }
            if(timeoutTime.hasPassedNow()){
                Timber.e("importDelivery - timed out")
                this.mapRepo.update(id = id, state = MapDeliveryState.ERROR, statusMsg = app.getString(
                    R.string.delivery_status_failed), statusDescr = "ImportDelivery- timed out")
                this.sendDeliveryStatus(id)
                return false
            }

            if (this.mapRepo.isDownloadCanceled(id)){
                Timber.d("importDelivery: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMsg = app.getString(
                    R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }
            TimeUnit.SECONDS.sleep(2)
            retDelivery = MapDeliveryClient.getMapImportDeliveryStatus(client, reqId)
        }

        if (retDelivery.url == null){
            Timber.e("importDelivery- download url is null", )
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMsg = app.getString(R.string.delivery_status_failed),
                statusDescr = "importDelivery - download url is null"
            )
            this.sendDeliveryStatus(id)
            return false

        }

        Timber.d("importDelivery - delivery is ready, download url: ${retDelivery.url} ")
        this.mapRepo.update(id = id, url = retDelivery.url, flowState = DeliveryFlowState.IMPORT_DELIVERY, statusDescr = "")
        return true
    }
}