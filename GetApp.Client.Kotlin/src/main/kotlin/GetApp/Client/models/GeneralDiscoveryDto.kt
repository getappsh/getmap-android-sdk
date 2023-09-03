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

import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.SituationalDiscoveryDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param personalDevice 
 * @param situationalDevice 
 * @param physicalDevice 
 */


data class GeneralDiscoveryDto (

    @Json(name = "personalDevice")
    val personalDevice: PersonalDiscoveryDto? = null,

    @Json(name = "situationalDevice")
    val situationalDevice: SituationalDiscoveryDto? = null,

    @Json(name = "physicalDevice")
    val physicalDevice: PhysicalDiscoveryDto? = null

)

