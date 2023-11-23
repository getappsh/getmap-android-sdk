package com.ngsoft.technician.mockserver

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest


internal class MockServer(assets: AssetManager) {

    private val _tag = "MockServer"

    private var responses: MockServerResponses

    private lateinit var mockServer: MockWebServer
    private lateinit var url: String

    var config: MockConfig = MockConfig()

    init {
        responses = MockServerResponses(assets)
        startServer()
    }
    private fun startServer(){
        Log.d(_tag, "Start Mock Server")

        if (this::mockServer.isInitialized){
            mockServer.shutdown()
        }
        mockServer = MockWebServer()
        mockServer.dispatcher = getDispatcher()
        mockServer.start(port = 1111)
        url = mockServer.url("/").toString()

        Log.d(_tag, "startServer - url: $url")
    }
    fun stopServer(){
        Log.i(_tag, "Stop Mock Server")
    }
    private fun getDispatcher(): Dispatcher{
        return object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                Log.d(_tag, "dispatch: request path ${request.path}")
                val path = request.path ?: ""
                return when {
                    path == "/api/login" -> responses.getLogin(config)
                    path == "/api/device/discover" -> responses.getDiscovery(config)
                    path == "/api/map/import/create" ->  responses.importCreate(config)
                    path.startsWith("/api/map/import/status/") -> responses.importStatus(config, path)
                    path == "/api/delivery/prepareDelivery" -> responses.prepareDelivery(config, request.body)
                    path.startsWith("/api/delivery/preparedDelivery/") -> responses.preparedDelivery(config, path)
                    path.startsWith("/api/download/") -> responses.downloadFile(config, path)

                    else -> MockResponse().setResponseCode(200)


                }
            }
        }
    }


    companion object {
        private var instance: MockServer? = null

        @Synchronized
        fun getInstance(context: Context): MockServer {
            if (instance == null) {
                instance = MockServer(context.assets)
            }
            return instance!!
        }
    }
}