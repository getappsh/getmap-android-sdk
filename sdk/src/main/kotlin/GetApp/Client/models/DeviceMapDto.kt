/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package GetApp.Client.models

import GetApp.Client.models.MapStateDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param maps 
 * @param id 
 * @param lastUpdatedDate 
 * @param OS 
 * @param availableStorage 
 * @param power 
 * @param bandwidth 
 * @param operativeState 
 */


data class DeviceMapDto (

    @Json(name = "maps")
    val maps: kotlin.collections.List<MapStateDto>,

    @Json(name = "id")
    val id: kotlin.String? = null,

    @Json(name = "lastUpdatedDate")
    val lastUpdatedDate: java.time.OffsetDateTime? = null,

    @Json(name = "OS")
    val OS: kotlin.String? = null,

    @Json(name = "availableStorage")
    val availableStorage: kotlin.String? = null,

    @Json(name = "power")
    val power: java.math.BigDecimal? = null,

    @Json(name = "bandwidth")
    val bandwidth: java.math.BigDecimal? = null,

    @Json(name = "operativeState")
    val operativeState: kotlin.Boolean? = null

)

