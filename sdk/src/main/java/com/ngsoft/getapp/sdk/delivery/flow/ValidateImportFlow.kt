package com.ngsoft.getapp.sdk.delivery.flow

import com.ngsoft.getapp.sdk.BuildConfig
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
            statusMsg = app.getString(R.string.delivery_status_in_verification),
            state = MapDeliveryState.DOWNLOAD,
            downloadProgress = 0, statusDescr = ""
        )

        if (this.mapRepo.isDownloadCanceled(id)){
            Timber.d("validateImport - Download $id, canceled by user")
            mapRepo.update(id, state = MapDeliveryState.CANCEL, statusMsg = app.getString(R.string.delivery_status_canceled))
            return false
        }
        val mapPkg = this.mapRepo.getById(id)!!

        val isValid = try{
            Timber.d("validateImport - fileName ${mapPkg.fileName}, jsonName ${mapPkg.jsonName}")
            val mapFile = File(mapPkg.path, mapPkg.fileName!!)
            val jsonFile = File(mapPkg.path, mapPkg.jsonName!!)

            val expectedHash = JsonUtils.getStringOrThrow(checksumAlgorithm, jsonFile.path)
            val actualHash = HashUtils.getCheckSumFromFile(checksumAlgorithm, mapFile) {
                Timber.d("validateImport - progress: $it")
                this.mapRepo.update(id, downloadProgress = it, statusMsg = app.getString(R.string.delivery_status_in_verification), statusDescr = "")
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
                statusMsg = app.getString(R.string.delivery_status_done),
                statusDescr = ""
            )
        }else{

            if (mapPkg.metadata.validationAttempt < 1){
                mapFileManager.deleteMapFiles(mapPkg.fileName, mapPkg.jsonName)
                val flowState = if (mapPkg.url != null){
                    DeliveryFlowState.IMPORT_DELIVERY
                }else if(mapPkg.reqId != null){
                    if (BuildConfig.USE_MAP_CACHE) {
                        DeliveryFlowState.IMPORT_STATUS
                    }else{
                        DeliveryFlowState.IMPORT_CREATE
                    }
                }else{
                    DeliveryFlowState.START
                }
                this.mapRepo.update(
                    id = id,
                    flowState = flowState,
                    validationAttempt = ++mapPkg.metadata.validationAttempt,
                    statusMsg = app.getString(R.string.delivery_status_failed_verification_try_again),
                    statusDescr = "Checksum validation Failed try downloading again",
                    mapDone = false,
                    jsonDone = false,
                )
                Timber.d("validateImport - Failed downloading again")
                return true
            }
            mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMsg = app.getString(R.string.delivery_status_failed_verification),
                statusDescr = "Checksum validation Failed"
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