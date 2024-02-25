package com.ngsoft.getapp.sdk

import GetApp.Client.models.MapConfigDto
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.getapp.sdk.old.DownloadProgress
import java.time.LocalDateTime
import java.time.OffsetDateTime

interface GetMapService {


    /**
     * Configuration settings of the map service.
     */
    val config: GeneralConfig

    /**
     * Get Downloaded map by id
     *
     * @param id Map id
     * @return MapData?
     */
    fun getDownloadedMap(id: String): MapData?

    /**
     * Get Downloaded maps
     *
     * @return List<MapData>
     */
    fun getDownloadedMaps(): List<MapData>

    /**
     * Get Downloaded maps
     *
     * @return LiveData<List<MapData>>
     */
    fun getDownloadedMapsLive(): LiveData<List<MapData>>

    /**
     * Delete Map
     *
     * @param id Map id
     * @throws Exception if map is on download process
     */
    fun deleteMap(id: String)

    /**
     * Cancel Download
     *
     * @param id Map id
     */
    fun cancelDownload(id: String)

    /**
     * Register download handler to on going download
     *
     * @param id Map id
     * @param downloadStatusHandler delivery progress handler
     * @receiver see [MapData]
     */
    fun registerDownloadHandler(id: String, downloadStatusHandler: (MapData) -> Unit)
    /**
     * Resume download
     *
     * @param id Map id
     * @param downloadStatusHandler delivery progress handler
     * @receiver see [MapData]
     * @return map download id
     */
    fun resumeDownload(id: String, downloadStatusHandler: (MapData) -> Unit): String

