package com.ngsoft.getapp.sdk.helpers

import GetApp.Client.models.InventoryUpdatesReqDto
import android.util.Log
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import java.time.OffsetDateTime

internal object InventoryClientHelper {
    private const val _tag = "InventoryClientHelper"

    fun getUpdates(config: GetMapService.GeneralConfig, mapRepo: MapRepo, client: GetAppClient, deviceId: String): List<String>{
        Log.i(_tag, "getUpdates")

        val inventory = mapRepo.getAll().mapNotNull { it.reqId }
        Log.d(_tag, "getUpdates - send ${inventory.size} maps")

        val req = InventoryUpdatesReqDto(deviceId = deviceId , inventory = inventory)
        val res = client.getMapApi.getMapControllerGetInventoryUpdates(req).updates

        config.lastInventoryCheck = OffsetDateTime.now()

        mapRepo.setMapsUpdatedValue(res)
        val mapsToUpdate = mapRepo.getMapsToUpdate()
        Log.i(_tag, "getUpdates - Found ${mapsToUpdate.size} maps to update")
        return mapsToUpdate
    }
}