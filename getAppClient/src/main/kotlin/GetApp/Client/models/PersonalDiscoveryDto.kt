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
 * @param name 
 * @param idNumber 
 * @param personalNumber 
 */


data class PersonalDiscoveryDto (

    @Json(name = "name")
    val name: kotlin.String? = null,

    @Json(name = "idNumber")
    val idNumber: kotlin.String? = null,

    @Json(name = "personalNumber")
    val personalNumber: kotlin.String? = null

)

