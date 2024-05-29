package com.ngsoft.getapp.sdk

import GetApp.Client.models.MapConfigDto
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.lang.reflect.Method


@RunWith(AndroidJUnit4::class)
class MapFileManagerUnitTest {

//    private fun runRefresh(mapPkg: MapPkg): MapPkg{
//        val res = refresh.invoke(service, mapPkg) as MapPkg
//        println(res.toString())
//        println(res.flowState)
//
//        return res
//    }
//
//    private fun runMoveFiles(pkgName: String, jsonName: String): Pair<String, String>{
//        val res = moveFiles.invoke(service, pkgName, jsonName) as Pair<String, String>
//        println("first: ${res.first}, second: ${res.second}")
//        return res
//    }
//
//    private fun generateMapPgk(): MapPkg{
//        return MapPkg (
//            fileName = "map.gpkg",
//            jsonName = "map.json",
//            url = "http://example.com",
//            reqId = "mock_req_id",
//            state = MapDeliveryState.DONE,
//            flowState = DeliveryFlowState.DONE,
//            bBox = "1.0, 1.0, 1.0, 1.0",
//            pId = "pId",
//            statusMessage = "")
//
//    }
//
//    @Test
//    fun testRefreshWhenOnlyTargetFilesExist() {
////        In this case deliveryFlowState needs to be DONE or MOVE_FILES
//        createMapInTarget()
//        createJsonInTarget()
//
//        val mapPkg = generateMapPgk()
//
//        mapPkg.state = MapDeliveryState.DONE
//
//        var res = runRefresh(mapPkg)
//        assert(res.flowState == DeliveryFlowState.DONE)
//        assert(res.state == MapDeliveryState.DONE)
//        assert(res.statusMessage == appContext.getString(R.string.delivery_status_done))
//
//
//        mapPkg.state = MapDeliveryState.CANCEL
//        res = runRefresh(mapPkg)
//
//        assert(res.flowState == DeliveryFlowState.MOVE_FILES)
//        assert(res.state == MapDeliveryState.CANCEL)
//        assert(res.statusMessage == mapPkg.statusMessage)
//
//    }
//
//    @Test
//    fun testRefreshWhenOnlyOriginalFilesExist() {
////        In this case deliveryFlowState needs to be DOWNLOAD_DONE
//        createMapInOriginal()
//        createJsonInOriginal()
//
//        val mapPkg = generateMapPgk()
//        mapPkg.state = MapDeliveryState.DONE
//        var res = runRefresh(mapPkg)
//
//        assert(res.flowState == DeliveryFlowState.DOWNLOAD_DONE)
//        assert(res.metadata.mapDone)
//        assert(res.metadata.jsonDone)
//
//        assert(res.state == MapDeliveryState.ERROR)
//        assert(res.statusMessage == appContext.getString(R.string.delivery_status_failed))
//
//
//        mapPkg.state = MapDeliveryState.CANCEL
//        res = runRefresh(mapPkg)
//        assert(res.state == MapDeliveryState.CANCEL)
//        assert(res.statusMessage == mapPkg.statusMessage)
//
//    }
//
//    @Test
//    fun testRefreshWhenOneOfOriginalFileExist(){
//        createMapInOriginal()
//
//        val mapPkg = generateMapPgk()
//        mapPkg.state = MapDeliveryState.DONE
//        mapPkg.url = null
//        mapPkg.reqId = null
//        var res = runRefresh(mapPkg)
//
//        assert(res.flowState == DeliveryFlowState.START)
//        assert(res.metadata.mapDone)
//        assert(!res.metadata.jsonDone)
//
//        assert(res.state == MapDeliveryState.ERROR)
//        assert(res.statusMessage == appContext.getString(R.string.delivery_status_failed))
//
//        mapPkg.reqId = "dummy reqId"
//        res = runRefresh(mapPkg)
//        assert(res.flowState == DeliveryFlowState.IMPORT_CREATE)
//
//        mapPkg.url = "dummy url"
//        res = runRefresh(mapPkg)
//        assert(res.flowState == DeliveryFlowState.IMPORT_DELIVERY)
//
//    }
//
//    @Test
//    fun testRefreshWhenJsonInTarget(){
//        createJsonInTarget()
//        val mapPkg = generateMapPgk()
//        mapPkg.state = MapDeliveryState.DONE
//        mapPkg.flowState = DeliveryFlowState.DONE
//        mapPkg.url = null
//        mapPkg.reqId = null
//
//        val res = runRefresh(mapPkg)
//
//        assert(res.state == MapDeliveryState.ERROR)
//        assert(res.flowState == DeliveryFlowState.START)
//        assert(res.statusMessage == appContext.getString(R.string.delivery_status_failed))
//    }


    @Test
    fun testMoveFiles1(){
        // Test when the classic test
        createJsonInOriginal()
        createMapInOriginal()

        val res = fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)

        assert(!isFileExistsInOriginal(MAP_NAME))
        assert(!isFileExistsInOriginal(JSON_NAME))
        assert(isFileExistsInTarget(res.first.name))
        assert(isFileExistsInTarget(res.second.name))

