package com.ngsoft.getapp.sdk

import GetApp.Client.models.DiscoveryMapDto
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoverySoftwareDto
import GetApp.Client.models.GeneralDiscoveryDto
import GetApp.Client.models.GeoLocationDto
import GetApp.Client.models.OfferingMapResDto
import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.PlatformDto
import GetApp.Client.models.PrepareDeliveryResDto
import GetApp.Client.models.SituationalDiscoveryDto
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Environment
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.downloader.FetchDownloader
import com.ngsoft.getapp.sdk.downloader.FetchDownloader.downloadFile
import com.ngsoft.getapp.sdk.downloader.FetchDownloader.isDownloadDone
import com.ngsoft.getapp.sdk.helpers.client.MapDeliveryClient
import com.ngsoft.getapp.sdk.helpers.client.MapImportClient
import com.ngsoft.getapp.sdk.helpers.logger.GlobalExceptionHandler
import com.ngsoft.getapp.sdk.helpers.logger.TimberLogger
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
import com.ngsoft.getapp.sdk.old.DownloadProgress
import com.ngsoft.getapp.sdk.utils.NetworkUtil
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.TilesCache
import com.tonyodev.fetch2.Fetch
import timber.log.Timber
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal open class DefaultGetMapService(private val appCtx: Context) : GetMapService {

    protected lateinit var client: GetAppClient
    protected lateinit var pref: Pref
    private lateinit var batteryManager: BatteryManager
    protected lateinit var mapFileManager: MapFileManager
    protected lateinit var cache: TilesCache

    override val config = ServiceConfig.getInstance(appCtx)

    open fun init(configuration: Configuration): Boolean {
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler())
        TimberLogger.initTimber()
        Timber.i("Init GetMapService")
        
        config.baseUrl = configuration.baseUrl
        config.downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        client = GetAppClient(ConnectionConfig(configuration.baseUrl, configuration.user, configuration.password))

        pref = Pref.getInstance(appCtx)
        FetchDownloader.init(appCtx)

        batteryManager = appCtx.getSystemService(BATTERY_SERVICE) as BatteryManager

        mapFileManager = MapFileManager(appCtx)

        cache = TilesCache(appCtx)


        if(configuration.serialNumber != null){
            pref.serialNumber = configuration.serialNumber
        }
        pref.username = configuration.user
        pref.password = configuration.password
        pref.baseUrl = configuration.baseUrl

        Timber.d("init - Device ID: ${pref.deviceId}")
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

    override fun setOnDownloadErrorListener(listener: (String) -> Unit) {
        TODO("Not implemented in DefaultGetMapService")
    }


    override fun generateQrCode(id: String, width: Int, height: Int): Bitmap {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun processQrCodeData(data: String): String {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun downloadMap(mp: MapProperties): String? {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun downloadUpdatedMap(id: String): String? {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun cancelDownload(id: String) {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun resumeDownload(id: String): String {
        TODO("Not implemented in DefaultGetMapService")
    }
    override fun getDownloadedMap(id: String): MapData? {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getDownloadedMaps(): List<MapData> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getDownloadedMapsLive(): LiveData<List<MapData>> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun deleteMap(id: String) {
        TODO("Not implemented in DefaultGetMapService")
    }

    override suspend fun synchronizeMapData() {
        TODO("Not implemented in DefaultGetMapService")
    }


//==================================================================================================



    override fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem> {
        Timber.i("getDiscoveryCatalog")

        val batteryPower = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        //fill that vast GetApp param...
        val query = DiscoveryMessageDto(DiscoveryMessageDto.DiscoveryType.getMinusMap,
            GeneralDiscoveryDto(
                PersonalDiscoveryDto("user-1","idNumber-123","personalNumber-123"),
                SituationalDiscoveryDto( BigDecimal("23"), bandwidth=NetworkUtil.getBandwidthQuality(appCtx)?.let { BigDecimal(it) },
                    OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC), true,
                    batteryPower.toBigDecimal(),
                    GeoLocationDto("33.4","23.3", "344")
                ),
                PhysicalDiscoveryDto(PhysicalDiscoveryDto.OSEnum.android,
                    "00-B0-D0-63-C2-26","129.2.3.4",
                    pref.deviceId, pref.serialNumber, "Yes",
                    mapFileManager.getAvailableSpaceByPolicy().toString())
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

        Timber.v("getDiscoveryCatalog - discovery object built")

        val discoveries = client.deviceApi.discoveryControllerDiscoveryCatalog(query)
        Timber.d("getDiscoveryCatalog -  offering results: $discoveries ")

        val result = mutableListOf<DiscoveryItem>()
        if (discoveries.map?.status == OfferingMapResDto.Status.Error){
            Timber.e("getDiscoveryCatalog: get-map offering error ${discoveries?.map.error?.message}")
            throw Exception("get-map offering error ${discoveries?.map.error?.message}")
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

        Timber.d("getDiscoveryCatalog - results $result")
        return result
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {
        return MapImportClient.getCreateMapImportStatus(client, inputImportRequestId)
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        return MapImportClient.createMapImport(client, inputProperties, pref.deviceId)
    }

    override fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus? {
        return MapDeliveryClient.getMapImportDeliveryStatus(client, inputImportRequestId)
    }

    override fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus? {
        return MapDeliveryClient.setMapImportDeliveryStart(client, inputImportRequestId, pref.deviceId)
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
            Timber.d("setMapImportDeploy - delivery not finished yet, nothing 2 download")
            return MapDeployState.ERROR
        }

        val file2download =
            //"http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg"
            deliveryStatus.url!!

        var completed = false
        var downloadId: Int = -1

        val fetch = Fetch.Impl.getDefaultInstance()

        downloadId = fetch.downloadFile(file2download)

        val timeoutTime = TimeSource.Monotonic.markNow() + 15.minutes

        while(!completed){
            TimeUnit.SECONDS.sleep(1)
            Timber.d("awaiting download completion...")

            if (fetch.isDownloadDone(downloadId)){
                completed = true
            }

            if(timeoutTime.hasPassedNow()){
                Timber.d("download wait loop - timed out")
                break
            }
        }

        Timber.d("download completed...")

        return MapDeployState.DONE
    }

    override fun sendBugReport(description: String?) {
        TODO("Not yet implemented")
    }
}
