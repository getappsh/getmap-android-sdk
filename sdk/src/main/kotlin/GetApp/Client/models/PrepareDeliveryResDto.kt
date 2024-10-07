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

import GetApp.Client.models.DeliveryItemDto
import GetApp.Client.models.ErrorDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param catalogId 
 * @param status 
 * @param progress 
 * @param propertySize 
 * @param url 
 * @param artifacts 
 * @param error 
 */


data class PrepareDeliveryResDto (

    @Json(name = "catalogId")
    val catalogId: kotlin.String,

    @Json(name = "status")
    val status: PrepareDeliveryResDto.Status,

    @Json(name = "progress")
    val progress: java.math.BigDecimal? = null,

    @Json(name = "size")
    val propertySize: java.math.BigDecimal? = null,

    @Json(name = "url")
    @Deprecated(message = "This property is deprecated.")
    val url: kotlin.String? = null,

    @Json(name = "artifacts")
    val artifacts: kotlin.collections.List<DeliveryItemDto>? = null,

    @Json(name = "error")
    val error: ErrorDto? = null

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

