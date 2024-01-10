package com.ngsoft.getapp.sdk

import android.content.Context
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.Method


@RunWith(AndroidJUnit4::class)
class RefreshMapStateUnitTest {

    private fun runRefresh(mapPkg: MapPkg): MapPkg{
        val res = refresh.invoke(service, mapPkg) as MapPkg
        println(res.toString())
        println(res.flowState)

        return res
    }

    private fun generateMapPgk(): MapPkg{
        return MapPkg (
            fileName = "map.gpkg",
            jsonName = "map.json",
            url = "http://example.com",
            reqId = "mock_req_id",
            state = MapDeliveryState.DONE,
            flowState = DeliveryFlowState.DONE,
            bBox = "1.0, 1.0, 1.0, 1.0",
            pId = "pId",
            statusMessage = "")

    }

    @Test
    fun testWhenOnlyTargetFilesExist() {
//        In this case deliveryFlowState needs to be DONE or MOVE_FILES
        createMapInTarget()
        createJsonInTarget()

        val mapPkg = generateMapPgk()

        mapPkg.state = MapDeliveryState.DONE

        var res = runRefresh(mapPkg)
        assert(res.flowState == DeliveryFlowState.DONE)
        assert(res.state == MapDeliveryState.DONE)
        assert(res.statusMessage == appContext.getString(R.string.delivery_status_done))


        mapPkg.state = MapDeliveryState.CANCEL
        res = runRefresh(mapPkg)

        assert(res.flowState == DeliveryFlowState.MOVE_FILES)
        assert(res.state == MapDeliveryState.CANCEL)
        assert(res.statusMessage == mapPkg.statusMessage)

    }

    @Test
    fun testWhenOnlyOriginalFilesExist() {
//        In this case deliveryFlowState needs to be DOWNLOAD_DONE
        createMapInOriginal()
        createJsonInOriginal()

        val mapPkg = generateMapPgk()
        mapPkg.state = MapDeliveryState.DONE
        var res = runRefresh(mapPkg)

        assert(res.flowState == DeliveryFlowState.DOWNLOAD_DONE)
        assert(res.metadata.mapDone)
        assert(res.metadata.jsonDone)

        assert(res.state == MapDeliveryState.ERROR)
        assert(res.statusMessage == appContext.getString(R.string.delivery_status_failed))


        mapPkg.state = MapDeliveryState.CANCEL
        res = runRefresh(mapPkg)
        assert(res.state == MapDeliveryState.CANCEL)
        assert(res.statusMessage == mapPkg.statusMessage)

    }

    @Test
    fun testWhenOneOfOriginalFileExist(){
        createMapInOriginal()

        val mapPkg = generateMapPgk()
        mapPkg.state = MapDeliveryState.DONE
        mapPkg.url = null
        mapPkg.reqId = null
        var res = runRefresh(mapPkg)

        assert(res.flowState == DeliveryFlowState.START)
        assert(res.metadata.mapDone)
        assert(!res.metadata.jsonDone)

        assert(res.state == MapDeliveryState.ERROR)
        assert(res.statusMessage == appContext.getString(R.string.delivery_status_failed))

        mapPkg.reqId = "dummy reqId"
        res = runRefresh(mapPkg)
        assert(res.flowState == DeliveryFlowState.IMPORT_CREATE)

        mapPkg.url = "dummy url"
        res = runRefresh(mapPkg)
        assert(res.flowState == DeliveryFlowState.IMPORT_DELIVERY)

    }

    @Test
    fun testWhenJsonInTarget(){
        createJsonInTarget()
        val mapPkg = generateMapPgk()
        mapPkg.state = MapDeliveryState.DONE
        mapPkg.flowState = DeliveryFlowState.DONE
        mapPkg.url = null
        mapPkg.reqId = null

        val res = runRefresh(mapPkg)

        assert(res.state == MapDeliveryState.ERROR)
        assert(res.flowState == DeliveryFlowState.START)
        assert(res.statusMessage == appContext.getString(R.string.delivery_status_failed))
    }


    companion object {
        @JvmStatic
        private lateinit var service: AsioSdkGetMapService
        private lateinit var appContext: Context
        private lateinit var refresh: Method
        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
            println("Path: $path")
            val cfg = Configuration(
                "http://localhost:3333",
                "rony@example.com",
                "rony123",
                path,
                16,
                5,5,
                null
            )

            service = AsioSdkGetMapService(appContext)
            service.init(cfg)

            refresh = service::class.java.getDeclaredMethod("refreshMapState", MapPkg::class.java)
            refresh.isAccessible = true

            service.purgeCache()
        }

    }

    private fun createMapInTarget(){
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
        File(dir, "map.gpkg").createNewFile()
    }

    private fun createMapInOriginal(){
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        File(dir, "map.gpkg").createNewFile()

    }

    private fun createJsonInTarget(){
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
        File(dir, "map.json").createNewFile()


    }

    private fun createJsonInOriginal(){
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        File(dir, "map.json").createNewFile()

    }


    @After
    fun deleteAllFiles(){
        val oDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        File(oDir, "map.gpkg").delete()
        File(oDir, "map.json").delete()
        val tDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
        File(tDir, "map.gpkg").delete()
        File(tDir, "map.json").delete()

    }


}