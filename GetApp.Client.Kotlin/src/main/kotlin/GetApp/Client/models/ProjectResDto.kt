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

import GetApp.Client.models.MemberResDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param id 
 * @param componentName 
 * @param OS 
 * @param platformType 
 * @param formation 
 * @param category 
 * @param artifactType 
 * @param tokens 
 * @param description 
 * @param members 
 */


data class ProjectResDto (

    @Json(name = "id")
    val id: java.math.BigDecimal? = null,

    @Json(name = "componentName")
    val componentName: kotlin.String? = null,

    @Json(name = "OS")
    val OS: kotlin.String? = null,

    @Json(name = "platformType")
    val platformType: kotlin.String? = null,

    @Json(name = "formation")
    val formation: kotlin.String? = null,

    @Json(name = "category")
    val category: kotlin.String? = null,

    @Json(name = "artifactType")
    val artifactType: kotlin.String? = null,

    @Json(name = "tokens")
    val tokens: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "description")
    val description: kotlin.String? = null,

    @Json(name = "members")
    val members: kotlin.collections.List<MemberResDto>? = null

)

