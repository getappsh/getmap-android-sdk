package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.InventoryUpdatesReqDto
import timber.log.Timber
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.time.OffsetDateTime

internal object InventoryClient {
    private const val _tag = "InventoryClientHelper"

    private fun getUpdates(config: GetMapService.GeneralConfig, mapRepo: MapRepo, client: GetAppClient, deviceId: String): List<String>{
        Timber.i("getUpdates")

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
        Timber.d("getUpdates - send ${inventory.size} maps")

        val req = InventoryUpdatesReqDto(deviceId = deviceId , inventory = inventory)
        val res = client.getMapApi.getMapControllerGetInventoryUpdates(req).updates

        config.lastInventoryCheck = OffsetDateTime.now()

        mapRepo.setMapsUpdatedValue(res)
        val mapsToUpdate = mapRepo.getMapsToUpdate()
        Timber.i("getUpdates - Found ${mapsToUpdate.size} possible maps updates")
        return mapsToUpdate
    }

    fun getDoneMapsToUpdate(client: GetAppClient, mapRepo: MapRepo, config: GetMapService.GeneralConfig, deviceId: String): List<String>{
        Timber.i("getDoneMapsToUpdate")
        val mapsDone = mapRepo.getAll().filter { it.state != MapDeliveryState.DONE }.map { it.id.toString() }
        val mapsToUpdate = getUpdates(config, mapRepo, client, deviceId)
        val doneMapsToUpdate = mapsToUpdate.subtract(mapsDone.toSet()).toList()
        Timber.d("getDoneMapsToUpdate - Found ${doneMapsToUpdate.size} maps to update")
        return doneMapsToUpdate
    }

    fun getNewUpdates(client: GetAppClient, mapRepo: MapRepo, config: GetMapService.GeneralConfig, deviceId: String): List<String>{
        Timber.i("getNewUpdates")
        val mapsToUpdateBefore = mapRepo.getMapsToUpdate()
        val mapsToUpdateAfter = getDoneMapsToUpdate(client, mapRepo, config, deviceId)
        val mapsToUpdateNew = mapsToUpdateAfter.subtract(mapsToUpdateBefore.toSet()).toList()
        Timber.i("getNewUpdates - Found ${mapsToUpdateNew.size} new maps to update")
        return mapsToUpdateNew
    }
}