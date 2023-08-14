package com.ngsoft.getapp.sdk

interface GetMapService {
    fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus?
    fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus?
    fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus?
    fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus?
    fun setMapImportDeliveryPause(inputImportRequestId: String?): MapImportDeliveryStatus?
    fun setMapImportDeliveryCancel(inputImportRequestId: String?): MapImportDeliveryStatus?
    fun setMapImportDeploy(
        inputImportRequestId: String?,
        inputState: MapDeployState?
    ): MapDeployState?
}