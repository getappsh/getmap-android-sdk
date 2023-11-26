package com.getapp.technician.mockserver

import GetApp.Client.infrastructure.Serializer
import GetApp.Client.models.CreateImportResDto
import GetApp.Client.models.ImportStatusResDto
import GetApp.Client.models.PrepareDeliveryResDto
import GetApp.Client.models.TokensDto
import android.content.res.AssetManager
import android.util.Log
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import okio.Buffer
import okio.source
import okio.use
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class MockServerResponses(private val assets: AssetManager) {
    private val _tag = "MockServerResponses"

    private val baseDownloadUrl = "http://localhost:3333/api/download/"
    private val mapFile = "Orthophoto_gaza_north_2_0_19_2023_11_21T13_13_01_792Z.gpkg"
    private val jsonFile = "Orthophoto_gaza_north_2_0_19_2023_11_21T13_13_01_792Z.json"


    fun getLogin(config: MockConfig): MockResponse {
        val token = TokensDto("access-token", "refresh-token")
        return MockResponse()
            .setBody(toJsonString(token))
            .setResponseCode(201)
    }

    fun getDiscovery(config: MockConfig): MockResponse {
        Log.d(_tag, "getDiscovery - timeout: " + config.discoveryTimeOut)
        return MockResponse()
            .setBody(readFile("discovery.json"))
            .throttleBody(1024, config.discoveryTimeOut.toLong(), TimeUnit.SECONDS)
            .setResponseCode(201)
    }

    fun importCreate(config: MockConfig): MockResponse{
        val import = CreateImportResDto("test-1", CreateImportResDto.Status.inProgress)

        return MockResponse()
            .setBody(toJsonString(import))
            .setResponseCode(201)

    }
    fun importStatus(config: MockConfig, path: String): MockResponse{
        val reqId = path.substring( path.lastIndexOf('/') + 1, path.length)
        val status = ImportStatusResDto.Status.done
        val importRes = ImportStatusResDto(
            status = status,
            deviceId = "device-1",
            importRequestId = reqId,
            packageUrl = baseDownloadUrl + mapFile,
            fileName = mapFile,
            createDate = OffsetDateTime.now())

        return MockResponse()
            .setBody(toJsonString(importRes))
            .setResponseCode(200)
    }

    fun prepareDelivery(config: MockConfig, body: Buffer): MockResponse{
        val catalogId = JSONObject(body.readUtf8()).get("catalogId").toString()
        val response =PrepareDeliveryResDto(
            catalogId = catalogId,
            status = PrepareDeliveryResDto.Status.done,
            url = baseDownloadUrl + mapFile)

        return MockResponse()
            .setBody(toJsonString(response))
            .setResponseCode(201)
    }

    fun preparedDelivery(config: MockConfig, path: String): MockResponse{
        val catalogId = path.substring( path.lastIndexOf('/') + 1, path.length)
        val response =PrepareDeliveryResDto(
            catalogId = catalogId,
            status = PrepareDeliveryResDto.Status.done,
            url = baseDownloadUrl + mapFile)

        return MockResponse()
            .setBody(toJsonString(response))
            .setResponseCode(200)
    }


    fun downloadFile(config: MockConfig, path: String): MockResponse{

        val fileName = if (path.substring( path.lastIndexOf('.') + 1, path.length) == "json"){
            if(config.failedValidation) "E-$jsonFile" else jsonFile
        }else{
            mapFile
        }
        val file = assets.open(fileName)
        val period = if (config.fastDownload) 5L else 50L

        return MockResponse()
            .setBody(Buffer().apply {
                writeAll(file.source())
            })
            .throttleBody(1024, period, TimeUnit.MILLISECONDS)

    }

    private fun readFile(fileName: String): String{
        return assets.open(fileName).bufferedReader().use{
            it.readText()
        }
    }

    private inline fun <reified T>toJsonString(content: T): String{
        return Serializer.moshi.adapter(T::class.java).toJson(content)
    }
}