package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.helpers.client.MapImportClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber

internal class ImportCreateFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx) {
    override fun execute(id: String): Boolean {
        Timber.i("importCreate")

        if (this.mapRepo.isDownloadCanceled(id)){
            Timber.d("importCreate - Download $id, canceled by user")
            this.mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
            this.sendDeliveryStatus(id)
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!
        val mapProperties = MapProperties(productId = mapPkg.pId, boundingBox = mapPkg.bBox, isBest = false)
        val retCreate = MapImportClient.createMapImport(this.client, mapProperties, this.pref.deviceId)

        Timber.d("importCreate - import request Id: ${retCreate?.importRequestId}")
        when(retCreate?.state){
            MapImportState.START, MapImportState.IN_PROGRESS, MapImportState.DONE,  ->{
                Timber.d("deliverTile - createMapImport -> OK, state: ${retCreate.state} message: ${retCreate.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.START,
                    flowState = DeliveryFlowState.IMPORT_CREATE,
                    statusMessage = this.app.getString(R.string.delivery_status_req_in_progress),
                    errorContent = retCreate.statusCode?.messageLog ?: "",
                    downloadProgress = retCreate.progress
                )
                this.sendDeliveryStatus(id)
                return true
            }
            MapImportState.CANCEL -> {
                Timber.w("getDownloadData - createMapImport -> CANCEL, message: ${retCreate.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.CANCEL,
                    statusMessage = this.app.getString(R.string.delivery_status_canceled),
                    errorContent = retCreate.statusCode?.messageLog

                )
                this.sendDeliveryStatus(id)
                return false
            }
            else -> {
                Timber.e("getDownloadData - createMapImport failed: ${retCreate?.state}, error: ${retCreate?.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate?.importRequestId,
                    state = MapDeliveryState.ERROR,
                    statusMessage = this.app.getString(R.string.delivery_status_failed),
                    errorContent = retCreate?.statusCode?.messageLog
                )
                this.sendDeliveryStatus(id)
                return false
            }
        }
    }
}