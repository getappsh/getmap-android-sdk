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

import GetApp.Client.models.DeviceDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param bugId 
 * @param logsUrl 
 * @param agentVersion 
 * @param device 
 * @param reportDate 
 * @param description 
 */


data class BugReportDto (

    @Json(name = "bugId")
    val bugId: java.math.BigDecimal,

    @Json(name = "logsUrl")
    val logsUrl: kotlin.String,

    @Json(name = "agentVersion")
    val agentVersion: kotlin.String,

    @Json(name = "device")
    val device: DeviceDto,

    @Json(name = "reportDate")
    val reportDate: java.time.OffsetDateTime,

    @Json(name = "description")
    val description: kotlin.String? = null

)

