package com.ngsoft.getapp.sdk.helpers

import GetApp.Client.models.InventoryUpdatesReqDto
import android.util.Log
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.time.OffsetDateTime

internal object InventoryClientHelper {
    private const val _tag = "InventoryClientHelper"

    private fun getUpdates(config: GetMapService.GeneralConfig, mapRepo: MapRepo, client: GetAppClient, deviceId: String): List<String>{
        Log.i(_tag, "getUpdates")

        val inventory = mapRepo.getAll()
            .filter { it.reqId != null }
            .associate {
                it.reqId!! to when(it.flowState){
                    DeliveryFlowState.START, DeliveryFlowState.IMPORT_CREATE,
                    DeliveryFlowState.IMPORT_STATUS, DeliveryFlowState.IMPORT_DELIVERY ->
                        InventoryUpdatesReqDto.Inventory.import
                    DeliveryFlowState.DOWNLOAD, DeliveryFlowState.DOWNLOAD_DONE, DeliveryFlowState.MOVE_FILES ->
                        InventoryUpdatesReqDto.Inventory.delivery
                    DeliveryFlowState.DONE ->
                        InventoryUpdatesReqDto.Inventory.installed
                }
        }
        Log.d(_tag, "getUpdates - send ${inventory.size} maps")

        val req = InventoryUpdatesReqDto(deviceId = deviceId , inventory = inventory)
        val res = client.getMapApi.getMapControllerGetInventoryUpdates(req).updates

        config.lastInventoryCheck = OffsetDateTime.now()

        mapRepo.setMapsUpdatedValue(res)
        val mapsToUpdate = mapRepo.getMapsToUpdate()
        Log.i(_tag, "getUpdates - Found ${mapsToUpdate.size} possible maps updates")
        return mapsToUpdate
    }

    fun getDoneMapsToUpdate(config: GetMapService.GeneralConfig, mapRepo: MapRepo, client: GetAppClient, deviceId: String): List<String>{
        Log.i(_tag, "getDoneMapsToUpdate")
        val mapsDone = mapRepo.getAll().filter { it.state != MapDeliveryState.DONE }.map { it.id.toString() }
        val mapsToUpdate = getUpdates(config, mapRepo, client, deviceId)
        val doneMapsToUpdate = mapsToUpdate.subtract(mapsDone.toSet()).toList()
        Log.d(_tag, "getDoneMapsToUpdate - Found ${doneMapsToUpdate.size} maps to update")
        return doneMapsToUpdate
    }

    fun getNewUpdates(config: GetMapService.GeneralConfig, mapRepo: MapRepo, client: GetAppClient, deviceId: String): List<String>{
        Log.i(_tag, "getNewUpdates")
        val mapsToUpdateBefore = mapRepo.getMapsToUpdate()
        val mapsToUpdateAfter = getDoneMapsToUpdate(config, mapRepo, client, deviceId)
        val mapsToUpdateNew = mapsToUpdateAfter.subtract(mapsToUpdateBefore.toSet()).toList()
        Log.i(_tag, "getNewUpdates - Found ${mapsToUpdateNew.size} new maps to update")
        return mapsToUpdateNew
    }
}