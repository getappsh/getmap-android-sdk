package com.ngsoft.getapp.sdk

import android.content.Context

class GetMapServiceFactory {
    companion object {

        /**
         * Creates service SDK-style
         *
         * @param appCtx application context
         * @param configuration service configuration
         * @return created service
         */
        @JvmStatic
        fun createService(appCtx: Context, configuration: Configuration): GetMapService {
            val service = DefaultGetMapService(appCtx)
            service.init(configuration)
            return service
        }

        /**
         * Creates service ASIOApp-style
         *
         * @param appCtx application context
         * @param configuration service configuration
         * @return created service
         */
        @JvmStatic
        fun createAsioAppSvc(appCtx: Context, configuration: Configuration): GetMapService {
            val service = AsioAppGetMapService(appCtx)
            service.init(configuration)
            return service
        }

    }
}