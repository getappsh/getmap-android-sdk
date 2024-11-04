package com.ngsoft.getapp.sdk

import GetApp.Client.models.NewBugReportDto
import android.content.Context
import com.ngsoft.getapp.sdk.exceptions.MissingIMEIException
import com.ngsoft.getapp.sdk.exceptions.VpnClosedException
import com.ngsoft.getapp.sdk.helpers.logger.TimberLogger
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber
import java.io.IOException
import java.io.Serializable
import java.util.concurrent.TimeUnit
import java.util.logging.FileHandler
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

typealias TestReportUpdater = (HashMap<Int, SystemTest.TestResults?>) -> Unit

class SystemTest private constructor(appCtx: Context,  configuration: Configuration) {
    companion object{
        const val TEST_DISCOVERY = 0
        const val TEST_CONFIG = 1
        const val TEST_IMPORT = 2
        const val TEST_DOWNLOAD = 3
        const val TEST_FILE_MOVE = 4
        const val TEST_INVENTORY_UPDATES = 5

        private val lock = Any()


        @Volatile
        private var instance: SystemTest? = null

        fun getInstance(appContext: Context, configuration: Configuration): SystemTest {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = SystemTest(appContext, configuration)
                    }
                }
            }
            return instance!!
        }

    }

    data class TestResults(
        val name: String,
        val testId: Int,
        var success: Boolean? = null,
        var message: String? = null
    ): Serializable

    private var service: AsioSdkGetMapService = AsioSdkGetMapService(appCtx)
    private var mapRepo: MapRepo
    private var client: GetAppClient
    private var pref: Pref
    private var testReport = hashMapOf<Int, TestResults?>()
    private var tree: FileLoggerTree? = null


    init {
        service.init(configuration)
        mapRepo = MapRepo(appCtx)
        pref = Pref.getInstance(appCtx)
        client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
    }

    private fun setUp(){
        tree = TimberLogger.getBugReportTree()
        tree?.apply { Timber.plant(this) }
        Timber.i("Setup - Test")
        tree?.files?.forEach{
            Timber.d("File: ${it.absolutePath}")
        }
    }

    private fun tearDown(){
        Timber.i("Teardown - Test")
        tree?.apply { Timber.uproot(this) }

        closeFileHandler()

        if (testReport.any{it.value?.success != true}){
            val sb = StringBuilder()
            testReport.forEach {
                val s = if (it.value?.success == true) "successfully" else "failed"
                sb.append("|${it.value?.name}-> $s ")
                }
            try {
                sendLogs(sb.toString())
            }catch (e: Exception){
                Timber.e(e)
            }
        }

        tree?.clear()

    }

    private fun sendLogs(description: String? = "SystemTest"){
        Timber.d("Send Logs")
        val res = client.bugReportApi.bugReportControllerReportNewBug(NewBugReportDto(
            deviceId = pref.deviceId,
            agentVersion = BuildConfig.VERSION_NAME,
            description = description
        ))
        Timber.d("Report Id: ${res.bugId}")
        val filePath = tree?.getFileName(0) ?: return
        client.uploadFile(res.uploadLogsUrl, filePath)



    }

    fun run(reportUpdater: TestReportUpdater){
        synchronized(lock){
            setUp()
            initTestReport(reportUpdater)
            testDiscovery(reportUpdater)
            testConfig(reportUpdater)
            testDelivery(reportUpdater)
            testInventoryUpdates(reportUpdater)
            tearDown()
        }

    }

    private fun initTestReport(reportUpdater: TestReportUpdater){
        testReport.clear()
        testReport[TEST_DISCOVERY] = null
        testReport[TEST_CONFIG] = null
        testReport[TEST_IMPORT] = null
        testReport[TEST_DOWNLOAD] = null
        testReport[TEST_FILE_MOVE] = null
        testReport[TEST_INVENTORY_UPDATES] = null
        reportUpdater(testReport)
    }

    fun testDiscovery(reportUpdater: TestReportUpdater){
        Timber.i("Test Discovery")
        testReport[TEST_DISCOVERY] = TestResults("בחירת תיחום", TEST_DISCOVERY)
        reportUpdater(testReport)
        try {
            service.getDiscoveryCatalog(MapProperties("system-test", "1.1.1.1", false))
            testReport[TEST_DISCOVERY]?.success = true

        }catch (io: IOException) {
            Timber.e(io)
            testReport[TEST_DISCOVERY]?.success = false
            testReport[TEST_DISCOVERY]?.message = io.message.toString()
            if (io.message.toString().lowercase().startsWith("unable to resolve host")) {
                tearDown()
                throw VpnClosedException()

            }

        }catch (e: Exception){
            Timber.e(e)
            testReport[TEST_DISCOVERY]?.success = false
            testReport[TEST_DISCOVERY]?.message = e.message.toString()
        }
        reportUpdater(testReport)
    }

    fun testConfig(reportUpdater: TestReportUpdater){
        Timber.i("Test Config")
        testReport[TEST_CONFIG] = TestResults("קבלת קונפיגורציה", TEST_CONFIG)
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
        Timber.i("Test Inventory Updates")
        testReport[TEST_INVENTORY_UPDATES] = TestResults("סטטוס עדכניות מפות", TEST_INVENTORY_UPDATES)
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
        Timber.i("Test Delivery")
        testReport[TEST_IMPORT] = TestResults("הפקת מפה", TEST_IMPORT)
        reportUpdater(testReport)

        val props = MapProperties(
            "system-test",
            "34.46087927,31.48921097,34.47834067,31.50156335",
            false
        )
        mapRepo.getByBBox(props.boundingBox).forEach {
            service.deleteMap(it.id.toString())
        }
        var id: String? = null;
        try {
            id = service.downloadMap(props);
        }catch (e: MissingIMEIException){
            Timber.e(e)
            testReport[TEST_IMPORT]?.message = e.message.toString()
        }
        if (id == null){
            testReport[TEST_IMPORT]?.success = false
            testReport[TEST_DOWNLOAD] = TestResults("הורדת מפות", TEST_DOWNLOAD, false)
            testReport[TEST_FILE_MOVE] = TestResults("העברת קבצים", TEST_FILE_MOVE, false)
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
                    testReport[TEST_DOWNLOAD] = TestResults("הורדת מפות", TEST_DOWNLOAD)
                }
                reportUpdater(testReport)
            }
            if (flowState >= DeliveryFlowState.DOWNLOAD_DONE){
                testReport[TEST_DOWNLOAD]?.success = true
                if (testReport[TEST_FILE_MOVE] == null){
                    testReport[TEST_FILE_MOVE] = TestResults("העברת קבצים", TEST_FILE_MOVE)
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
                    testReport[TEST_DOWNLOAD] = TestResults("הורדת מפות", TEST_DOWNLOAD, false)
                    testReport[TEST_DOWNLOAD]?.message = if (timeoutTime.hasPassedNow()) "Time-out" else mapPkg.statusDescr
                }
                if (testReport[TEST_FILE_MOVE]?.success != true){
                    testReport[TEST_FILE_MOVE] = TestResults("העברת קבצים", TEST_FILE_MOVE, false)
                    testReport[TEST_FILE_MOVE]?.message = if (timeoutTime.hasPassedNow()) "Time-out" else mapPkg.statusDescr
                }
                reportUpdater(testReport)
                break
            }

        }
        service.deleteMap(id)
    }


    private fun closeFileHandler(){
        val fileHandlerField = FileLoggerTree::class.java.getDeclaredField("fileHandler")
        fileHandlerField.isAccessible = true
        val fileHandler = fileHandlerField.get(tree) as FileHandler?
        fileHandler?.close()
    }

}