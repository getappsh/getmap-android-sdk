package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.BuildConfig
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.helpers.client.MapImportClient
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class ImportStatusFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx)  {
    @OptIn(ExperimentalTime::class)
    override fun execute(id: String): Boolean {
        Timber.i("checkImportStatue")

        val reqId = this.mapRepo.getReqId(id)!!;
        var timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes

        var stat : CreateMapImportStatus? = null
        var lastProgress : Int? = null
        do{
            if(timeoutTime.hasPassedNow()){
                Timber.w("checkImportStatus - timed out")
                this.mapRepo.update(
                    id = id,
                    state = MapDeliveryState.ERROR,
                    statusMsg = app.getString(R.string.delivery_status_failed),
                    statusDescr = "checkImportStatus - timed out"
                )
                this.sendDeliveryStatus(id)
                return false

            }

            TimeUnit.SECONDS.sleep(2)
            if (this.mapRepo.isDownloadCanceled(id)){
                Timber.d("checkImportStatue: Download $id, canceled by user")
                mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMsg = app.getString(
                    R.string.delivery_status_canceled))
                this.sendDeliveryStatus(id)
                return false
            }

            try {
                stat = MapImportClient.getCreateMapImportStatus(client, reqId)
            }catch (e: IOException){
                Timber.e("checkImportStatue - SocketException, try again. error: ${e.message.toString()}" )
                this.mapRepo.update(id = id,
                    statusMsg = app.getString(R.string.delivery_status_connection_issue_try_again), statusDescr = e.message.toString())
                continue
            }

            when(stat?.state){
                MapImportState.ERROR -> {
                    Timber.e("checkImportStatus - MapImportState -> ERROR, error:  ${stat.statusCode?.messageLog}")
                    if (stat.statusCode?.statusCode == StatusCode.REQUEST_ID_NOT_FOUND){
                        Timber.e("checkImportStatus - status code is REQUEST_ID_NOT_FOUND, set as obsolete")
                        handleMapNotExistsOnServer(id)
                    }else{
                        this.mapRepo.update(id = id, state = MapDeliveryState.ERROR,
                            statusMsg = app.getString(R.string.delivery_status_failed), statusDescr = stat.statusCode?.messageLog)
                    }
                    this.sendDeliveryStatus(id)
                    return false
                }
                MapImportState.CANCEL -> {
                    Timber.w("checkImportStatus - MapImportState -> CANCEL, message: ${stat.statusCode?.messageLog}")
                    this.mapRepo.update(id = id, state = MapDeliveryState.CANCEL,
                        statusMsg = app.getString(R.string.delivery_status_canceled), statusDescr = stat.statusCode?.messageLog)
                    this.sendDeliveryStatus(id)
                    return false

                }
                MapImportState.IN_PROGRESS -> {
                    Timber.w("checkImportStatus - MapImportState -> IN_PROGRESS, progress: ${stat.progress}")
                    this.mapRepo.update(id = id, downloadProgress = stat.progress,
                        statusMsg = app.getString(R.string.delivery_status_req_in_progress), statusDescr = "")
                    if (lastProgress != stat.progress){
                        timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes
                    }
                    lastProgress = stat.progress
                }
                else -> {}
            }

        }while (stat == null || stat.state!! != MapImportState.DONE)

        Timber.d("checkImportStatue: MapImportState.Done")

        var flowState = DeliveryFlowState.IMPORT_STATUS;
        var url: String? = null;
        if (!BuildConfig.USE_MAP_CACHE){
            flowState = DeliveryFlowState.IMPORT_DELIVERY;
            url = stat.url;
        }

        this.mapRepo.update(
            id = id,
            downloadProgress = 100,
            flowState = flowState,
            url = url,
            statusDescr = "")
        return true
    }
}