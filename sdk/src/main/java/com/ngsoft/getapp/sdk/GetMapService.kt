package com.ngsoft.getapp.sdk

import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import java.time.LocalDateTime

interface GetMapService {

    fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String?

    /**
     * Purge cache registry (cached tile files remains intact)
     *
     */
    fun purgeCache()

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

}