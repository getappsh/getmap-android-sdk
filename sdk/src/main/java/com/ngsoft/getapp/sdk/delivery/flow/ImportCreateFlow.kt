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
            this.mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMsg = app.getString(R.string.delivery_status_canceled))
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
                    statusMsg = this.app.getString(R.string.delivery_status_req_in_progress),
                    statusDescr = retCreate.statusCode?.messageLog ?: "",
                    downloadProgress = retCreate.progress
                )
                return true
            }
            MapImportState.CANCEL -> {
                Timber.w("getDownloadData - createMapImport -> CANCEL, message: ${retCreate.statusCode?.messageLog}")
                this.mapRepo.update(
                    id = id,
                    reqId = retCreate.importRequestId,
                    state = MapDeliveryState.CANCEL,
                    statusMsg = this.app.getString(R.string.delivery_status_canceled),
                    statusDescr = retCreate.statusCode?.messageLog

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
                    statusMsg = this.app.getString(R.string.delivery_status_failed),
                    statusDescr = retCreate?.statusCode?.messageLog
                )
                this.sendDeliveryStatus(id)
                return false
            }
        }
    }
}