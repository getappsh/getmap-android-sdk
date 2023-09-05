package com.ngsoft.getapp.sdk

import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoveryResDto
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient

class GetMapServiceImpl (configuration: Configuration) : GetMapService {

    private val client: GetAppClient

    init {
        client = GetAppClient(ConnectionConfig(configuration.baseUrl, configuration.user, configuration.password))
    }

    override fun getDiscoveryCatalog(query: DiscoveryMessageDto): DiscoveryResDto {
        return client.deviceApi.deviceControllerDiscoveryCatalog(query)
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = CreateMapImportStatus()
        status.importRequestId = inputImportRequestId
        status.statusCode = Status()
        status.statusCode!!.statusCode = StatusCode.SUCCESS
        status.state = MapImportState.START
        return status
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        if(inputProperties == null)
            throw Exception("invalid inputProperties")

        val status = CreateMapImportStatus()
        status.statusCode = Status()
        status.statusCode!!.statusCode = StatusCode.SUCCESS
        status.state = MapImportState.IN_PROGRESS
        return status
    }

    override fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.importRequestId = inputImportRequestId
        status.message = Status()
        status.message!!.statusCode = StatusCode.SUCCESS
        status.state = MapDeliveryState.CONTINUE
        return status
    }

    override fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.importRequestId = inputImportRequestId
        status.message = Status()
        status.message!!.statusCode = StatusCode.SUCCESS
        status.state = MapDeliveryState.START
        return status
    }

    override fun setMapImportDeliveryPause(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.importRequestId = inputImportRequestId
        status.message = Status()
        status.message!!.statusCode = StatusCode.SUCCESS
        status.state = MapDeliveryState.PAUSE
        return status
    }

    override fun setMapImportDeliveryCancel(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.importRequestId = inputImportRequestId
        status.message = Status()
        status.message!!.statusCode = StatusCode.SUCCESS
        status.state = MapDeliveryState.CANCEL
        return status
    }

    override fun setMapImportDeploy(
        inputImportRequestId: String?,
        inputState: MapDeployState?
    ): MapDeployState? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")


        return MapDeployState.DONE
    }

}