package com.ngsoft.getapp.sdk.helpers

import GetApp.Client.models.InventoryUpdatesReqDto
import android.util.Log
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo

internal object InventoryClientHelper {
    private val _tag = "InventoryClientHelper"

    fun getUpdates(mapRepo: MapRepo, client: GetAppClient, deviceId: String): Int{
        Log.i(_tag, "getUpdates")

        val inventory = mapRepo.getAll().mapNotNull { it.reqId }
        Log.d(_tag, "getUpdates - send ${inventory.size} maps")

        val req = InventoryUpdatesReqDto(deviceId = deviceId , inventory = inventory)
        val res = client.getMapApi.getMapControllerGetInventoryUpdates(req).updates

        mapRepo.setMapsUpdatedValue(res)
        val amountToUpdate = res.values.count { !it }
        Log.i(_tag, "getUpdates - Found $amountToUpdate maps to update")
        return amountToUpdate
    }
}