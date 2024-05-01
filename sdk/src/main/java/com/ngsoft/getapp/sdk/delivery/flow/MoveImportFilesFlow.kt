package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber

internal class MoveImportFilesFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx) {
    override fun execute(id: String): Boolean {
        Timber.i("moveImportFiles - id: $id")

        val mapPkg = this.mapRepo.getById(id)!!

        return try {
            Timber.d("moveImportFiles - fileName ${mapPkg.fileName} jsonName: ${mapPkg.jsonName}")
            val (fileName, jsonName) = mapFileManager.moveFilesToTargetDir(mapPkg.fileName!!, mapPkg.jsonName!!)
//            fileName = mapFileManager.moveFileToTargetDir(fileName)
//            jsonName = mapFileManager.moveFileToTargetDir(jsonName)
            this.mapRepo.update(id, flowState = DeliveryFlowState.MOVE_FILES, fileName = fileName, jsonName = jsonName)
            true
        }catch (e: Exception){
            Timber.e("moveImportFiles - move file failed: ${e.message.toString()}", )
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMsg = app.getString(R.string.delivery_status_failed),
                statusDescr = e.message.toString()
            )
            sendDeliveryStatus(id)
            false
        }
    }
}