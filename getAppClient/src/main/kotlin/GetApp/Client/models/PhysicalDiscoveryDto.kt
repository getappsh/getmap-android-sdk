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
 * @param OS 
 * @param MAC 
 * @param IP 
 * @param ID 
 * @param serialNumber 
 * @param possibleBandwidth 
 * @param availableStorage 
 */


data class PhysicalDiscoveryDto (

    @Json(name = "OS")
    val OS: PhysicalDiscoveryDto.OSEnum,

    @Json(name = "MAC")
    val MAC: kotlin.String? = null,

    @Json(name = "IP")
    val IP: kotlin.String? = null,

    @Json(name = "ID")
    val ID: kotlin.String? = null,

    @Json(name = "serialNumber")
    val serialNumber: kotlin.String? = null,

    @Json(name = "possibleBandwidth")
    val possibleBandwidth: kotlin.String? = null,

    @Json(name = "availableStorage")
    val availableStorage: kotlin.String? = null

) {

    /**
     * 
     *
     * Values: android,windows,linux
     */
    @JsonClass(generateAdapter = false)
    enum class OSEnum(val value: kotlin.String) {
        @Json(name = "android") android("android"),
        @Json(name = "windows") windows("windows"),
        @Json(name = "linux") linux("linux");
    }
}

