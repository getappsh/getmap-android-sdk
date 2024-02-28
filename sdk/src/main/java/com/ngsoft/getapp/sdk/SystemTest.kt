package com.ngsoft.getapp.sdk

import android.content.Context
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

class SystemTest(private val appCtx: Context,  configuration: Configuration) {
    companion object{
        private const val TEST_CONFIG = 0
        private const val TEST_IMPORT = 1
        private const val TEST_DOWNLOAD = 2
    }

    data class TestResults(
        val name: String,
        val testId: Int,
        var success: Boolean? = null,
        var message: String? = null
    )

    private var service: AsioSdkGetMapService
    private var mapRepo: MapRepo
    private var mapsList: LiveData<List<MapData>>? = null
    var testReport = hashMapOf<Int, TestResults?>()

    init {
        service = AsioSdkGetMapService(appCtx)
        service.init(configuration)
        mapRepo = MapRepo(appCtx)
    }

    private fun initTestReport(){
        testReport.clear()
        testReport[TEST_CONFIG] = null
        testReport[TEST_IMPORT] = null
    }
    fun run(){
        initTestReport()
        testConfig()
        testDelivery()

    }

    fun testConfig(){
        testReport[TEST_CONFIG] = TestResults("Config", TEST_CONFIG)
        val lastUpdate = service.config.lastConfigCheck
        try {
            service.fetchConfigUpdates()
            if (service.config.lastConfigCheck != lastUpdate){
                testReport[TEST_CONFIG]?.success = true
            }else{
                testReport[TEST_CONFIG]?.success = false
            }
        }catch (e: Exception){
            testReport[TEST_CONFIG]?.success = false
            testReport[TEST_CONFIG]?.message = e.message.toString()

        }
    }

    @OptIn(ExperimentalTime::class)
    fun testDelivery(){
        testReport[TEST_IMPORT] = TestResults("Import Map", TEST_IMPORT)

        val props = MapProperties(
            "system-test",
            "34.46087927,31.48921097,34.47834067,31.50156334",
            false
        )
        mapRepo.getByBBox(props.boundingBox).forEach {
            service.deleteMap(it.id.toString())
        }

        val id = service.downloadMap(props);
        if (id == null){
            testReport[TEST_IMPORT]?.success = false
            testReport[TEST_DOWNLOAD] = TestResults("Download Map", TEST_DOWNLOAD, false)
            return
        }



        val timeoutTime = TimeSource.Monotonic.markNow() + 15.minutes
        while(true){
            TimeUnit.SECONDS.sleep(1)
            val mapPkg = mapRepo.getById(id)

            val flowState = mapPkg?.flowState ?: continue
            val state = mapPkg.state

            if(flowState >= DeliveryFlowState.IMPORT_STATUS){
                testReport[TEST_IMPORT]?.success = true

                if (testReport[TEST_DOWNLOAD] == null){
                    testReport[TEST_DOWNLOAD] = TestResults("Download Map", TEST_DOWNLOAD)
                }
            }
            if (flowState >= DeliveryFlowState.DOWNLOAD_DONE){
                testReport[TEST_DOWNLOAD]?.success = true
            }
//            if (flowState >= DeliveryFlowState.MOVE_FILES){}

            if(state == MapDeliveryState.DONE){
                break
            }
            if(state == MapDeliveryState.ERROR || state == MapDeliveryState.CANCEL || state == MapDeliveryState.PAUSE || timeoutTime.hasPassedNow()){
                if (testReport[TEST_IMPORT]?.success != true){
                    testReport[TEST_IMPORT]?.success = false
                    testReport[TEST_IMPORT]?.message = if (timeoutTime.hasPassedNow()) "Time Out" else mapPkg.statusDescr

                }
                if (testReport[TEST_DOWNLOAD]?.success != true){
                    testReport[TEST_DOWNLOAD] = TestResults("Download Map", TEST_DOWNLOAD, false)
                    testReport[TEST_DOWNLOAD]?.message = if (timeoutTime.hasPassedNow()) "Time Out" else mapPkg.statusDescr
                }
                break
            }

        }
        service.deleteMap(id)
    }

}