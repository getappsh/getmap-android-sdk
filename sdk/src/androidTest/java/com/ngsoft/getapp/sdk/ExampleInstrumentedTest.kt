package com.ngsoft.getapp.sdk

import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
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