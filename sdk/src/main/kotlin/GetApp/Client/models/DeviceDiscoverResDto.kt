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

import GetApp.Client.models.ComponentDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param deviceId 
 * @param produceTime 
 * @param comps 
 */


data class DeviceDiscoverResDto (

    @Json(name = "deviceId")
    val deviceId: kotlin.String,

    @Json(name = "produceTime")
    val produceTime: java.time.OffsetDateTime,

    @Json(name = "comps")
    val comps: kotlin.collections.List<ComponentDto>

)

