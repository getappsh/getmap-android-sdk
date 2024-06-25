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
 * @param deviceId 
 * @param agentVersion 
 * @param description 
 */


data class NewBugReportDto (

    @Json(name = "deviceId")
    val deviceId: kotlin.String,

    @Json(name = "agentVersion")
    val agentVersion: kotlin.String,

    @Json(name = "description")
    val description: kotlin.String? = null

)

