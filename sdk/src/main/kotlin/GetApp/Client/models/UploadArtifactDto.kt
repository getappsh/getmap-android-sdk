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
 * @param platform 
 * @param component 
 * @param formation 
 * @param OS 
 * @param version 
 * @param releaseNotes 
 * @param propertySize 
 * @param url 
 * @param artifactType 
 * @param uploadToken 
 */


data class UploadArtifactDto (

    @Json(name = "platform")
    val platform: kotlin.String? = null,

    @Json(name = "component")
    val component: kotlin.String? = null,

    @Json(name = "formation")
    val formation: kotlin.String? = null,

    @Json(name = "OS")
    val OS: kotlin.String? = null,

    @Json(name = "version")
    val version: kotlin.String? = null,

    @Json(name = "releaseNotes")
    val releaseNotes: kotlin.String? = null,

    @Json(name = "size")
    val propertySize: kotlin.String? = null,

    @Json(name = "url")
    val url: kotlin.String? = null,

    @Json(name = "artifactType")
    val artifactType: kotlin.String? = null,

    @Json(name = "uploadToken")
    val uploadToken: kotlin.String? = null

)

