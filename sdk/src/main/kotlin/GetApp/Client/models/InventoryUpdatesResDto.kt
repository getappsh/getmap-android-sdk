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
 * @param updates 
 */


data class InventoryUpdatesResDto (

    @Json(name = "updates")
    val updates: kotlin.collections.Map<kotlin.String, kotlin.Boolean>

)

