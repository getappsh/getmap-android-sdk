package com.ngsoft.getapp.sdk

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DownloadTests {

    /**
    works on emulator (API 33) only.
    for my Samsung Galaxy 7 (and emulator also) add permission in AndroidManifest.xml:
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"  tools:node="replace" />
    run test on device - it will fail on permission, then run:
    adb shell pm grant com.ngsoft.getapp.sdk.test android.permission.WRITE_EXTERNAL_STORAGE
    and run again
    */
//    @JvmField
//    @get:Rule
//    var runtimeRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)


    @OptIn(ExperimentalTime::class)
    @Test
    fun downloadTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = PackageDownloader(appContext, Environment.DIRECTORY_DOWNLOADS)

        var completed = false
        var downloadId: Long = -1

        val downloadCompletionHandler: (Long) -> Unit = {
            println("processing download ID=$it completion event...")
            completed = it == downloadId
        }

        downloadId = downloader.downloadFile(
            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg",
            downloadCompletionHandler
        )

        assertNotEquals(downloadId, 0)

        val timeoutTime = TimeSource.Monotonic.markNow() + 2.minutes
        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            println("awaiting download completion...")

            if(timeoutTime.hasPassedNow()){
                println("breaking wait loop")
                break
            }
        }

        println("download completed...")

    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun packagesDownloadTest(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = PackagesDownloader(appContext, Environment.DIRECTORY_DOWNLOADS)

        var completed = false

        val downloadProgressHandler: (DownloadProgress) -> Unit = {
            println("processing download progress=$it event...")
            completed = it.isCompleted
        }

        val files = listOf(
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg",
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_16_2023_07_03T09_23_46_306Z.gpkg",
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_16_2023_07_03T09_22_00_607Z.gpkg",
            "http://getmap-dev.getapp.sh/api/Download/Orthophoto_tzor_crop_1_0_12_2023_07_03T05_46_13_022Z.gpkg",
            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_07_02T14_24_17_828Z.gpkg"
        )

        downloader.downloadFiles(files, downloadProgressHandler)

        val timeoutTime = TimeSource.Monotonic.markNow() + 5.minutes
        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            println("awaiting download completion...")

            if(timeoutTime.hasPassedNow()){
                println("breaking wait loop")
                break
            }
        }

        println("download completed...")

    }
}