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
import GetApp.Client.models.ProjectResDto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 
 *
 * @param member 
 * @param project 
 */


data class MemberProjectResDto (

    @Json(name = "member")
    val member: MemberResDto? = null,

    @Json(name = "project")
    val project: ProjectResDto? = null

)

