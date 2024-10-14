package com.ngsoft.getapp.sdk

import GetApp.Client.models.OfferingMapResDto
import GetApp.Client.models.PrepareDeliveryResDto
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.downloader.FetchDownloader
import com.ngsoft.getapp.sdk.downloader.FetchDownloader.downloadFile
import com.ngsoft.getapp.sdk.downloader.FetchDownloader.isDownloadDone
import com.ngsoft.getapp.sdk.helpers.DeviceInfoHelper
import com.ngsoft.getapp.sdk.helpers.DiscoveryManager
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
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.TilesCache
import com.tonyodev.fetch2.Fetch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal open class DefaultGetMapService(private val appCtx: Context) : GetMapService {

    protected lateinit var client: GetAppClient
    protected lateinit var pref: Pref
    protected lateinit var mapFileManager: MapFileManager
    protected lateinit var cache: TilesCache
    protected lateinit var deviceInfo: DeviceInfoHelper

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

        mapFileManager = MapFileManager(appCtx)

        cache = TilesCache(appCtx)


        if(configuration.serialNumber != null){
            pref.serialNumber = configuration.serialNumber
        }
        pref.username = configuration.user
        pref.password = configuration.password
        pref.baseUrl = configuration.baseUrl

        deviceInfo = DeviceInfoHelper.getInstance(appCtx);

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

    override fun synchronizeMapData() {
        TODO("Not implemented in DefaultGetMapService")
    }


//==================================================================================================



    override fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem> {
        Timber.i("getDiscoveryCatalog")
        val result = DiscoveryManager(appCtx).startDiscovery(true);

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
}
