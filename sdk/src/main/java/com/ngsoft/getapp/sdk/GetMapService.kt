package com.ngsoft.getapp.sdk

import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapProperties
import java.time.LocalDateTime

interface GetMapService {


    /**
     * That's basically all ASIO needs 4 discovery - grab all updates available
     * and return  updates as list of (grid?) stamps
     */
    fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapProperties>

    /**
     * That's basically all ASIO needs 4 delivery:
     * 1. break down extent into grid's stamps
     * 2. deliver these stamps with progress updates 2 ASIO
     * 3. stamps management on device
     */
    fun deliverExtent(extent: MapProperties, onProgress: (Long) -> Unit)


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