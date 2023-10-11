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

/**
 * 
 *
 * @param name 
 * @param platformNumber 
 * @param virtualSize 
 * @param components 
 */


data class PlatformDto (

    @Json(name = "name")
    val name: kotlin.String? = null,

    @Json(name = "platformNumber")
    val platformNumber: kotlin.String? = null,

    @Json(name = "virtualSize")
    val virtualSize: java.math.BigDecimal? = null,

    @Json(name = "components")
    val components: kotlin.collections.List<ComponentDto>? = null

)
