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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param discoveryType 
 * @param general 
 * @param softwareData 
 * @param mapData 
 */


data class DiscoveryMessageDto (

    @Json(name = "discoveryType")
    val discoveryType: DiscoveryMessageDto.DiscoveryType,

    @Json(name = "general")
    val general: GeneralDiscoveryDto? = null,

    @Json(name = "softwareData")
    val softwareData: DiscoverySoftwareDto? = null,

    @Json(name = "mapData")
    val mapData: DiscoveryMapDto? = null

) {

    /**
     * 
     *
     * Values: app,map
     */
    @JsonClass(generateAdapter = false)
    enum class DiscoveryType(val value: kotlin.String) {
        @Json(name = "get-app") app("get-app"),
        @Json(name = "get-map") map("get-map");
    }
}

