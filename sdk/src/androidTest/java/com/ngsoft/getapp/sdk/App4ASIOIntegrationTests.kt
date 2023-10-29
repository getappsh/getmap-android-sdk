package com.ngsoft.getapp.sdk

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
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

        private lateinit var tilesUpdates: List<MapTile>
        private lateinit var updateDate: LocalDateTime

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
                16,
                5,5
            )

            service = GetMapServiceFactory.createAsioAppSvc(appContext, cfg)

            //purge cache 4 testing
            service.purgeCache()
        }

    }

    @Test
    fun a_GetUpdateDate(){
        val props = MapProperties("dummy product","1,2,3,4",false)
        val products = service.getDiscoveryCatalog(props)
        assert(products.isNotEmpty())

        val productOfInterest = products.find { it.productId ==  "getmap:Ashdod2"}
        assert(productOfInterest != null)

        updateDate = productOfInterest?.updateDate?.toLocalDateTime()!!
        assert(updateDate != LocalDateTime.of(1,1,1,0,0))

        println("update date = $updateDate")
    }

    @Test
    fun b_GetExtentUpdates() {

        val props = MapProperties(
            "getmap:Ashdod2",
            "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
//            "dcf8f87e-f02d-4b7a-bf7b-c8b64b2d202a",
//            "35.24013558,32.17154827,35.24551706,32.17523034",
            false
        )

        tilesUpdates = service.getExtentUpdates(props, updateDate)

        assert(tilesUpdates.isNotEmpty())

        val updatesCount = tilesUpdates.count()
        println("got count = $updatesCount")

        assert(updatesCount == 6)

        tilesUpdates.forEach{
            println(it)
        }
    }

    @Test
    fun c_DeliverTiles() {
        var downloadedCount = 0
        val downloadProgressHandler: (DownloadProgress) -> Unit = {
            println("processing download progress=$it event...")
            downloadedCount = it.packagesProgress.count { pkg ->  pkg.isCompleted }
        }

        val delivered = service.deliverExtentTiles(tilesUpdates, downloadProgressHandler)
        assert(downloadedCount == 6)
        assert(delivered.isNotEmpty())
        assert(delivered.count() == 6)

        delivered.forEach{
            println(it)
        }

    }

    @Test
    fun d_CheckTilesInCache(){
        val props = MapProperties(
            "getmap:Ashdod2",
            "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
//            "dcf8f87e-f02d-4b7a-bf7b-c8b64b2d202a",
//            "35.24013558,32.17154827,35.24551706,32.17523034",
            false
        )

        tilesUpdates = service.getExtentUpdates(props, updateDate)
        assert(tilesUpdates.isEmpty())
    }

}