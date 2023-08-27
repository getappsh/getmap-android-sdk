package com.ngsoft.getapp.sdk

/**
 * Interface: GetMapService
 *
 * Provides services related to map import and deployment.
 */
interface GetMapService {

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
     * Deploys the imported map.
     *
     * @param inputImportRequestId The ID of the import request. Can be null.
     * @param inputState The state for the map deployment. Can be null.
     * @return MapDeployState? The updated deployment state of the map. Can be null.
     */
    fun setMapImportDeploy(
        inputImportRequestId: String?,
        inputState: MapDeployState?
    ): MapDeployState?
}