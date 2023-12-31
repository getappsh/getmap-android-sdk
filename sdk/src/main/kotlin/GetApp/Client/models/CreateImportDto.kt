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

import GetApp.Client.models.MapProperties

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param deviceId 
 * @param mapProperties 
 */


data class CreateImportDto (

    @Json(name = "deviceId")
    val deviceId: kotlin.String? = null,

    @Json(name = "mapProperties")
    val mapProperties: MapProperties? = null

)

