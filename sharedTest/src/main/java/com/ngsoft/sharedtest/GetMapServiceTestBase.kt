package com.ngsoft.sharedtest

import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory

open class GetMapServiceTestBase {

    companion object {

        @JvmStatic
        protected var service: GetMapService
        init {
            val cfg = Configuration(
                Settings.baseUrl,
                Settings.user,
                Settings.password,
                "todo:"
            )

            println("GetMapServiceTestBase Init, using ${cfg}\nCreating GetMapService...")
            service = GetMapServiceFactory.createService(cfg)
            println("GetMapService created...")
        }
    }

}