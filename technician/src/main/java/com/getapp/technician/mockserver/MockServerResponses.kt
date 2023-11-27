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
import java.io.File
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

        val fileString = if (config.discoveryPath != null) {
            readFile(config.discoveryPath!!)
        }else {
            readFileFromAssets("discovery.json")
        }

        return MockResponse()
            .setBody(fileString)
            .throttleBody(1024, config.discoveryTimeOut.toLong(), TimeUnit.SECONDS)
            .setResponseCode(201)
    }

    fun importCreate(config: MockConfig): MockResponse{
        var error: String? = null
        if (config.importCreateStatus == CreateImportResDto.Status.error){
            error = "Libot failed to create requested map";
        }
        val import = CreateImportResDto("test-1", config.importCreateStatus, messageLog=error)

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
            packageUrl = baseDownloadUrl + getMapFileName(config),
            fileName = getMapFileName(config),
            createDate = OffsetDateTime.now())

        return MockResponse()
            .setBody(toJsonString(importRes))
            .setResponseCode(200)
    }

    private fun getMapFileName(config: MockConfig): String{
        val mapPath = config.mapPath
        return mapPath?.substring( mapPath.lastIndexOf('/') + 1, mapPath.length) ?: mapFile
    }

    fun prepareDelivery(config: MockConfig, body: Buffer): MockResponse{
        val catalogId = JSONObject(body.readUtf8()).get("catalogId").toString()
        val response = PrepareDeliveryResDto(
            catalogId = catalogId,
            status = PrepareDeliveryResDto.Status.done,
            url = baseDownloadUrl + getMapFileName(config))

        return MockResponse()
            .setBody(toJsonString(response))
            .setResponseCode(201)
    }

    fun preparedDelivery(config: MockConfig, path: String): MockResponse{
        val catalogId = path.substring( path.lastIndexOf('/') + 1, path.length)
        val response = PrepareDeliveryResDto(
            catalogId = catalogId,
            status = PrepareDeliveryResDto.Status.done,
            url = baseDownloadUrl + getMapFileName(config))

        return MockResponse()
            .setBody(toJsonString(response))
            .setResponseCode(200)
    }


    fun downloadFile(config: MockConfig, path: String): MockResponse{
        Log.d(_tag, "downloadFile - path: $path")
        val file = if (path.substring( path.lastIndexOf('.') + 1, path.length) == "json"){
            val jsonName = if(config.failedValidation) "E-$jsonFile" else jsonFile
            config.jsonPath?.let { File(it).inputStream() } ?: assets.open(jsonName)
        }else{
            config.mapPath?.let { File(it).inputStream() } ?: assets.open(mapFile)
        }


        val period = if (config.fastDownload) 5L else 50L

        return MockResponse()
            .setBody(Buffer().apply {
                writeAll(file.source())
            })
            .throttleBody(1024, period, TimeUnit.MILLISECONDS)

    }

    private fun readFileFromAssets(fileName: String): String{
        return assets.open(fileName).bufferedReader().use{
            it.readText()
        }
    }

    private fun readFile(filePath: String): String{
        return File(filePath).bufferedReader().use {
            it.readText()
        }
    }

    private inline fun <reified T>toJsonString(content: T): String{
        return Serializer.moshi.adapter(T::class.java).toJson(content)
    }
}