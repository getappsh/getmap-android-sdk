package com.ngsoft.getapp.sdk

import GetApp.Client.infrastructure.ClientException
import GetApp.Client.infrastructure.ServerException
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.old.DownloadProgress
import com.ngsoft.getapp.sdk.old.PackageDownloader
import com.ngsoft.getapp.sdk.old.PackagesDownloader
import com.ngsoft.getappclient.VpnExceptionInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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
    adb shell -d pm grant com.ngsoft.getapp.sdk.test android.permission.WRITE_EXTERNAL_STORAGE
    and run again
     or run this from an app
    if (!Environment.isExternalStorageManager()){
    val intent = Intent()
    intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
    val uri = Uri.fromParts("package", "com.ngsoft.getapp.sdk.test", null)
    intent.data = uri
    startActivity(intent)
    }
    */
//    @JvmField
//    @get:Rule
//    var runtimeRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)


    @OptIn(ExperimentalTime::class)
    @Test
    fun downloadTest() {

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = PackageDownloader(appContext, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)

        var tmr: Timer? = null
        var completed = false
        var downloadId: Long = -1

        val downloadCompletionHandler: (Long) -> Unit = {
            println("processing download ID=$it completion event...")
            completed = it == downloadId
            if(completed)
                tmr?.cancel()
        }

        downloadId = downloader.downloadFile(
            //"http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg",
            "http://getmap-dev.getapp.sh/api/Download/dwnld-test123.gpkg",
            onDownloadCompleted = downloadCompletionHandler
        )

        assertNotEquals(downloadId, 0)

        tmr = timer(initialDelay = 0, period = 250 ) {
            val progress = downloader.queryProgress(downloadId)
            println("download progress = $progress")
        }

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
        val downloader = PackagesDownloader(appContext, Environment.DIRECTORY_DOWNLOADS, null)

        var completed = false

        val downloadProgressHandler: (DownloadProgress) -> Unit = {
            println("processing download progress event:\n| $it |")
            completed = it.isCompleted
        }

        val files = listOf(
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg",
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_16_2023_07_03T09_23_46_306Z.gpkg",
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_16_2023_07_03T09_22_00_607Z.gpkg",
//            "http://getmap-dev.getapp.sh/api/Download/Orthophoto_tzor_crop_1_0_12_2023_07_03T05_46_13_022Z.gpkg",
//            "http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_07_02T14_24_17_828Z.gpkg"

            "http://getmap-dev.getapp.sh/api/Download/dwnld-test123.gpkg",
            "http://getmap-dev.getapp.sh/api/Download/dwnld-test456.gpkg",
            "http://getmap-dev.getapp.sh/api/Download/archive.tar.gz"

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

    @OptIn(ExperimentalTime::class)
    @Test
    fun timerTest(){
        val completed = false

        val t = timer(initialDelay = 0, period = 100 ) {
            println("tick...")
        }

        val timeoutTime = TimeSource.Monotonic.markNow() + 5.seconds
        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            println("awaiting download completion...")

            if(timeoutTime.hasPassedNow()){
                println("breaking wait loop")
                t.cancel()
                break
            }
        }

        println("download completed...")

    }



    @Test
    fun vpnInterceptorTest(){
        val client = OkHttpClient.Builder()
            .addInterceptor(VpnExceptionInterceptor())
            .build()
        val request = Request.Builder().url("http://even.np.pz/").build()

        try {
            val res = client.newCall(request).execute()
            println(res)
        }catch (e: IOException){
            println("IOException")
            println(e.message.toString())
            e.printStackTrace()
        }catch (e: UnsupportedOperationException){
            println("UnsupportedOperationException")
            println(e.message.toString())
            e.printStackTrace()
        }catch (e: ClientException){
            println("ClientException")
            println(e.message.toString())
            e.printStackTrace()
        }catch (e: ServerException){
            println("ServerException")
            println(e.message.toString())
            e.printStackTrace()
        }

    }
    @OptIn(ExperimentalTime::class)
    @Test
    fun vpnDownloadTest(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = PackageDownloader(appContext, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)
        var completed = false
        val downloadId = downloader.downloadFile(
     "http://even.np.pz/a.json"
        ){
            println("download info = ${downloader.queryStatus(it)}")
            completed = true
        }


        val tmr = timer(initialDelay = 0, period = 250 ) {
            val progress = downloader.queryProgress(downloadId)
            println("download info = ${downloader.queryStatus(downloadId)}")

        }


        val timeoutTime = TimeSource.Monotonic.markNow() + 30.seconds

        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            println("awaiting download completion...")

            if(timeoutTime.hasPassedNow()){
                println("breaking wait loop")
                break
            }

        }

        tmr.cancel()
    }

}