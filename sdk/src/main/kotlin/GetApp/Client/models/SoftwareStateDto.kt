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

import GetApp.Client.models.ComponentDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param software 
 * @param downloadDate 
 * @param deployDate 
 * @param offering 
 * @param state 
 */


data class SoftwareStateDto (

    @Json(name = "software")
    val software: ComponentDto,

    @Json(name = "downloadDate")
    val downloadDate: java.time.OffsetDateTime,

    @Json(name = "deployDate")
    val deployDate: java.time.OffsetDateTime,

    @Json(name = "offering")
    val offering: kotlin.collections.List<ComponentDto>,

    @Json(name = "state")
    val state: SoftwareStateDto.State? = null

) {

    /**
     * 
     *
     * Values: offering,push,delivery,deploy,installed,uninstalled
     */
    @JsonClass(generateAdapter = false)
    enum class State(val value: kotlin.String) {
        @Json(name = "offering") offering("offering"),
        @Json(name = "push") push("push"),
        @Json(name = "delivery") delivery("delivery"),
        @Json(name = "deploy") deploy("deploy"),
        @Json(name = "installed") installed("installed"),
        @Json(name = "uninstalled") uninstalled("uninstalled");
    }
}

