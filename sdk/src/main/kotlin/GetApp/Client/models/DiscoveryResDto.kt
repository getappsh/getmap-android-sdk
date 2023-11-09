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

import GetApp.Client.models.DiscoveryMapResDto
import GetApp.Client.models.OfferingResponseDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param software 
 * @param map 
 */


data class DiscoveryResDto (

    @Json(name = "software")
    val software: OfferingResponseDto? = null,

    @Json(name = "map")
    val map: kotlin.collections.List<DiscoveryMapResDto>? = null

)

