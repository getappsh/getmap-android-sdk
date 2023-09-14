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
class ExampleInstrumentedTest {

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


    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ngsoft.getapp.sdk.test", appContext.packageName)
    }

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

}