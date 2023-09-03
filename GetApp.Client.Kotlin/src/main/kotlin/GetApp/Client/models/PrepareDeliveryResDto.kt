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
 * @param catalogId 
 * @param status 
 * @param url 
 */


data class PrepareDeliveryResDto (

    @Json(name = "catalogId")
    val catalogId: kotlin.String,

    @Json(name = "status")
    val status: PrepareDeliveryResDto.Status,

    @Json(name = "url")
    val url: kotlin.String? = null

) {

    /**
     * 
     *
     * Values: start,inProgress,done,error
     */
    @JsonClass(generateAdapter = false)
    enum class Status(val value: kotlin.String) {
        @Json(name = "start") start("start"),
        @Json(name = "inProgress") inProgress("inProgress"),
        @Json(name = "done") done("done"),
        @Json(name = "error") error("error");
    }
}

