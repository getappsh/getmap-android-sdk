package com.ngsoft.getapp.sdk

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapProperties
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class App4ASIOIntegrationTests {
    companion object {

        @JvmStatic
        private lateinit var service: GetMapService

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val cfg = Configuration(
                "http://getapp-dev.getapp.sh:3000",
                "rony@example.com",
                "rony123",
                //currently downloads file to a path within the public external storage directory
                Environment.DIRECTORY_DOWNLOADS,
                16
            )

            service = GetMapServiceFactory.createService(appContext, cfg)
        }

    }

    @Test
    fun a_ExtentUpdates_IsOk() {
        val props = MapProperties(
            "getmap:Ashdod2",
            "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
//            "dcf8f87e-f02d-4b7a-bf7b-c8b64b2d202a",
//            "35.24013558,32.17154827,35.24551706,32.17523034",
            false
        )

        val updates = service.getExtentUpdates(props, LocalDateTime.of(2023, 11, 23, 1, 2, 3 ))

        assert(updates.isNotEmpty())

        val updatesCount = updates.count()
        println("got count = $updatesCount")

        assert(updatesCount == 6)

        updates.forEach{
            println(it)
        }
    }

}