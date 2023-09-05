package com.ngsoft.getappclient

import GetApp.Client.apis.DeviceApi
import GetApp.Client.apis.LoginApi
import GetApp.Client.infrastructure.ApiClient
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

class GetAppClient(config: ConnectionConfig) {

    private val tokens: TokensDto
    val deviceApi: DeviceApi

    init {
        if (config.baseUrl.isEmpty())
            throw Exception("Base url is empty")
        if (config.user.isEmpty())
            throw Exception("User is empty")
        if (config.password.isEmpty())
            throw Exception("Password is empty")

        println("GetApp base url = ${config.baseUrl}")
        //todo: remove pwd later
        println("GetApp user = ${config.user}, password = ${config.password}")
        println("Logging in...")

        tokens = LoginApi(config.baseUrl).loginControllerGetToken(
            UserLoginDto(config.user, config.password)
        )

        println("Logged in, access token = ${tokens.accessToken}")

        setAccessToken<ApiClient>(tokens.accessToken.toString())

        deviceApi = DeviceApi(config.baseUrl)

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


}