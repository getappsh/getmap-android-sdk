package com.ngsoft.getapp.sdk

import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.tilescache.TilesCache
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import com.ngsoft.tilescache.models.TilePkg
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class AsioAppGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val TAG = "AsioAppGetMapService"
    private lateinit var packagesDownloader: PackagesDownloader
    private lateinit var extentUpdates: ExtentUpdates
    private lateinit var cache: TilesCache
    private var zoomLevel: Int = 0
    private var deliveryTimeoutMinutes: Int = 5
    private var downloadTimeoutMinutes: Int = 5


    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        zoomLevel = configuration.zoomLevel
        deliveryTimeoutMinutes = configuration.deliveryTimeout
        downloadTimeoutMinutes = configuration.downloadTimeout

        //todo: investigate AppContext substitution/mocking for non-android tests instead of that:
        if(appCtx::class.java.name != "com.ngsoft.sharedtest.FakeAppContext"){
            packagesDownloader = PackagesDownloader(appCtx, configuration.storagePath, super.downloader)
            extentUpdates = ExtentUpdates(appCtx)
            cache = TilesCache(appCtx)
        }

        return true
    }

    override fun purgeCache(){
        cache.purge()
    }

    override fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile> {
        val result = extentUpdates.getExtentUpdates(extent, zoomLevel, updateDate)
        Log.i(TAG,"getExtentUpdates - got ${result.count()} extent updates")
        return result
    }

    @OptIn(ExperimentalTime::class)
    override fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile> {
        Log.i(TAG,"deliverExtentTiles - delivering tiles...")
        val tiles2download = mutableListOf<Pair<String, MapTile>>()
        extentTiles.forEach {
            val tileFile = deliverTile(it)
            if(tileFile != null) {
                tiles2download.add(Pair(tileFile, it))
            } else {
                Log.w(TAG,"deliverExtentTiles - failed to import/deliver tile: $it")
            }
        }

        Log.i(TAG,"deliverExtentTiles - downloading tiles...")
        val downloadedTiles = mutableListOf<MapTile>()
        var completed = false
        val downloadProgressHandler: (DownloadProgress) -> Unit = { progress ->
            println("deliverExtentTiles => processing download progress=$progress event...")
            progress.packagesProgress.forEach { pkg ->
                if(pkg.isCompleted){
                    val found = tiles2download.find { PackageDownloader.getFileNameFromUri(it.first) == pkg.fileName }
                    if (found != null) {
                        if (downloadedTiles.indexOf(found.second) == -1){
                            found.second.fileName = pkg.fileName
                            downloadedTiles.add(found.second)
                        }
                    } else {
                        throw IllegalStateException("Failed to find MapTile by file name = ${pkg.fileName}")
                    }
                }
            }
            completed = progress.isCompleted
            onProgress.invoke(progress)
        }

        packagesDownloader.downloadFiles(tiles2download.map { it.first }, downloadProgressHandler)
        val timeoutTime = TimeSource.Monotonic.markNow() + downloadTimeoutMinutes.minutes
        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            if(timeoutTime.hasPassedNow()){
                Log.w(TAG,"deliverExtentTiles - tiles download timed out...")
                break
            }
        }

        Log.i(TAG,"deliverExtentTiles - registering delivered tiles in cache...")
        downloadedTiles.forEach{
            println("reg. tile: ${it.fileName!!} | ${it.boundingBox}")
            cache.registerTilePkg(TilePkg( it.productId, it.fileName!!,
                Tile(it.x, it.y, it.zoom), string2BBox(it.boundingBox),
                LocalDateTime.now(), it.dateUpdated, LocalDateTime.now())
        )}

        Log.i(TAG,"deliverExtentTiles - tiles delivery completed...")
        return downloadedTiles
    }

    private fun deliverTile(tile: MapTile): String? {
        val retCreate = createMapImport(MapProperties(tile.productId, tile.boundingBox, false))
        when(retCreate?.state){
            MapImportState.IN_PROGRESS -> Log.d(TAG,"deliverTile - createMapImport => IN_PROGRESS")
            MapImportState.START -> if( !checkImportStatus(retCreate.importRequestId!!)) return null
            MapImportState.DONE -> Log.d(TAG,"deliverTile - createMapImport => DONE")
            MapImportState.CANCEL -> {
                Log.w(TAG,"deliverTile - createMapImport => CANCEL")
                return null
            }
            else -> {
                Log.e(TAG,"deliverTile - createMapImport failed: ${retCreate?.state}")
                return null
            }
        }

        val retDelivery = setMapImportDeliveryStart(retCreate.importRequestId!!)
        when(retDelivery?.state){
            MapDeliveryState.DONE -> Log.d(TAG,"deliverTile - setMapImportDeliveryStart => DONE")
            MapDeliveryState.START -> if( !checkDeliveryStatus(retCreate.importRequestId!!)) return null
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE,
            MapDeliveryState.PAUSE ->  Log.d(TAG,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
            MapDeliveryState.CANCEL -> {
                Log.w(TAG,"deliverTile - setMapImportDeliveryStart => CANCEL")
                return null
            }
            else -> {
                Log.e(TAG,"deliverTile - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                return null
            }
        }

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(retCreate.importRequestId!!)
        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Log.e(TAG,"deliverTile - prepared delivery status => ${deliveryStatus.status} is not done!")
            return null
        }

        return deliveryStatus.url
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatus(requestId: String) : Boolean {
        var stat = getCreateMapImportStatus(requestId)
        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state!! != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getCreateMapImportStatus(requestId)

            when(stat?.state){
                MapImportState.ERROR -> {
                    Log.e(TAG,"checkImportStatus - MapImportState.ERROR")
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(TAG,"checkImportStatus - MapImportState.CANCEL")
                    return false
                }
                else -> {}
            }

            if(timeoutTime.hasPassedNow()){
                Log.w(TAG,"checkImportStatus - timed out")
                return false
            }
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    private fun checkDeliveryStatus(requestId: String) : Boolean {
        var stat = getMapImportDeliveryStatus(requestId)
        val timeoutTime = TimeSource.Monotonic.markNow() + deliveryTimeoutMinutes.minutes
        while (stat?.state != MapDeliveryState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getMapImportDeliveryStatus(requestId)
            when(stat?.state){
                MapDeliveryState.ERROR -> {
                    Log.e(TAG,"checkDeliveryStatus - MapDeliveryState.ERROR")
                    return false
                }
                MapDeliveryState.CANCEL -> {
                    Log.w(TAG,"checkDeliveryStatus - MapDeliveryState.CANCEL")
                    return false
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Log.w(TAG,"checkDeliveryStatus - timed out")
                return false
            }
        }

        return true
    }

    private fun string2BBox(bBoxStr: String): BBox {
        val digits = bBoxStr.split(',')
        return BBox(
            digits[0].toDouble(),
            digits[1].toDouble(),
            digits[2].toDouble(),
            digits[3].toDouble()
        )
    }
}