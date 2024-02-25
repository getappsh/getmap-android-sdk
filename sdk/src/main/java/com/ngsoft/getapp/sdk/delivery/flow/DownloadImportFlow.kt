package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber

internal class DownloadImportFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx) {
    override fun execute(id: String): Boolean {
        Timber.i("downloadImport")

        val mapPkg = this.mapRepo.getById(id)!!
        val pkgUrl = mapPkg.url!!
        val jsonUrl = FileUtils.changeFileExtensionToJson(pkgUrl)

        val jsonDownloadId = if(!mapPkg.metadata.jsonDone) downloadFile(jsonUrl) else null
        val pkgDownloadId = if(!mapPkg.metadata.mapDone) downloadFile(pkgUrl) else null
        Timber.d("downloadImport - jsonDownloadId: $jsonDownloadId, pkgDownloadId: $pkgDownloadId")

        val statusMessage = if(mapPkg.metadata.validationAttempt <= 0) app.getString(R.string.delivery_status_download) else app.getString(
            R.string.delivery_status_failed_verification_try_again)
        this.mapRepo.update(
            id = id, JDID = jsonDownloadId, MDID = pkgDownloadId,
            state =  MapDeliveryState.DOWNLOAD, flowState = DeliveryFlowState.DOWNLOAD,
            statusMsg = statusMessage, statusDescr = "", downloadProgress = 0
        )
        this.sendDeliveryStatus(id)

        return true
    }
}