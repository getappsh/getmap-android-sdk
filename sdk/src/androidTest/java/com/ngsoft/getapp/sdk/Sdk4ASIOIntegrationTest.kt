package com.ngsoft.getapp.sdk

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapProperties
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class Sdk4ASIOIntegrationTest {


    companion object {

        private lateinit var mapId: String

        @JvmStatic
        private lateinit var service: GetMapService

//        @JvmField
//        @get:Rule
//        var runtimeRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)



        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            println("Path: $path")
            val cfg = Configuration(
                "http://getapp-dev.getapp.sh:3000",
                "rony@example.com",
                "rony123",
                path,
                16,
                null
            )

            service = GetMapServiceFactory.createAsioSdkSvc(appContext, cfg)

            service.purgeCache()
        }

    }
    @Test
    fun discoveryTest(){
        val props = MapProperties("dummy product", "1,2,3,4",false)
        val products = service.getDiscoveryCatalog(props)

        assert(products.isNotEmpty())
    }


    @Test
    fun deliveryTest(){
        val props = MapProperties(
            "dummy product",
            "34.47146482,31.55712952,34.48496631,31.56652666",
            false
        )

        val latch = CountDownLatch(1)
        val downloadProgressHandler: (MapDownloadData) -> Unit = {
            println("Map delivery state: ${it.deliveryStatus}")
            assert(it.deliveryStatus != MapDeliveryState.ERROR) {"it.deliveryStatus == MapDeliveryState.ERROR"}
            if (it.deliveryStatus == MapDeliveryState.DONE){
                assert(it.fileName != null)
                assert(it.fileName!!.isNotEmpty())

                assert(it.jsonName != null)
                assert(it.jsonName!!.isNotEmpty())

                latch.countDown()
            }


        }
        val id = service.downloadMap(props, downloadProgressHandler)

        assert(id != null)
        mapId = id!!;

        latch.await()
    }

    @Test
    fun getDownloadedMapsTest(){
        val map = service.getDownloadedMap(mapId)
        assert(map != null)
        assert(map!!.id == mapId)

        val maps = service.getDownloadedMaps()
        val fMap = maps.find { it.id == mapId }

        assert(fMap != null)
        assert(fMap!!.id == mapId)

        assert(fMap.fileName != null)
        assert(fMap.fileName!!.isNotEmpty())

        assert(fMap.jsonName != null)
        assert(fMap.jsonName!!.isNotEmpty())
    }

}