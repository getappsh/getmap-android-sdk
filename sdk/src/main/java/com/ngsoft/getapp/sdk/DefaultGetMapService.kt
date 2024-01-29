package com.ngsoft.getapp.sdk

import GetApp.Client.models.CreateImportDto
import GetApp.Client.models.CreateImportResDto
import GetApp.Client.models.DeliveryStatusDto
import GetApp.Client.models.DiscoveryMapDto
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoverySoftwareDto
import GetApp.Client.models.ErrorDto
import GetApp.Client.models.GeneralDiscoveryDto
import GetApp.Client.models.GeoLocationDto
import GetApp.Client.models.ImportStatusResDto
import GetApp.Client.models.OfferingMapResDto
import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.PlatformDto
import GetApp.Client.models.PrepareDeliveryReqDto
import GetApp.Client.models.PrepareDeliveryResDto
import GetApp.Client.models.SituationalDiscoveryDto
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.helpers.GlobalExceptionHandler
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DeliveryStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getapp.sdk.old.DownloadProgress
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.TilesCache
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal open class DefaultGetMapService(private val appCtx: Context) : GetMapService {

    private val _tag = "DefaultGetMapService"
    protected lateinit var client: GetAppClient
    protected lateinit var downloader: PackageDownloader
    protected lateinit var pref: Pref
    private lateinit var batteryManager: BatteryManager
    protected lateinit var mapFileManager: MapFileManager
    protected lateinit var cache: TilesCache

    override val config: GetMapService.GeneralConfig = ServiceConfig.getInstance(appCtx)

    open fun init(configuration: Configuration): Boolean {
        Log.i(_tag, "Init GetMapService" )

        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler())

        config.storagePath = configuration.storagePath
        client = GetAppClient(ConnectionConfig(configuration.baseUrl, configuration.user, configuration.password))

        val dir = Environment.DIRECTORY_DOWNLOADS
        downloader = PackageDownloader(appCtx, dir)

        pref = Pref.getInstance(appCtx)

        batteryManager = appCtx.getSystemService(BATTERY_SERVICE) as BatteryManager

        mapFileManager = MapFileManager(appCtx, downloader)

        cache = TilesCache(appCtx)


        if(configuration.imei != null){
            pref.deviceId = configuration.imei
        }
        pref.username = configuration.user
        pref.password = configuration.password
        pref.baseUrl = configuration.baseUrl

        Log.d(_tag, "init - Device ID: ${pref.deviceId}")

        return true
    }

    override fun purgeCache(){
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun fetchInventoryUpdates(): List<String> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun fetchConfigUpdates() {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun setOnInventoryUpdatesListener(listener: (List<String>) -> Unit) {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun generateQrCode(id: String, width: Int, height: Int): Bitmap {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun processQrCodeData(data: String, downloadStatusHandler: (MapDownloadData) -> Unit): String {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (MapDownloadData) -> Unit): String? {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun downloadUpdatedMap(id: String, downloadStatusHandler: (MapDownloadData) -> Unit): String? {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun cancelDownload(id: String) {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun registerDownloadHandler(id: String, downloadStatusHandler: (MapDownloadData) -> Unit) {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun resumeDownload(id: String, downloadStatusHandler: (MapDownloadData) -> Unit): String {
        TODO("Not implemented in DefaultGetMapService")
    }
    override fun getDownloadedMap(id: String): MapDownloadData? {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getDownloadedMaps(): List<MapDownloadData> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getDownloadedMapsLive(): LiveData<List<MapDownloadData>> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun deleteMap(id: String) {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun synchronizeMapData() {
        TODO("Not implemented in DefaultGetMapService")
    }


//==================================================================================================

    override fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem> {
        Log.i(_tag, "getDiscoveryCatalog")

        val batteryPower = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        //fill that vast GetApp param...
        val query = DiscoveryMessageDto(DiscoveryMessageDto.DiscoveryType.getMinusMap,
            GeneralDiscoveryDto(
                PersonalDiscoveryDto("user-1","idNumber-123","personalNumber-123"),
                SituationalDiscoveryDto( BigDecimal("23"), BigDecimal("2"),
                    OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC), true,
                    batteryPower.toBigDecimal(),
                    GeoLocationDto("33.4","23.3", "344")
                ),
                PhysicalDiscoveryDto(PhysicalDiscoveryDto.OSEnum.android,
                    "00-B0-D0-63-C2-26","129.2.3.4",
                    pref.deviceId, pref.generateDeviceId(), "Yes",
                    FileUtils.getAvailableSpace(config.storagePath).toString())
            ),

            DiscoverySoftwareDto("yatush", PlatformDto("Olar","1", BigDecimal("0"),
                emptyList()
            )),

            DiscoveryMapDto(inputProperties.productId,"no-name","3","osm","bla-bla",
                inputProperties.boundingBox,
                "WGS84", LocalDateTime.now().toString(), LocalDateTime.now().toString(), LocalDateTime.now().toString(),
                "DJI Mavic","raster","N/A","ME","CCD","3.14","0.12"
            )
        )

        Log.v(_tag, "getDiscoveryCatalog - discovery object built")

        val discoveries = client.deviceApi.discoveryControllerDiscoveryCatalog(query)
        Log.d(_tag, "getDiscoveryCatalog -  offering results: $discoveries ")

        val result = mutableListOf<DiscoveryItem>()
        if (discoveries.map?.status == OfferingMapResDto.Status.error){
            Log.e(_tag, "getDiscoveryCatalog: get-map offering error ${discoveries.map.error?.message}")
            throw Exception("get-map offering error ${discoveries.map.error?.message}")
        }

        discoveries.map?.products?.forEach {
            result.add(DiscoveryItem(
                it.id.toString(),
                it.productId.toString(),
                it.productName.toString(),
                it.productVersion.toString(),
                it.productType,
                it.productSubType,
                it.description,
                it.imagingTimeBeginUTC,
                it.imagingTimeEndUTC,
                it.maxResolutionDeg!!,
                it.footprint.toString(),
                it.transparency.toString(),
                it.region,
                it.ingestionDate,
            ))
        }

        Log.d(_tag, "getDiscoveryCatalog - results $result")
        return result
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.getMapApi.getMapControllerGetImportStatus(inputImportRequestId)
        val result = CreateMapImportStatus()
        result.importRequestId = inputImportRequestId
        result.statusCode = Status()

        result.progress = status.metaData?.progress

        when(status.status) {
            ImportStatusResDto.Status.start -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.START
            }
            ImportStatusResDto.Status.done -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.DONE
            }
            ImportStatusResDto.Status.inProgress -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = status.error?.message
                result.state = MapImportState.IN_PROGRESS
            }
            ImportStatusResDto.Status.pending -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            ImportStatusResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = status.error?.message
                result.state = MapImportState.CANCEL
            }
            ImportStatusResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.NOT_FOUND
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)

                if(status.error?.errorCode == ErrorDto.ErrorCode.notFound)
                    result.statusCode!!.statusCode = StatusCode.REQUEST_ID_NOT_FOUND

                result.state = MapImportState.ERROR
            }
            ImportStatusResDto.Status.pause -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            ImportStatusResDto.Status.expired -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            ImportStatusResDto.Status.archived -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            else -> {
                result.state = MapImportState.ERROR
                if(status.error?.errorCode == ErrorDto.ErrorCode.notFound)
                    result.statusCode!!.statusCode = StatusCode.REQUEST_ID_NOT_FOUND
                else
                    result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR

            }
        }

        return result
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        if(inputProperties == null)
            throw Exception("invalid inputProperties")

        val params = CreateImportDto(pref.deviceId, GetApp.Client.models.MapProperties(
            BigDecimal(12), inputProperties.boundingBox,"dummy name", inputProperties.productId,
            BigDecimal(0), BigDecimal(0)
        ))

        val status = client.getMapApi.getMapControllerCreateImport(params)
        val result = CreateMapImportStatus()
        result.importRequestId = status.importRequestId
        result.statusCode = Status()

        when(status.status) {
            CreateImportResDto.Status.start -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.START
            }
            CreateImportResDto.Status.done -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.DONE
            }
            CreateImportResDto.Status.inProgress -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            CreateImportResDto.Status.pending -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.IN_PROGRESS
            }
            CreateImportResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.CANCEL
            }
            CreateImportResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            CreateImportResDto.Status.pause -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            CreateImportResDto.Status.expired -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }
            CreateImportResDto.Status.archived -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.statusCode!!.messageLog = (status.error?.message ?: status.importRequestId)
                result.state = MapImportState.ERROR
            }

            else -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.statusCode!!.messageLog = status.error?.message
                result.state = MapImportState.ERROR

            }
        }

        return result
    }

    override fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(inputImportRequestId)

        Log.d(_tag,"getMapImportDeliveryStatus | download url: ${status.url}")

        val result = MapImportDeliveryStatus()
        result.importRequestId = status.catalogId
        result.message = Status()
        result.message!!.statusCode = StatusCode.SUCCESS
        result.url = status.url

        when (status.status){
            PrepareDeliveryResDto.Status.start -> result.state = MapDeliveryState.START
            PrepareDeliveryResDto.Status.inProgress -> result.state = MapDeliveryState.CONTINUE
            PrepareDeliveryResDto.Status.done -> result.state = MapDeliveryState.DONE
            PrepareDeliveryResDto.Status.error -> result.state = MapDeliveryState.ERROR
        }

        return result
    }

    override fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val prepareDelivery = PrepareDeliveryReqDto(inputImportRequestId, pref.deviceId, PrepareDeliveryReqDto.ItemType.map)
        val status = client.deliveryApi.deliveryControllerPrepareDelivery(prepareDelivery)

        Log.d(_tag,"setMapImportDeliveryStart | download url: ${status.url}")

        val result = MapImportDeliveryStatus()
        result.importRequestId = inputImportRequestId
        result.message = Status()
        result.message!!.statusCode = StatusCode.SUCCESS
        result.url = status.url

        when (status.status){
            PrepareDeliveryResDto.Status.start -> result.state = MapDeliveryState.START
            PrepareDeliveryResDto.Status.inProgress -> result.state = MapDeliveryState.CONTINUE
            PrepareDeliveryResDto.Status.done -> result.state = MapDeliveryState.DONE
            PrepareDeliveryResDto.Status.error -> result.state = MapDeliveryState.ERROR
        }

        return result
    }

    override fun setMapImportDeliveryPause(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        //not implemented on GetApp side AFAIK
        val result = MapImportDeliveryStatus()
        result.importRequestId = inputImportRequestId
        result.message = Status()
        result.message!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
        result.state = MapDeliveryState.ERROR
        return result
    }

    override fun setMapImportDeliveryCancel(inputImportRequestId: String?): MapImportDeliveryStatus? {
        TODO("Not implemented in DefaultGetMapService")
    }

    @OptIn(ExperimentalTime::class)
    override fun setMapImportDeploy(inputImportRequestId: String?, inputState: MapDeployState?): MapDeployState? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val deliveryStatus = client.deliveryApi.deliveryControllerGetPreparedDeliveryStatus(inputImportRequestId)

        if(deliveryStatus.status != PrepareDeliveryResDto.Status.done) {
            Log.d(_tag,"setMapImportDeploy - delivery not finished yet, nothing 2 download")
            return MapDeployState.ERROR
        }

        val file2download =
            //"http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg"
            deliveryStatus.url!!

        var completed = false
        var downloadId: Long = -1

        val downloadCompletionHandler: (Long) -> Unit = {
            Log.d(_tag,"processing download ID=$it completion event...")
            completed = it == downloadId
        }

        downloadId = downloader.downloadFile(file2download, downloadCompletionHandler)

        val timeoutTime = TimeSource.Monotonic.markNow() + 15.minutes

        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            Log.d(_tag,"awaiting download completion...")

            if(timeoutTime.hasPassedNow()){
                Log.d(_tag,"download wait loop - timed out")
                break
            }
        }

        Log.d(_tag,"download completed...")

        return MapDeployState.DONE
    }

    fun pushDeliveryStatus(deliveryStatus: DeliveryStatus){
        val status = when(deliveryStatus.state){
            MapDeliveryState.START -> DeliveryStatusDto.DeliveryStatus.start
            MapDeliveryState.DONE -> DeliveryStatusDto.DeliveryStatus.done
            MapDeliveryState.ERROR -> DeliveryStatusDto.DeliveryStatus.error
            MapDeliveryState.CANCEL -> DeliveryStatusDto.DeliveryStatus.cancelled
            MapDeliveryState.PAUSE -> DeliveryStatusDto.DeliveryStatus.pause
            MapDeliveryState.CONTINUE -> DeliveryStatusDto.DeliveryStatus.`continue`
            MapDeliveryState.DOWNLOAD -> DeliveryStatusDto.DeliveryStatus.download
            MapDeliveryState.DELETED -> DeliveryStatusDto.DeliveryStatus.deleted
        }

        val dlv = DeliveryStatusDto(
            type = DeliveryStatusDto.Type.map,
            deviceId = pref.deviceId,
            deliveryStatus = status,
            catalogId = deliveryStatus.reqId,
            downloadData = deliveryStatus.progress?.toBigDecimal(),
            downloadStart = deliveryStatus.start,
            downloadStop = deliveryStatus.stop,
            downloadDone = deliveryStatus.done,
            currentTime = OffsetDateTime.now()
        )
        Thread {
            try {
                client.deliveryApi.deliveryControllerUpdateDownloadStatus(dlv)
            } catch (exc: Exception) {
                Log.e(_tag, "sendDeliveryStatus failed error: ${exc.message.toString()}")
                exc.printStackTrace()
            }
        }.start()
    }
}
