package com.ngsoft.getapp.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import junit.framework.TestCase.fail
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SdkIntegrationTests {
    companion object {
        private var requestId: String =
            "invalid request id"
            //"1151176418437103616"

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
                "todo: storage path"
            )

            service = GetMapServiceFactory.createService(appContext, cfg)
        }

    }

    @Test
    fun a_DiscoveryCatalog_IsNotEmpty() {

        val props = MapProperties(
            "dummy product",
            "1,2,3,4",
            false
        )

        val ret = service.getDiscoveryCatalog(props)
        assert(ret.isNotEmpty())

        println("Got discovery items:")
        ret.forEach {
            println(it)
        }
    }


    @Test
    fun b_CreateMapImport_IsOk() {

        val props = MapProperties(
            "dcf8f87e-f02d-4b7a-bf7b-c8b64b2d202a",
            "35.24013558,32.17154827,35.24551706,32.17523034",
            false
        )

        val ret = service.createMapImport(props)
        assert(ret != null)
        assert(!ret?.importRequestId.isNullOrEmpty())

        //request id is used in subsequent calls
        requestId = ret?.importRequestId.toString()
        println("RequestID = $requestId")

        println(ret?.statusCode?.statusCode?.toString())
        println(ret?.state?.toString())
    }

    @Test
    fun c_CreateMapImportStatus_IsOk(){
        println("checking import state...")

        var interations = 0
        var stat = getImportStatus()
        while (stat != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getImportStatus()
            if(stat == MapImportState.ERROR)
                fail("error")
            if(interations++ > 180){
                fail("timed out")
            }
        }
    }

    private fun getImportStatus() : MapImportState {

        val ret = service.getCreateMapImportStatus(requestId)
        assert(ret != null)
        assert(requestId == ret?.importRequestId)

        println(ret?.statusCode?.statusCode?.toString())
        println(ret?.state?.toString())

        return ret?.state!!
    }

    @Test
    fun d_MapImportDeliveryStart_IsOk(){

        val ret = service.setMapImportDeliveryStart(requestId)
        assert(ret != null)
        assert(requestId == ret?.importRequestId)

        println(ret?.state?.toString())
    }

    @Test
    fun e_MapImportDeliveryStatus_IsOk(){

        //In current GetApp 4 Zayad implementation it returns almost instantly.
        //In another implementation is should take a #N number of requests to figure out is delivery ready
        val ret = service.getMapImportDeliveryStatus(requestId)

        assert(ret != null)
        assert(requestId == ret?.importRequestId)

        println(ret?.state?.toString())
    }

    @Test
    fun f_MapImportDeploy_IsOk(){

        val ret = service.setMapImportDeploy(requestId, null)

        assert(ret != null)
        assert(ret == MapDeployState.DONE)

    }

}