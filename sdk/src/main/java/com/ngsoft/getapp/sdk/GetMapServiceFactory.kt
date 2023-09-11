package com.ngsoft.getapp.sdk

import android.content.Context

class GetMapServiceFactory {
    companion object {

        /**
         * Creates service
         *
         * @param configuration service configuration
         * @return created service
         */
        @JvmStatic
        fun createService(configuration: Configuration): GetMapService {
            val service = DefaultGetMapService()
            service.init(configuration, null)
            return service
        }
    }
}