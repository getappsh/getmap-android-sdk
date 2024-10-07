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
 * @param id 
 * @param lastUpdatedDate 
 * @param lastConnectionDate 
 * @param name 
 * @param OS 
 * @param availableStorage 
 * @param power 
 * @param bandwidth 
 * @param operativeState 
 * @param groupName 
 * @param groupId 
 * @param uid 
 */


data class DeviceDto (

    @Json(name = "id")
    val id: kotlin.String? = null,

    @Json(name = "lastUpdatedDate")
    val lastUpdatedDate: java.time.OffsetDateTime? = null,

    @Json(name = "lastConnectionDate")
    val lastConnectionDate: java.time.OffsetDateTime? = null,

    @Json(name = "name")
    val name: kotlin.String? = null,

    @Json(name = "OS")
    val OS: kotlin.String? = null,

    @Json(name = "availableStorage")
    val availableStorage: kotlin.String? = null,

    @Json(name = "power")
    val power: java.math.BigDecimal? = null,

    @Json(name = "bandwidth")
    val bandwidth: java.math.BigDecimal? = null,

    @Json(name = "operativeState")
    val operativeState: kotlin.Boolean? = null,

    @Json(name = "groupName")
    val groupName: kotlin.String? = null,

    @Json(name = "groupId")
    val groupId: java.math.BigDecimal? = null,

    @Json(name = "uid")
    val uid: java.math.BigDecimal? = null

)