        assert(res.first.name == MAP_NAME)
        assert(res.second.name == JSON_NAME)
    }

    @Test
    fun testMoveFiles2() {
        // Test when the package file exists in the target path but not in the storage path
        createJsonInOriginal()
        createMapInTarget()

        val res = fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)

        assert(!isFileExistsInOriginal(MAP_NAME))
        assert(!isFileExistsInOriginal(JSON_NAME))
        assert(isFileExistsInTarget(res.first.name))
        assert(isFileExistsInTarget(res.second.name))

        assert(res.first.name != MAP_NAME)
        assert(res.second.name != JSON_NAME)
    }

    @Test
    fun testMoveFiles3() {
        // Test when the package file exists in both the original and target paths
        createJsonInOriginal()
        createMapInOriginal()
        createMapInTarget()

        val res = fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)

        assert(!isFileExistsInOriginal(MAP_NAME))
        assert(!isFileExistsInOriginal(JSON_NAME))
        assert(isFileExistsInTarget(res.first.name))
        assert(isFileExistsInTarget(res.second.name))

        assert(res.first.name != MAP_NAME)
        assert(res.second.name != JSON_NAME)
    }

    @Test
    fun testMoveFiles4() {
//        Like 2 to json
        createJsonInTarget()
        createMapInOriginal()

        val res = fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)

        assert(!isFileExistsInOriginal(MAP_NAME))
        assert(!isFileExistsInOriginal(JSON_NAME))
        assert(isFileExistsInTarget(res.first.name))
        assert(isFileExistsInTarget(res.second.name))

        assert(res.first.name != MAP_NAME)
        assert(res.second.name != JSON_NAME)
    }

    @Test
    fun testMoveFiles5() {
//        Like 3 for json
        createMapInOriginal()
        createJsonInOriginal()
        createJsonInTarget()

        val res = fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)

        assert(!isFileExistsInOriginal(MAP_NAME))
        assert(!isFileExistsInOriginal(JSON_NAME))
        assert(isFileExistsInTarget(res.first.name))
        assert(isFileExistsInTarget(res.second.name))

        assert(res.first.name != MAP_NAME)
        assert(res.second.name != JSON_NAME)
    }

    @Test
    fun testMoveFiles6() {
//        Like 5 and 3 for both files
        createMapInOriginal()
        createMapInTarget()
        createJsonInOriginal()
        createJsonInTarget()

        val res = fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)

        assert(!isFileExistsInOriginal(MAP_NAME))
        assert(!isFileExistsInOriginal(JSON_NAME))
        assert(isFileExistsInTarget(res.first.name))
        assert(isFileExistsInTarget(res.second.name))

        assert(res.first.name != MAP_NAME)
        assert(res.second.name != JSON_NAME)
    }


    @Test
    fun testMoveFiles7() {
        createJsonInOriginal()
        var exception: IOException? = null
        try {
            fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)
        } catch (e: IOException) {
            exception = e
            println(e.message.toString())
        }
        assert(exception != null)
    }

    @Test
    fun testMoveFiles8() {
        createMapInOriginal()
        var exception: IOException? = null
        try {
            fileManager.moveFilesToTargetDir(MAP_NAME, JSON_NAME)
        } catch (e: IOException) {
            exception = e
            println(e.message.toString())
        }
        assert(exception != null)
    }


//    TODO create test where there is no enough place on SD and save on Flash

    companion object {
        const val MAP_NAME = "test_map.gpkg"
        const val JSON_NAME = "test_map.json"
//        @JvmStatic
//        private lateinit var service: AsioSdkGetMapService
        private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        private val config = ServiceConfig.getInstance(appContext)
        //        private lateinit var refresh: Method
        private var fileManager = MapFileManager(appContext)
        private var originalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        private var targetDir = fileManager.sdTargetDir.path

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            config.downloadPath = originalDir
            config.targetStoragePolicy = MapConfigDto.TargetStoragePolicy.sDOnly

//            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
//            println("Path: $path")
//            val cfg = Configuration(
//                "http://localhost:3333",
//                BuildConfig.USERNAME,
//                BuildConfig.PASSWORD,
//                path,
//                16,
//                null
//            )
//
//            service = AsioSdkGetMapService(appContext)
//            service.init(cfg)
//            refresh = service::class.java.getDeclaredMethod("refreshMapState", MapPkg::class.java)
//            refresh.isAccessible = true

//            service.purgeCache()
        }

    }

    private fun createMapInTarget(){
        File(targetDir, MAP_NAME).createNewFile()
    }

    private fun createMapInOriginal(){
        File(originalDir, MAP_NAME).createNewFile()
    }

    private fun createJsonInTarget(){
        File(targetDir, JSON_NAME).createNewFile()
    }

    private fun createJsonInOriginal(){
        File(originalDir, JSON_NAME).createNewFile()
    }

    private fun isFileExistsInOriginal(fileName: String): Boolean{
        return File(originalDir, fileName).exists()
    }
    private fun isFileExistsInTarget(fileName: String): Boolean{
        return File(targetDir, fileName).exists()
    }

    @After
    fun deleteAllFiles(){
        File(originalDir, MAP_NAME).delete()
        File(originalDir, JSON_NAME).delete()

        val directory = File(targetDir)

        if (directory.exists() && directory.isDirectory) {
            val filesToDelete = directory.listFiles { file ->
                file.isFile && file.name.startsWith("test_map")
            }

            filesToDelete?.forEach { file ->
                try {
                    file.delete()
                    println("File deleted: ${file.name}")
                } catch (e: IOException) {
                    println("Error deleting file ${file.name}: ${e.message}")
                }
            }
        } else {
            println("Directory does not exist or is not a valid directory.")
        }

    }


}