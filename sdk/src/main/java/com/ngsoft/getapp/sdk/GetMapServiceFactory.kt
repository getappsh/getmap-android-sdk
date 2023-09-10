package com.ngsoft.getapp.sdk

class GetMapServiceFactory {
    companion object {
        @JvmStatic
        fun createService(configuration: Configuration): GetMapService {
            val service = DefaultGetMapService()
            service.init(configuration, null)
            return service
        }
    }
}