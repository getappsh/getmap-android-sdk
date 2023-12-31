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
 * @param deviceId 
 * @param importRequestId 
 * @param packageUrl 
 * @param fileName 
 * @param createDate 
 * @param status 
 * @param messageLog 
 */


data class ImportStatusResDto (

    @Json(name = "deviceId")
    val deviceId: kotlin.String? = null,

    @Json(name = "importRequestId")
    val importRequestId: kotlin.String? = null,

    @Json(name = "packageUrl")
    val packageUrl: kotlin.String? = null,

    @Json(name = "fileName")
    val fileName: kotlin.String? = null,

    @Json(name = "createDate")
    val createDate: java.time.OffsetDateTime? = null,

    @Json(name = "status")
    val status: ImportStatusResDto.Status? = null,

    @Json(name = "messageLog")
    val messageLog: kotlin.String? = null

) {

    /**
     * 
     *
     * Values: start,inProgress,done,cancel,pause,error,pending,expired,archived
     */
    @JsonClass(generateAdapter = false)
    enum class Status(val value: kotlin.String) {
        @Json(name = "Start") start("Start"),
        @Json(name = "InProgress") inProgress("InProgress"),
        @Json(name = "Done") done("Done"),
        @Json(name = "Cancel") cancel("Cancel"),
        @Json(name = "Pause") pause("Pause"),
        @Json(name = "Error") error("Error"),
        @Json(name = "Pending") pending("Pending"),
        @Json(name = "Expired") expired("Expired"),
        @Json(name = "Archived") archived("Archived");
    }
}

