package com.ngsoft.getapp.sdk

import GetApp.Client.models.DeliveryStatusDto
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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class AsioAppGetMapService (private val appCtx: Context) : DefaultGetMapService(appCtx) {

    private val _tag = "AsioAppGetMapService"
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

        packagesDownloader = PackagesDownloader(appCtx, configuration.storagePath, super.downloader)
        extentUpdates = ExtentUpdates(appCtx)
        cache = TilesCache(appCtx)

        return true
    }

    override fun purgeCache(){
        cache.purge()
    }

    override fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile> {
        val result = extentUpdates.getExtentUpdates(extent, zoomLevel, updateDate)
        Log.i(_tag,"getExtentUpdates - got ${result.count()} extent updates")
        return result
    }

    @OptIn(ExperimentalTime::class)
    override fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile> {
        Log.i(_tag,"deliverExtentTiles - delivering tiles...")

        val tiles2download = mutableListOf<Pair<String, MapTile>>()
        val deliveryMapsStatus = mutableListOf<Pair<String, DeliveryStatusDto>>()
        extentTiles.forEach {
            val tileFile = deliverTile(it)
            if(tileFile?.url != null) {
                tiles2download.add(Pair(tileFile.url, it))
                val dlv =  initDeliveryStatus(tileFile.catalogId)
                sendDeliveryStatus(dlv)
                deliveryMapsStatus.add(Pair(tileFile.url, dlv))
            } else {
                Log.w(_tag,"deliverExtentTiles - failed to import/deliver tile: $it")
            }
        }

        Log.i(_tag,"deliverExtentTiles - downloading tiles...")
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

                            val dlv = deliveryMapsStatus.find { PackageDownloader.getFileNameFromUri(it.first) == pkg.fileName }?.second
                            if (dlv != null){
                                val updated = dlv.copy(
                                    deliveryStatus = DeliveryStatusDto.DeliveryStatus.done,
                                    downloadDone = OffsetDateTime.now()
                                )
                                sendDeliveryStatus(updated)
                            }
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
                Log.w(_tag,"deliverExtentTiles - tiles download timed out...")

                val filteredDeliveryMapsStatus = deliveryMapsStatus.filter { (url, _) ->
                    downloadedTiles.none { it.fileName ==  PackageDownloader.getFileNameFromUri(url) }
                }

                filteredDeliveryMapsStatus.forEach {(_, dlv)->
                    val updated = dlv.copy(
                        deliveryStatus = DeliveryStatusDto.DeliveryStatus.error,
                        downloadStop = OffsetDateTime.now()
                    )
                    sendDeliveryStatus(updated)
                }

                break
            }
        }

        Log.i(_tag,"deliverExtentTiles - registering delivered tiles in cache...")
        downloadedTiles.forEach{
            println("reg. tile: ${it.fileName!!} | ${it.boundingBox}")
            cache.registerTilePkg(it.productId, it.fileName!!,
                Tile(it.x, it.y, it.zoom), string2BBox(it.boundingBox),
                it.dateUpdated
            )
        }

        Log.i(_tag,"deliverExtentTiles - tiles delivery completed...")
        return downloadedTiles
    }

    private fun deliverTile(tile: MapTile): PrepareDeliveryResDto? {
        val retCreate = createMapImport(MapProperties(tile.productId, tile.boundingBox, false))
        when(retCreate?.state){
            MapImportState.IN_PROGRESS -> Log.d(_tag,"deliverTile - createMapImport => IN_PROGRESS")
            MapImportState.START -> if( !checkImportStatus(retCreate.importRequestId!!)) return null
            MapImportState.DONE -> Log.d(_tag,"deliverTile - createMapImport => DONE")
            MapImportState.CANCEL -> {
                Log.w(_tag,"deliverTile - createMapImport => CANCEL")
                return null
            }
            else -> {
                Log.e(_tag,"deliverTile - createMapImport failed: ${retCreate?.state}")
                return null
            }
        }

        val retDelivery = setMapImportDeliveryStart(retCreate.importRequestId!!)
        when(retDelivery?.state){
            MapDeliveryState.DONE -> Log.d(_tag,"deliverTile - setMapImportDeliveryStart => DONE")
            MapDeliveryState.START -> if( !checkDeliveryStatus(retCreate.importRequestId!!)) return null
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE,
            MapDeliveryState.PAUSE ->  Log.d(_tag,"deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
            MapDeliveryState.CANCEL -> {
                Log.w(_tag,"deliverTile - setMapImportDeliveryStart => CANCEL")
                return null
            }
            else -> {
                Log.e(_tag,"deliverTile - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                return null
            }
        }

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(retCreate.importRequestId!!)
        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Log.e(_tag,"deliverTile - prepared delivery status => ${deliveryStatus.status} is not done!")
            return null
        }

        return deliveryStatus
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
                    Log.e(_tag,"checkImportStatus - MapImportState.ERROR")
                    return false
                }
                MapImportState.CANCEL -> {
                    Log.w(_tag,"checkImportStatus - MapImportState.CANCEL")
                    return false
                }
                else -> {}
            }

            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkImportStatus - timed out")
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
                    Log.e(_tag,"checkDeliveryStatus - MapDeliveryState.ERROR")
                    return false
                }
                MapDeliveryState.CANCEL -> {
                    Log.w(_tag,"checkDeliveryStatus - MapDeliveryState.CANCEL")
                    return false
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Log.w(_tag,"checkDeliveryStatus - timed out")
                return false
            }
        }

        return true
    }

    private fun sendDeliveryStatus(dlv: DeliveryStatusDto) {
        Thread{
            try{
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            }catch (exc: Exception){
                Log.e(_tag, "sendDeliveryStatus failed error: $exc", )
            }
        }.start()
    }

    private fun initDeliveryStatus(catalogId: String): DeliveryStatusDto {
        return DeliveryStatusDto(
            deliveryStatus = DeliveryStatusDto.DeliveryStatus.start,
            type = DeliveryStatusDto.Type.map,
            deviceId = "getapp-agent",
            catalogId = catalogId,
            downloadStart=OffsetDateTime.now(),
        )
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