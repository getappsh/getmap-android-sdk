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
 * @param bugId 
 * @param uploadLogsUrl 
 */


data class NewBugReportResDto (

    @Json(name = "bugId")
    val bugId: java.math.BigDecimal,

    @Json(name = "uploadLogsUrl")
    val uploadLogsUrl: kotlin.String

)
