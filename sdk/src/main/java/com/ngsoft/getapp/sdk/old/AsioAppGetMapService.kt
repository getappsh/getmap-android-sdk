package com.ngsoft.getapp.sdk.old

import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import timber.log.Timber
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.DefaultGetMapService
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.getapp.sdk.utils.FileUtils
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
    private var zoomLevel: Int = 0



    override fun init(configuration: Configuration): Boolean {
        super.init(configuration)
        zoomLevel = configuration.zoomLevel

        packagesDownloader = PackagesDownloader(appCtx, config.downloadPath, super.downloader)
        extentUpdates = ExtentUpdates(appCtx)

        return true
    }

    override fun purgeCache(){
        cache.purge()
    }

    override fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile> {
        val result = extentUpdates.getExtentUpdates(extent, zoomLevel, updateDate)
        Timber.i("getExtentUpdates - got ${result.count()} extent updates")
        return result
    }

    @OptIn(ExperimentalTime::class)
    override fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile> {
        Timber.i("deliverExtentTiles - delivering tiles...")

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
                Timber.w("deliverExtentTiles - failed to import/deliver tile: $it")
            }
        }

        Timber.i("deliverExtentTiles - downloading tiles...")
        val downloadedTiles = mutableListOf<MapTile>()
        var completed = false
        val downloadProgressHandler: (DownloadProgress) -> Unit = { progress ->
            println("deliverExtentTiles => processing download progress=$progress event...")
            progress.packagesProgress.forEach { pkg ->
                if(pkg.isCompleted){
                    val found = tiles2download.find { FileUtils.getFileNameFromUri(it.first) == pkg.fileName }
                    if (found != null) {
                        if (downloadedTiles.indexOf(found.second) == -1){
                            found.second.fileName = pkg.fileName
                            downloadedTiles.add(found.second)

                            val dlv = deliveryMapsStatus.find { FileUtils.getFileNameFromUri(it.first) == pkg.fileName }?.second
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
        val timeoutTime = TimeSource.Monotonic.markNow() + config.downloadTimeoutMins.minutes
        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            if(timeoutTime.hasPassedNow()){
                Timber.w("deliverExtentTiles - tiles download timed out...")

                val filteredDeliveryMapsStatus = deliveryMapsStatus.filter { (url, _) ->
                    downloadedTiles.none { it.fileName ==  FileUtils.getFileNameFromUri(url) }
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

        Timber.i("deliverExtentTiles - registering delivered tiles in cache...")
        downloadedTiles.forEach{
            println("reg. tile: ${it.fileName!!} | ${it.boundingBox}")
            cache.registerTilePkg(it.productId, it.fileName!!,
                Tile(it.x, it.y, it.zoom), string2BBox(it.boundingBox),
                it.dateUpdated
            )
        }

        Timber.i("deliverExtentTiles - tiles delivery completed...")
        return downloadedTiles
    }

    private fun deliverTile(tile: MapTile): PrepareDeliveryResDto? {
        val retCreate = createMapImport(MapProperties(tile.productId, tile.boundingBox, false))
        when(retCreate?.state){
            MapImportState.IN_PROGRESS -> Timber.d("deliverTile - createMapImport => IN_PROGRESS")
            MapImportState.START -> if( !checkImportStatus(retCreate.importRequestId!!)) return null
            MapImportState.DONE -> Timber.d("deliverTile - createMapImport => DONE")
            MapImportState.CANCEL -> {
                Timber.w("deliverTile - createMapImport => CANCEL")
                return null
            }
            else -> {
                Timber.e("deliverTile - createMapImport failed: ${retCreate?.state}")
                return null
            }
        }

        val retDelivery = setMapImportDeliveryStart(retCreate.importRequestId!!)
        when(retDelivery?.state){
            MapDeliveryState.DONE -> Timber.d("deliverTile - setMapImportDeliveryStart => DONE")
            MapDeliveryState.START -> if( !checkDeliveryStatus(retCreate.importRequestId!!)) return null
            MapDeliveryState.DOWNLOAD,
            MapDeliveryState.CONTINUE,
            MapDeliveryState.PAUSE ->  Timber.d("deliverTile - setMapImportDeliveryStart => ${retDelivery.state}")
            MapDeliveryState.CANCEL -> {
                Timber.w("deliverTile - setMapImportDeliveryStart => CANCEL")
                return null
            }
            else -> {
                Timber.e("deliverTile - setMapImportDeliveryStart failed: ${retDelivery?.state}")
                return null
            }
        }

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(retCreate.importRequestId!!)
        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Timber.e("deliverTile - prepared delivery status => ${deliveryStatus.status} is not done!")
            return null
        }

        return deliveryStatus
    }

    @OptIn(ExperimentalTime::class)
    private fun checkImportStatus(requestId: String) : Boolean {
        var stat = getCreateMapImportStatus(requestId)
        val timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes
        while (stat?.state!! != MapImportState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getCreateMapImportStatus(requestId)

            when(stat?.state){
                MapImportState.ERROR -> {
                    Timber.e("checkImportStatus - MapImportState.ERROR")
                    return false
                }
                MapImportState.CANCEL -> {
                    Timber.w("checkImportStatus - MapImportState.CANCEL")
                    return false
                }
                else -> {}
            }

            if(timeoutTime.hasPassedNow()){
                Timber.w("checkImportStatus - timed out")
                return false
            }
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    private fun checkDeliveryStatus(requestId: String) : Boolean {
        var stat = getMapImportDeliveryStatus(requestId)
        val timeoutTime = TimeSource.Monotonic.markNow() + config.deliveryTimeoutMins.minutes
        while (stat?.state != MapDeliveryState.DONE){
            TimeUnit.SECONDS.sleep(1)
            stat = getMapImportDeliveryStatus(requestId)
            when(stat?.state){
                MapDeliveryState.ERROR -> {
                    Timber.e("checkDeliveryStatus - MapDeliveryState.ERROR")
                    return false
                }
                MapDeliveryState.CANCEL -> {
                    Timber.w("checkDeliveryStatus - MapDeliveryState.CANCEL")
                    return false
                }
                else -> {}
            }
            if(timeoutTime.hasPassedNow()){
                Timber.w("checkDeliveryStatus - timed out")
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
                Timber.e("sendDeliveryStatus failed error: $exc", )
            }
        }.start()
    }

    private fun initDeliveryStatus(catalogId: String): DeliveryStatusDto {
        return DeliveryStatusDto(
            deliveryStatus = DeliveryStatusDto.DeliveryStatus.start,
            type = DeliveryStatusDto.Type.map,
            deviceId = pref.deviceId,
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