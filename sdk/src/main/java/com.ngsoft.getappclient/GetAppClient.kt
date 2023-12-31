package com.ngsoft.getappclient

import GetApp.Client.apis.DeliveryApi
import GetApp.Client.apis.DeviceApi
import GetApp.Client.apis.GetMapApi
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

internal class GetAppClient(config: ConnectionConfig) {

    private val TAG = "GetAppClient"

    val deviceApi: DeviceApi
    val getMapApi: GetMapApi
    val deliveryApi: DeliveryApi

    init {
        if (config.baseUrl.isEmpty())
            throw Exception("Base url is empty")
        if (config.user.isEmpty())
            throw Exception("User is empty")
        if (config.password.isEmpty())
            throw Exception("Password is empty")

        Log.i(TAG, "GetApp base url = ${config.baseUrl}")
        Log.i(TAG, "GetApp user = ${config.user}")

        val tokenProvider = AccessTokenProvider(config);

        val client = OkHttpClient.Builder()
            .authenticator(AccessTokenAuthenticator(tokenProvider))
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer ${tokenProvider.token()}")
                    .build()
                chain.proceed(request)
            })
            .build()

        deviceApi = DeviceApi(config.baseUrl, client)
        getMapApi = GetMapApi(config.baseUrl, client)
        deliveryApi = DeliveryApi(config.baseUrl, client)

    }

    private inline fun <reified T> setAccessToken(token: String) {
        val companionObject = T::class.companionObject
        if (companionObject != null) {
            val companionInstance = T::class.companionObjectInstance
            val property = companionObject.members.first { it.name == "accessToken" } as KMutableProperty<*>?
            property?.setter?.call(companionInstance, token)
        }
    }

}