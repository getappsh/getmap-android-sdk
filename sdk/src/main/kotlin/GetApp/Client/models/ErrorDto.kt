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
 * @param errorCode `MAP.unknown`: Error code not listed in the enum <br /> `MAP.notFound`: No found the map with given id <br /> `MAP.bBoxIsInvalid`: BBox is probably invalid <br /> `MAP.bBoxNotInAnyPolygon`: The given BBox in not contains in any polygon <br /> `MAP.exportMapFailed`: Some error occurs when import map <br /> `MAP.requestInProgress`: Delivery was already requested and in processing! <br /> `MAP.areaTooLarge`: Area too large to distribute, reduce request size and try again <br /> `MAP.areaTooSmall`: Area too small to distribute, increase request size and try again . 
 * @param message 
 */


data class ErrorDto (

    /* `MAP.unknown`: Error code not listed in the enum <br /> `MAP.notFound`: No found the map with given id <br /> `MAP.bBoxIsInvalid`: BBox is probably invalid <br /> `MAP.bBoxNotInAnyPolygon`: The given BBox in not contains in any polygon <br /> `MAP.exportMapFailed`: Some error occurs when import map <br /> `MAP.requestInProgress`: Delivery was already requested and in processing! <br /> `MAP.areaTooLarge`: Area too large to distribute, reduce request size and try again <br /> `MAP.areaTooSmall`: Area too small to distribute, increase request size and try again .  */
    @Json(name = "errorCode")
    val errorCode: ErrorDto.ErrorCode,

    @Json(name = "message")
    val message: kotlin.String? = null

) {

    /**
     * `MAP.unknown`: Error code not listed in the enum <br /> `MAP.notFound`: No found the map with given id <br /> `MAP.bBoxIsInvalid`: BBox is probably invalid <br /> `MAP.bBoxNotInAnyPolygon`: The given BBox in not contains in any polygon <br /> `MAP.exportMapFailed`: Some error occurs when import map <br /> `MAP.requestInProgress`: Delivery was already requested and in processing! <br /> `MAP.areaTooLarge`: Area too large to distribute, reduce request size and try again <br /> `MAP.areaTooSmall`: Area too small to distribute, increase request size and try again . 
     *
     * Values: unknown,notFound,bBoxIsInvalid,bBoxNotInAnyPolygon,exportMapFailed,requestInProgress,areaTooLarge,areaTooSmall
     */
    @JsonClass(generateAdapter = false)
    enum class ErrorCode(val value: kotlin.String) {
        @Json(name = "MAP.unknown") unknown("MAP.unknown"),
        @Json(name = "MAP.notFound") notFound("MAP.notFound"),
        @Json(name = "MAP.bBoxIsInvalid") bBoxIsInvalid("MAP.bBoxIsInvalid"),
        @Json(name = "MAP.bBoxNotInAnyPolygon") bBoxNotInAnyPolygon("MAP.bBoxNotInAnyPolygon"),
        @Json(name = "MAP.exportMapFailed") exportMapFailed("MAP.exportMapFailed"),
        @Json(name = "MAP.requestInProgress") requestInProgress("MAP.requestInProgress"),
        @Json(name = "MAP.areaTooLarge") areaTooLarge("MAP.areaTooLarge"),
        @Json(name = "MAP.areaTooSmall") areaTooSmall("MAP.areaTooSmall");
    }
}
