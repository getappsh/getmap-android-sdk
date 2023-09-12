package com.ngsoft.getapp.sdk

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.util.concurrent.TimeUnit

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


    @Test
    fun downloadTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = PackageDownloader(appContext, "todo")

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

        var iterations = 0;
        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            println("awaiting download completion...")

            if(iterations++ > 25){
                println("breaking wait loop")
                break
            }
        }

        println("download completed...")

    }

}