package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.CreateImportDto
import GetApp.Client.models.CreateImportResDto
import GetApp.Client.models.ErrorDto
import GetApp.Client.models.ImportStatusResDto
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getappclient.GetAppClient
import java.math.BigDecimal

internal object MapImportClient {
    fun getCreateMapImportStatus(client: GetAppClient, inputImportRequestId: String?): CreateMapImportStatus? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.getMapApi.getMapControllerGetImportStatus(inputImportRequestId)
        val result = CreateMapImportStatus()
        result.importRequestId = inputImportRequestId
        result.statusCode = Status()

        result.progress = status.metaData?.progress
        result.url = status.metaData?.packageUrl

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
                result.statusCode!!.messageLog = status.error?.message
                result.state = MapImportState.IN_PROGRESS
            }
            ImportStatusResDto.Status.pending -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            ImportStatusResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = status.error?.message
                result.state = MapImportState.CANCEL
            }
            ImportStatusResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.NOT_FOUND
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)

                if(status.error?.errorCode == ErrorDto.ErrorCode.notFound)
                    result.statusCode!!.statusCode = StatusCode.REQUEST_ID_NOT_FOUND

                result.state = MapImportState.ERROR
            }
            ImportStatusResDto.Status.pause -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            ImportStatusResDto.Status.expired -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            ImportStatusResDto.Status.archived -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            else -> {
                result.state = MapImportState.ERROR
                if(status.error?.errorCode == ErrorDto.ErrorCode.notFound)
                    result.statusCode!!.statusCode = StatusCode.REQUEST_ID_NOT_FOUND
                else
                    result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR

            }
        }

        return result
    }

    fun createMapImport(client: GetAppClient, inputProperties: MapProperties?, deviceId: String): CreateMapImportStatus? {
        if(inputProperties == null)
            throw Exception("invalid inputProperties")

        val params = CreateImportDto(deviceId, GetApp.Client.models.MapProperties(
            boundingBox=inputProperties.boundingBox, productName="dummy name", productId=inputProperties.productId,
            zoomLevel = BigDecimal(12), targetResolution = BigDecimal(0), lastUpdateAfter=BigDecimal(0)
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
            CreateImportResDto.Status.pending -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            CreateImportResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.CANCEL
            }
            CreateImportResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            CreateImportResDto.Status.pause -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            CreateImportResDto.Status.expired -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            CreateImportResDto.Status.archived -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }

            else -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.statusCode!!.messageLog = status.error?.message
                result.state = MapImportState.ERROR

            }
        }

        return result
    }
}