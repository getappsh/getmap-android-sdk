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
 * @param zoomLevel 
 * @param productName 
 * @param productId 
 * @param boundingBox 
 * @param targetResolution 
 * @param lastUpdateAfter 
 */


data class MapProperties (

    @Json(name = "zoomLevel")
    val zoomLevel: java.math.BigDecimal,

    @Json(name = "productName")
    val productName: kotlin.String? = null,

    @Json(name = "productId")
    val productId: kotlin.String? = null,

    @Json(name = "boundingBox")
    val boundingBox: kotlin.String? = null,

    @Json(name = "targetResolution")
    val targetResolution: java.math.BigDecimal? = null,

    @Json(name = "lastUpdateAfter")
    val lastUpdateAfter: java.math.BigDecimal? = null

)
