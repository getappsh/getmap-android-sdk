package com.ngsoft.sharedtest

import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceImpl

open class GetMapServiceTestBase {
    companion object {

        @JvmStatic
        protected var api: GetMapService
        init {
            val cfg = Configuration(
                Settings.baseUrl,
                Settings.user,
                Settings.password,
                "todo:"
            )

            println("GetMapServiceTestBase Init, using ${cfg}\nCreating GetMapService...")
            api = GetMapServiceImpl(cfg)
            println("GetMapService created...")
        }
    }

}