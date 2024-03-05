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

typealias TestReportUpdater = (HashMap<Int, SystemTest.TestResults?>) -> Unit

class SystemTest(private val appCtx: Context,  configuration: Configuration) {
    companion object{
        const val TEST_CONFIG = 0
        const val TEST_IMPORT = 1
        const val TEST_DOWNLOAD = 2
        const val TEST_FILE_MOVE = 3
        const val TEST_INVENTORY_UPDATES = 4
    }

    data class TestResults(
        val name: String,
        val testId: Int,
        var success: Boolean? = null,
        var message: String? = null
    )

    private var service: AsioSdkGetMapService
    private var mapRepo: MapRepo
    private var testReport = hashMapOf<Int, TestResults?>()

    init {
        service = AsioSdkGetMapService(appCtx)
        service.init(configuration)
        mapRepo = MapRepo(appCtx)
    }
    fun run(reportUpdater: TestReportUpdater){
        initTestReport(reportUpdater)
        testConfig(reportUpdater)
        testDelivery(reportUpdater)
        testInventoryUpdates(reportUpdater)
    }

    private fun initTestReport(reportUpdater: TestReportUpdater){
        testReport.clear()
        testReport[TEST_CONFIG] = null
        testReport[TEST_IMPORT] = null
        testReport[TEST_DOWNLOAD] = null
        testReport[TEST_FILE_MOVE] = null
        testReport[TEST_INVENTORY_UPDATES] = null
        reportUpdater(testReport)
    }

    fun testConfig(reportUpdater: TestReportUpdater){
        testReport[TEST_CONFIG] = TestResults("Config", TEST_CONFIG)
        reportUpdater(testReport)

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
        reportUpdater(testReport)
    }

    fun testInventoryUpdates(reportUpdater: TestReportUpdater){
        testReport[TEST_INVENTORY_UPDATES] = TestResults("Inventory Updates", TEST_INVENTORY_UPDATES)
        reportUpdater(testReport)

        try {
            service.fetchInventoryUpdates()
            testReport[TEST_INVENTORY_UPDATES]?.success = true
        }catch (e: Exception){
            testReport[TEST_INVENTORY_UPDATES]?.success = false
            testReport[TEST_INVENTORY_UPDATES]?.message = e.message.toString()
        }
        reportUpdater(testReport)
    }

    @OptIn(ExperimentalTime::class)
    fun testDelivery(reportUpdater: TestReportUpdater){
        testReport[TEST_IMPORT] = TestResults("Import Map", TEST_IMPORT)
        reportUpdater(testReport)

        val props = MapProperties(
            "system-test",
            "34.46087927,31.48921097,34.47834067,31.50156335",
            false
        )
        mapRepo.getByBBox(props.boundingBox).forEach {
            service.deleteMap(it.id.toString())
        }

        val id = service.downloadMap(props);
        if (id == null){
            testReport[TEST_IMPORT]?.success = false
            testReport[TEST_DOWNLOAD] = TestResults("Download Map", TEST_DOWNLOAD, false)
            testReport[TEST_FILE_MOVE] = TestResults("Move Files", TEST_FILE_MOVE, false)
            reportUpdater(testReport)
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
                reportUpdater(testReport)
            }
            if (flowState >= DeliveryFlowState.DOWNLOAD_DONE){
                testReport[TEST_DOWNLOAD]?.success = true
                if (testReport[TEST_FILE_MOVE] == null){
                    testReport[TEST_FILE_MOVE] = TestResults("Move Files", TEST_FILE_MOVE)
                }
                reportUpdater(testReport)
            }
            if (flowState >= DeliveryFlowState.MOVE_FILES){
                testReport[TEST_FILE_MOVE]?.success = true
                reportUpdater(testReport)
            }

            if(state == MapDeliveryState.DONE){
                reportUpdater(testReport)
                break
            }
            if(state == MapDeliveryState.ERROR || state == MapDeliveryState.CANCEL || state == MapDeliveryState.PAUSE || timeoutTime.hasPassedNow()){
                if (testReport[TEST_IMPORT]?.success != true){
                    testReport[TEST_IMPORT]?.success = false
                    testReport[TEST_IMPORT]?.message = if (timeoutTime.hasPassedNow()) "Time-out" else mapPkg.statusDescr

                }
                if (testReport[TEST_DOWNLOAD]?.success != true){
                    testReport[TEST_DOWNLOAD] = TestResults("Download Map", TEST_DOWNLOAD, false)
                    testReport[TEST_DOWNLOAD]?.message = if (timeoutTime.hasPassedNow()) "Time-out" else mapPkg.statusDescr
                }
                if (testReport[TEST_FILE_MOVE]?.success != true){
                    testReport[TEST_FILE_MOVE] = TestResults("Move Files", TEST_FILE_MOVE, false)
                    testReport[TEST_FILE_MOVE]?.message = if (timeoutTime.hasPassedNow()) "Time-out" else mapPkg.statusDescr
                }
                reportUpdater(testReport)
                break
            }

        }
        service.deleteMap(id)
    }

}