package com.ngsoft.getapp.sdk.models

import com.ngsoft.getapp.sdk.utils.JsonUtils
import com.ngsoft.tilescache.models.DeliveryFlowState
import org.json.JSONObject
import java.nio.file.Paths
import java.time.OffsetDateTime

class MapData(
    var id: String? = null,
    var footprint: String? = null,
    var fileName: String? = null,
    var jsonName: String? = null,
    var deliveryState: MapDeliveryState,
    var url: String? = null,
    var path: String? = null,
    var statusMsg: String? = null,
    var progress: Int = 0,
    var statusDescr: String? = null,
    var isUpdated: Boolean = true,
    var downloadStart: OffsetDateTime?,
    var downloadStop: OffsetDateTime?,
    var downloadDone: OffsetDateTime?,
    val reqDate: OffsetDateTime,
    var flowState: DeliveryFlowState,
    ){
    fun getJson(): JSONObject?{
        return try {
            JsonUtils.readJson(Paths.get(path, jsonName).toString())
        }catch (e: Exception){
            null
        }
    }
}

