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
 * @param platforms 
 * @param formations 
 * @param categories 
 * @param operationsSystem 
 */


data class ProjectConfigDto (

    @Json(name = "platforms")
    val platforms: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "formations")
    val formations: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "categories")
    val categories: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "operationsSystem")
    val operationsSystem: kotlin.collections.List<kotlin.String>? = null

)

