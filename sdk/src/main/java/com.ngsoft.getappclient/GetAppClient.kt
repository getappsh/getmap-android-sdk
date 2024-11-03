package com.ngsoft.getappclient

import GetApp.Client.apis.DeliveryApi
import GetApp.Client.apis.DeviceApi
import GetApp.Client.apis.DeviceBugReportApi
import GetApp.Client.apis.DeviceDiscoveryApi
import GetApp.Client.apis.GetMapApi
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance


//TODO make it singleton
internal class GetAppClient(config: ConnectionConfig) {

    private val TAG = "GetAppClient"

    val deviceDiscoverApi: DeviceDiscoveryApi
    val getMapApi: GetMapApi
    val deliveryApi: DeliveryApi
    val bugReportApi: DeviceBugReportApi
    val deviceApi: DeviceApi

    init {
        if (config.baseUrl.isEmpty())
            throw Exception("Base url is empty")
        if (config.user.isEmpty())
            throw Exception("User is empty")
        if (config.password.isEmpty())
            throw Exception("Password is empty")

        Timber.i("GetApp base url = ${config.baseUrl}")
        Timber.i("GetApp user = ${config.user}")

        val tokenProvider = AccessTokenProvider(config);

        val client = OkHttpClient.Builder()
            .authenticator(AccessTokenAuthenticator(tokenProvider))
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer ${tokenProvider.token()}")
                    .build()
                chain.proceed(request)
            })
            .addInterceptor(VpnExceptionInterceptor())
            .build()

        deviceDiscoverApi = DeviceDiscoveryApi(config.baseUrl, client)
        getMapApi = GetMapApi(config.baseUrl, client)
        deliveryApi = DeliveryApi(config.baseUrl, client)
        bugReportApi = DeviceBugReportApi(config.baseUrl, client)
        deviceApi = DeviceApi(config.baseUrl, client);

    }

    private inline fun <reified T> setAccessToken(token: String) {
        val companionObject = T::class.companionObject
        if (companionObject != null) {
            val companionInstance = T::class.companionObjectInstance
            val property = companionObject.members.first { it.name == "accessToken" } as KMutableProperty<*>?
            property?.setter?.call(companionInstance, token)
        }
    }

    @Throws(IOException::class)
    fun uploadFile(url: String, filePath: String) {
        val client = OkHttpClient()
        val file = File(filePath)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to upload file: ${response.code}")
        }
    }



}