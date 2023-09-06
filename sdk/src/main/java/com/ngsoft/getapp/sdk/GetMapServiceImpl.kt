package com.ngsoft.getapp.sdk

import GetApp.Client.models.ComponentDto
import GetApp.Client.models.CreateImportDto
import GetApp.Client.models.CreateImportResDto
import GetApp.Client.models.DiscoveryMapDto
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoverySoftwareDto
import GetApp.Client.models.GeneralDiscoveryDto
import GetApp.Client.models.GeoLocationDto
import GetApp.Client.models.ImportStatusResDto
import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.PlatformDto
import GetApp.Client.models.SituationalDiscoveryDto
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class GetMapServiceImpl (configuration: Configuration) : GetMapService {

    private val client: GetAppClient

    init {
        client = GetAppClient(ConnectionConfig(configuration.baseUrl, configuration.user, configuration.password))
    }

    override fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem> {

        //fill that vast GetApp param...
        val query = DiscoveryMessageDto(DiscoveryMessageDto.DiscoveryType.app,
            GeneralDiscoveryDto(
                PersonalDiscoveryDto("tank","idNumber-123","personalNumber-123"),
                SituationalDiscoveryDto( BigDecimal("23"), BigDecimal("2"),
                    OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC), true, BigDecimal("34"),
                    GeoLocationDto("33.4","23.3", "344")
                ),
                PhysicalDiscoveryDto(PhysicalDiscoveryDto.OSEnum.android,
                    "00-B0-D0-63-C2-26","129.2.3.4",
                    "4", "13kb23", "12kb", "1212Mb")
            ),

            DiscoverySoftwareDto("yatush", PlatformDto("Merkava","106", BigDecimal("223"),
                listOf(
                    ComponentDto(
                    "dummyCatId", "somename", "11", "N/A", BigDecimal("22"), "N/A"
                    ),
                    ComponentDto(
                        "dummyCatId", "somename", "22", "N/A", BigDecimal("33"), "N/A"
                    )
                )
            )),

            DiscoveryMapDto("if32","map4","3","osm","bla-bla",
                inputProperties.boundingBox.toString(),
                "WGS84", LocalDateTime.now().toString(), LocalDateTime.now().toString(), LocalDateTime.now().toString(),
                "DJI Mavic","raster","N/A","ME","CCD","3.14","0.12"
            )
        )

        val discoveries = client.deviceApi.deviceControllerDiscoveryCatalog(query)
        val result = mutableListOf<DiscoveryItem>()
        discoveries.map?.forEach {
            //println(it)
            result.add(DiscoveryItem(it.productId.toString(),it.productName.toString(), it.boundingBox.toString()))
        }

        return result
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.getMapApi.getMapControllerGetImportStatus(inputImportRequestId)
        val result = CreateMapImportStatus()
        result.importRequestId = inputImportRequestId
        result.statusCode = Status()

        when(status.status) {
            ImportStatusResDto.Status.start -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.START
            }
            ImportStatusResDto.Status.done -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.DONE
            }
            ImportStatusResDto.Status.inProgress -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            ImportStatusResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.CANCEL
            }
            ImportStatusResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.NOT_FOUND

                if(status.fileName == "Request not found")
                    result.statusCode!!.statusCode = StatusCode.REQUEST_ID_NOT_FOUND

                result.state = MapImportState.ERROR
            }
            else -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.state = MapImportState.ERROR
            }
        }

        return result
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        if(inputProperties == null)
            throw Exception("invalid inputProperties")

        val params = CreateImportDto("getapp-server", GetApp.Client.models.MapProperties(
            BigDecimal(12), "dummy name", inputProperties.productId, inputProperties.boundingBox,
            BigDecimal(0), BigDecimal(0)
        ))

        val status = client.getMapApi.getMapControllerCreateImport(params)
        val result = CreateMapImportStatus()
        result.importRequestId = status.importRequestId
        result.statusCode = Status()

        when(status.status) {
            CreateImportResDto.Status.start -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.START
            }
            CreateImportResDto.Status.done -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.DONE
            }
            CreateImportResDto.Status.inProgress -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            CreateImportResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.CANCEL
            }
            CreateImportResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.state = MapImportState.ERROR
            }
            else -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.state = MapImportState.ERROR
            }
        }

        return result
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
