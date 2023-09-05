package com.ngsoft.getapp.sdk

import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoveryResDto
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapProperties

interface GetMapService {

    fun getDiscoveryCatalog(query: DiscoveryMessageDto): DiscoveryResDto


    //obsolete
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