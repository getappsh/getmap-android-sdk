package com.ngsoft.getapp.sdk

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.getapp.sdk.old.DownloadProgress
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
        private val extentUpdMapProps = MapProperties(
            "getmap:Ashdod2",
//            "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
            "34.43952527,31.52167451,34.44305441,31.52412417",
//            "dcf8f87e-f02d-4b7a-bf7b-c8b64b2d202a",
//            "35.24013558,32.17154827,35.24551706,32.17523034",
            false
        )

        @JvmStatic
        private lateinit var service: GetMapService

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val cfg = Configuration(
                "http://getapp-dev.getapp.sh:3000",
                BuildConfig.USERNAME,
                BuildConfig.PASSWORD,
                //currently downloads file to a path within the public external storage directory
                16,
                null
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

        println(products)
        val productOfInterest = products.find { it.productId ==  "045eaa61-8f61-48d3-a240-4b02a683eca3"}
        assert(productOfInterest != null)

        updateDate = productOfInterest?.imagingTimeBeginUTC?.toLocalDateTime()!!
        assert(updateDate != LocalDateTime.of(1,1,1,0,0))

        println("update date = $updateDate")
    }

    @Test
    fun b_GetExtentUpdates() {
        getExtentUpdates()
    }

    private fun getExtentUpdates() {
        tilesUpdates = service.getExtentUpdates(extentUpdMapProps, updateDate)
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
        deliverTiles(tilesUpdates, 6)
    }

    private fun deliverTiles(tiles: List<MapTile>, validationCount: Int) {
        var downloadedCount = 0
        val downloadProgressHandler: (DownloadProgress) -> Unit = {
            println("processing download progress=$it event...")
            downloadedCount = it.packagesProgress.count { pkg ->  pkg.isCompleted }
        }

        val delivered = service.deliverExtentTiles(tiles, downloadProgressHandler)
        assert(downloadedCount == validationCount)
        assert(delivered.isNotEmpty())
        assert(delivered.count() == validationCount)

        delivered.forEach{
            println(it)
        }
    }

    @Test
    fun d_CheckTilesInCache(){
        checkTilesInCache()
    }

    private fun checkTilesInCache(){
        tilesUpdates = service.getExtentUpdates(extentUpdMapProps, updateDate)
        assert(tilesUpdates.isEmpty())
    }

    @Test
    fun e_GetExtentUpdates2() {
        //change update date
        updateDate = updateDate.plusDays(3)
        getExtentUpdates()
    }

    @Test
    fun f_DeliverTiles2() {

        println("delivering single tile")
        val oneTile = listOf(tilesUpdates[0])
        deliverTiles(oneTile, oneTile.count())

        println("delivering 2 tiles")
        val twoTiles = listOf(tilesUpdates[1], tilesUpdates[2])
        deliverTiles(twoTiles, twoTiles.count())

        println("delivering 3 tiles")
        val threeTiles = listOf(tilesUpdates[3], tilesUpdates[4], tilesUpdates[5])
        deliverTiles(threeTiles, threeTiles.count())

    }

    @Test
    fun g_CheckTilesInCache2(){
        checkTilesInCache()
    }

}