    /**
     * Deliver extent tiles
     *
     * @param mp map properties to deliver
     * @param downloadStatusHandler delivery progress handler
     * @receiver see [MapData]
     * @return map download id
     */
    fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapData) -> Unit): String?

    /**
     * Download updated map
     *
     * @param id Map id
     * @param downloadStatusHandler delivery progress handler
     * @receiver see [MapData]
     * @return map download id
     */
    fun downloadUpdatedMap(id: String, downloadStatusHandler: (MapData) -> Unit): String?

    /**
     * Synchronize Map data by reading the files from storage, and syncing them against the DB
     */
    fun synchronizeMapData()

    /**
     * Purge cache registry (cached tile files remains intact)
     *
     */
    fun purgeCache()

    /**
     * Fetch inventory Updates from the server
     * @return list of ids that can be updates
     * @throws Exception when the request to server failed for some reason
     */
    @Throws(Exception::class)
    fun fetchInventoryUpdates(): List<String>

    /**
     * Fetch config Updates from the server
     * @return
     * @throws Exception when the request to server failed for some reason
     */
    @Throws(Exception::class)
    fun fetchConfigUpdates()
    /**
     * Set listener to get notified when there is a new map update
     */
    fun setOnInventoryUpdatesListener(listener: (List<String>) -> Unit)

    /**
     * Generate QR code from map json file.
     * @param id Map id
     * @param width of the QR code
     * @param height of the QR code
     */
    fun generateQrCode(id: String, width: Int=1000, height: Int=1000): Bitmap

    /**
     * Process QR code data.
     * @param data from the QR code
     * @param downloadStatusHandler delivery progress handler
     * @receiver see [MapData]
     * @return map download id
     */
    fun processQrCodeData(data: String, downloadStatusHandler: (MapData) -> Unit): String

    /**
     * Get extent updates
     *
     * @param extent of map to get tiles updates for
     * @param updateDate to lookup cached tile against. Call getDiscoveryCatalog() to get update date for the layer of interest.
     * @return list of tile updates
     */

    fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile>

    /**
     * Deliver extent tiles
     *
     * @param extentTiles to deliver
     * @param onProgress delivery progress handler
     * @receiver see [DownloadProgress]
     * @return list of *actually* delivered tiles (echo semantics)
     */
    fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile>


    /**
     * Fetches product catalog
     *
     * @param inputProperties query params (for future use, currently gets all items available)
     * @return collection of catalog items
     */
    fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem>


    /**
     * Fetches the status of a map creation import.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @return CreateMapImportStatus? The status of the map creation import. Can be null.
     */
    fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus?

    /**
     * Initiates a map import process.
     *
     * @param inputProperties The properties for the map import. Can be null.
     * @return CreateMapImportStatus? The status of the map creation import. Can be null.
     */
    fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus?

    /**
     * Fetches the delivery status of a map import.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @return MapImportDeliveryStatus? The delivery status of the map import. Can be null.
     */
    fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus?

    /**
     * Starts the delivery process for a map import.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @return MapImportDeliveryStatus? The updated delivery status of the map import. Can be null.
     */
    fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus?

    /**
     * Pauses the delivery process for a map import.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @return MapImportDeliveryStatus? The updated delivery status of the map import. Can be null.
     */
    fun setMapImportDeliveryPause(inputImportRequestId: String?): MapImportDeliveryStatus?

    /**
     * Cancels the delivery process for a map import.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @return MapImportDeliveryStatus? The updated delivery status of the map import. Can be null.
     */
    fun setMapImportDeliveryCancel(inputImportRequestId: String?): MapImportDeliveryStatus?

    /**
     * Deploys the imported map to device (downloads a file to a path within the public external storage directory).
     * Storage permissions are not handled in SDK library - it's up to the app.
     * That's blocking method (@hanokhaloni - I suggest callbacks in revised version).
     * Download completion wait loop time-out is hard-coded to 15 minutes.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @param inputState The state for the map deployment. Can be null.
     * @return MapDeployState? The updated deployment state of the map. Can be null.
     */
    fun setMapImportDeploy(inputImportRequestId: String?,inputState: MapDeployState?): MapDeployState?


    interface GeneralConfig {

        /**
         * Server URL
         */
        val baseUrl: String

        /**
         * SD Storage path to stored maps
         */

        val sdStoragePath: String


        /**
         * Flash Storage path to stored maps
         */

        val flashStoragePath: String

        /**
         * Storage policy
         */
        var targetStoragePolicy: MapConfigDto.TargetStoragePolicy

        /**
         * Path where downloaded maps are saved.
         */
        val downloadPath: String

        /**
         * Timeout duration for map delivery operations in minutes.
         */
        var deliveryTimeoutMins: Int

        /**
         * Timeout duration for map download operations in minutes.
         */
        var downloadTimeoutMins: Int

        /**
         * Number of retries allowed for map downloads.
         */
        var downloadRetry: Int

        /**
         * Maximum allowable size for a map in megabytes.
         */
        var maxMapSizeInMB: Long

        /**
         * Maximum allowable size for a map in Square Kilometer.
         */
        val maxMapAreaSqKm: Long

        /**
         * Maximum number of parallel downloads allowed.
         */
        var maxParallelDownloads: Int

        /**
         * Interval for periodic inventory execution in minutes.
         */
        var periodicInventoryIntervalMins: Int

        /**
         * Run config job, set to false when admin controls the config.
         */
        var applyServerConfig: Boolean

        /**
         * Interval for periodic map configuration updates in minutes.
         */
        var periodicConfIntervalMins: Int

        /**
         * Matomo URL.
         */
        var matomoUrl: String

        /**
         * Matomo Dimension id
         */
        var matomoDimensionId: String


        /**
         * Matomo Site id
         */
        var matomoSiteId: String


        /**
         * Interval for Matomo updates in minutes.
         */
        var matomoUpdateIntervalMins: Int

        /**
         * Minimum available space required on the device for map operations.
         */
        var minAvailableSpaceMB: Long

        /**
         * Minimum Inclusion of footprint in a map product in percentages.
         */
        val mapMinInclusionPct: Int

        /**
         * Last configuration check timestamp.
         */
        var lastConfigCheck: OffsetDateTime?

        /**
         * Last inventory check timestamp.
         */
        var lastInventoryCheck: OffsetDateTime?

        /**
         * Last server configuration update timestamp.
         */
        var lastServerConfigUpdate: OffsetDateTime?

        /**
         * Last server inventory job
         */
        var lastServerInventoryJob: OffsetDateTime?
    }
}