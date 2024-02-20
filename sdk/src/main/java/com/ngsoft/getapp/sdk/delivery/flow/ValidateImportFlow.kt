package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.delivery.DeliveryContext
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.utils.HashUtils
import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import timber.log.Timber
import java.io.File

internal class ValidateImportFlow(dlvCtx: DeliveryContext) : DeliveryFlow(dlvCtx){
    override fun execute(id: String): Boolean {
        Timber.i("validateImport - id: $id")
        this.mapRepo.update(
            id = id,
            statusMessage = app.getString(R.string.delivery_status_in_verification),
            downloadProgress = 0, errorContent = ""
        )

        if (this.mapRepo.isDownloadCanceled(id)){
            Timber.d("validateImport - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMessage = app.getString(R.string.delivery_status_canceled))
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!

        val isValid = try{
            Timber.d("validateImport - fileName ${mapPkg.fileName}, jsonName ${mapPkg.jsonName}")
            val mapFile = File(config.storagePath, mapPkg.fileName!!)
            val jsonFile = File(config.storagePath, mapPkg.jsonName!!)

            val expectedHash = JsonUtils.getStringOrThrow(checksumAlgorithm, jsonFile.path)
            val actualHash = HashUtils.getCheckSumFromFile(checksumAlgorithm, mapFile) {
                Timber.d("validateImport - progress: $it")
                this.mapRepo.update(id, downloadProgress = it, statusMessage = app.getString(R.string.delivery_status_in_verification), errorContent = "")
            }
            Timber.d("validateImport - expectedHash: $expectedHash, actualHash: $actualHash")

            val isValid = expectedHash == actualHash
            Timber.d("validateImport - validation result for id: $id is: $isValid")

            isValid

        }catch (e: Exception){
            Timber.e("validateImport - Failed to validate map, error: ${e.message.toString()} ")
            true
        }
        if (isValid){
            this.findAndRemoveDuplicates(id)
            this.mapRepo.update(
                id = id,
                state =  MapDeliveryState.DONE,
                flowState = DeliveryFlowState.DONE,
                statusMessage = app.getString(R.string.delivery_status_done),
                errorContent = ""
            )
        }else{
            if (mapPkg.metadata.validationAttempt < 1){
                mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)

                this.mapRepo.update(
                    id = id,
                    flowState = DeliveryFlowState.IMPORT_DELIVERY,
                    validationAttempt = ++mapPkg.metadata.validationAttempt,
                    statusMessage = app.getString(R.string.delivery_status_failed_verification_try_again),
                    errorContent = "Checksum validation Failed try downloading again",
                )
                Timber.d("validateImport - Failed downloading again")
                return true
            }
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMessage = app.getString(R.string.delivery_status_failed_verification),
                errorContent = "Checksum validation Failed"
            )
        }
        this.sendDeliveryStatus(id)

        return isValid
    }

    private fun findAndRemoveDuplicates(id: String){
        Timber.i("findAndRemoveDuplicate")
        val mapPkg = this.mapRepo.getById(id) ?: return
        val duplicates = this.mapRepo.getByBBox(mapPkg.bBox, mapPkg.footprint).toMutableList()
        duplicates.removeIf { it.id.toString() == id }

        Timber.d("findAndRemoveDuplicate - found ${duplicates.size} duplicates")
        duplicates.forEach {
            Timber.d("findAndRemoveDuplicate - remove: ${it.id}")
            try {
                mapFileManager.deleteMap(it)
                MapDeliveryClient.sendDeliveryStatus(client, mapRepo, it.id.toString(), pref.deviceId, MapDeliveryState.DELETED)
                this.mapRepo.remove(it.id.toString())
            }catch (error: Exception){
                Timber.e("findAndRemoveDuplicates - failed to delete the map, error: ${error.message.toString()}", )
            }
        }
    }
}