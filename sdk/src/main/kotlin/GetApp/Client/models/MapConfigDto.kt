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
 * @param deliveryTimeoutMins 
 * @param maxMapAreaSqKm 
 * @param maxMapSizeInMB 
 * @param maxParallelDownloads 
 * @param downloadRetryTime 
 * @param downloadTimeoutMins 
 * @param periodicInventoryIntervalMins 
 * @param periodicConfIntervalMins 
 * @param periodicMatomoIntervalMins 
 * @param minAvailableSpaceMB 
 * @param mapMinInclusionInPercentages 
 * @param matomoUrl 
 * @param matomoDimensionId 
 * @param matomoSiteId 
 * @param sdStoragePath 
 * @param flashStoragePath 
 * @param targetStoragePolicy 
 * @param sdInventoryMaxSizeMB 
 * @param flashInventoryMaxSizeMB 
 * @param lastCheckingMapUpdatesDate 
 * @param lastConfigUpdateDate 
 */


data class MapConfigDto (

    @Json(name = "deliveryTimeoutMins")
    val deliveryTimeoutMins: java.math.BigDecimal? = null,

    @Json(name = "MaxMapAreaSqKm")
    val maxMapAreaSqKm: java.math.BigDecimal? = null,

    @Json(name = "maxMapSizeInMB")
    val maxMapSizeInMB: java.math.BigDecimal? = null,

    @Json(name = "maxParallelDownloads")
    val maxParallelDownloads: java.math.BigDecimal? = null,

    @Json(name = "downloadRetryTime")
    val downloadRetryTime: java.math.BigDecimal? = null,

    @Json(name = "downloadTimeoutMins")
    val downloadTimeoutMins: java.math.BigDecimal? = null,

    @Json(name = "periodicInventoryIntervalMins")
    val periodicInventoryIntervalMins: java.math.BigDecimal? = null,

    @Json(name = "periodicConfIntervalMins")
    val periodicConfIntervalMins: java.math.BigDecimal? = null,

    @Json(name = "periodicMatomoIntervalMins")
    val periodicMatomoIntervalMins: java.math.BigDecimal? = null,

    @Json(name = "minAvailableSpaceMB")
    val minAvailableSpaceMB: java.math.BigDecimal? = null,

    @Json(name = "mapMinInclusionInPercentages")
    val mapMinInclusionInPercentages: java.math.BigDecimal? = null,

    @Json(name = "matomoUrl")
    val matomoUrl: kotlin.String? = null,

    @Json(name = "matomoDimensionId")
    val matomoDimensionId: kotlin.String? = null,

    @Json(name = "matomoSiteId")
    val matomoSiteId: kotlin.String? = null,

    @Json(name = "sdStoragePath")
    val sdStoragePath: kotlin.String? = null,

    @Json(name = "flashStoragePath")
    val flashStoragePath: kotlin.String? = null,

    @Json(name = "targetStoragePolicy")
    val targetStoragePolicy: MapConfigDto.TargetStoragePolicy? = TargetStoragePolicy.sDOnly,

    @Json(name = "sdInventoryMaxSizeMB")
    val sdInventoryMaxSizeMB: java.math.BigDecimal? = null,

    @Json(name = "flashInventoryMaxSizeMB")
    val flashInventoryMaxSizeMB: java.math.BigDecimal? = null,

    @Json(name = "lastCheckingMapUpdatesDate")
    val lastCheckingMapUpdatesDate: java.time.OffsetDateTime? = null,

    @Json(name = "lastConfigUpdateDate")
    val lastConfigUpdateDate: java.time.OffsetDateTime? = null

) {

    /**
     * 
     *
     * Values: sDOnly,flashThenSD,sDThenFlash,flashOnly
     */
    @JsonClass(generateAdapter = false)
    enum class TargetStoragePolicy(val value: kotlin.String) {
        @Json(name = "SDOnly") sDOnly("SDOnly"),
        @Json(name = "FlashThenSD") flashThenSD("FlashThenSD"),
        @Json(name = "SDThenFlash") sDThenFlash("SDThenFlash"),
        @Json(name = "FlashOnly") flashOnly("FlashOnly");
    }
}

