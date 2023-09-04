package com.ngsoft.getapp.sdk

import GetApp.Client.apis.DeviceApi
import GetApp.Client.apis.LoginApi
import GetApp.Client.infrastructure.ApiClient
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoveryResDto
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

class GetMapServiceImpl (configuration: Configuration) : GetMapService {

    private val tokens: TokensDto
    private val deviceApi: DeviceApi

    init {
        if (configuration.baseUrl.isEmpty())
            throw Exception("Base url is empty")
        if (configuration.user.isEmpty())
            throw Exception("User is empty")
        if (configuration.password.isEmpty())
            throw Exception("Password is empty")

        println("GetApp base url = ${configuration.baseUrl}")
        //todo: remove pwd later
        println("GetApp user = ${configuration.user}, password = ${configuration.password}")
        println("Logging in...")

        tokens = LoginApi(configuration.baseUrl).loginControllerGetToken(
            UserLoginDto(configuration.user, configuration.password))

        println("Logged in, access token = ${tokens.accessToken}")

        setAccessToken<ApiClient>(tokens.accessToken.toString())

        deviceApi = DeviceApi(configuration.baseUrl)

    }

    private inline fun <reified T> setAccessToken(token: String) {
        val companionObject = T::class.companionObject
        if (companionObject != null) {
            val companionInstance = T::class.companionObjectInstance
            val property = companionObject.members.first { it.name == "accessToken" } as KMutableProperty<*>?
            property?.setter?.call(companionInstance, token)
//            val curr = property?.getter?.call(companionInstance)
//            println("set access token = ${curr.toString()}")
        }
    }

    override fun getDiscoveryCatalog(query: DiscoveryMessageDto): DiscoveryResDto {
        return deviceApi.deviceControllerDiscoveryCatalog(query)
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