package com.ngsoft.getapp.sdk

import GetApp.Client.models.CreateImportDto
import GetApp.Client.models.CreateImportResDto
import GetApp.Client.models.DiscoveryMapDto
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoverySoftwareDto
import GetApp.Client.models.GeneralDiscoveryDto
import GetApp.Client.models.GeoLocationDto
import GetApp.Client.models.ImportStatusResDto
import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.PlatformDto
import GetApp.Client.models.PrepareDeliveryReqDto
import GetApp.Client.models.PrepareDeliveryResDto
import GetApp.Client.models.SituationalDiscoveryDto
import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.models.CreateMapImportStatus
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.DownloadHebStatus
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportDeliveryStatus
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.MapTile
import com.ngsoft.getapp.sdk.models.Status
import com.ngsoft.getapp.sdk.models.StatusCode
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
    protected lateinit var cache: TilesCache


    open fun init(configuration: Configuration): Boolean {
        client = GetAppClient(ConnectionConfig(configuration.baseUrl, configuration.user, configuration.password))
        downloader = PackageDownloader(appCtx, configuration.storagePath)
        pref = Pref.getInstance(appCtx)
        cache = TilesCache(appCtx)
        return true
    }

    override fun purgeCache(){
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun getExtentUpdates(extent: MapProperties, updateDate: LocalDateTime): List<MapTile> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun deliverExtentTiles(extentTiles: List<MapTile>, onProgress: (DownloadProgress) -> Unit): List<MapTile> {
        TODO("Not implemented in DefaultGetMapService")
    }

    override fun downloadMap(mp: MapProperties, downloadStatusHandler: (DownloadHebStatus, Int) -> Unit) {
        TODO("Not implemented in DefaultGetMapService")
    }


//==================================================================================================

    override fun getDiscoveryCatalog(inputProperties: MapProperties): List<DiscoveryItem> {

        //fill that vast GetApp param...
        val query = DiscoveryMessageDto(DiscoveryMessageDto.DiscoveryType.app,
            GeneralDiscoveryDto(
                PersonalDiscoveryDto("tank","idNumber-123","personalNumber-123"),
                SituationalDiscoveryDto( BigDecimal("23"), BigDecimal("2"),
                    OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC), true, BigDecimal("34"),
                    GeoLocationDto("33.4","23.3", "344")
                ),
                PhysicalDiscoveryDto(PhysicalDiscoveryDto.OSEnum.android,
                    "00-B0-D0-63-C2-26","129.2.3.4",
                    pref.deviceId, "13kb23", "12kb", "1212Mb")
            ),

            DiscoverySoftwareDto("yatush", PlatformDto("Merkava","106", BigDecimal("223"),
                emptyList()
            )),

            DiscoveryMapDto(inputProperties.productId,"no-name","3","osm","bla-bla",
                inputProperties.boundingBox,
                "WGS84", LocalDateTime.now().toString(), LocalDateTime.now().toString(), LocalDateTime.now().toString(),
                "DJI Mavic","raster","N/A","ME","CCD","3.14","0.12"
            )
        )

        val discoveries = client.deviceApi.deviceControllerDiscoveryCatalog(query)
        val result = mutableListOf<DiscoveryItem>()
        discoveries.map?.forEach {
            result.add(DiscoveryItem(it.productId.toString(),it.productName.toString(), it.boundingBox.toString(), it.updateDateUTC!!))
        }

        return result
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.getMapApi.getMapControllerGetImportStatus(inputImportRequestId)
        val result = CreateMapImportStatus()
        result.importRequestId = inputImportRequestId
        result.statusCode = Status()

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
                result.state = MapImportState.IN_PROGRESS
            }
            ImportStatusResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.CANCEL
            }
            ImportStatusResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.NOT_FOUND

                if(status.fileName == "Request not found")
                    result.statusCode!!.statusCode = StatusCode.REQUEST_ID_NOT_FOUND

                result.state = MapImportState.ERROR
            }
            else -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
                result.state = MapImportState.ERROR
            }
        }

        return result
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        if(inputProperties == null)
            throw Exception("invalid inputProperties")

        val params = CreateImportDto(pref.deviceId, GetApp.Client.models.MapProperties(
            BigDecimal(12), "dummy name", inputProperties.productId, inputProperties.boundingBox,
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
            CreateImportResDto.Status.cancel -> {
                result.statusCode!!.statusCode = StatusCode.SUCCESS
                result.state = MapImportState.CANCEL
            }
            CreateImportResDto.Status.error -> {
                result.statusCode!!.statusCode = StatusCode.INTERNAL_SERVER_ERROR
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
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = client.getMapApi.getMapControllerCancelImportCreate(inputImportRequestId)

        val result = MapImportDeliveryStatus()
        result.importRequestId = inputImportRequestId
        result.message = Status()
        result.message!!.statusCode = StatusCode.SUCCESS

        when(status.status){
            CreateImportResDto.Status.start -> result.state = MapDeliveryState.START
            CreateImportResDto.Status.inProgress -> result.state = MapDeliveryState.CONTINUE
            CreateImportResDto.Status.done -> result.state = MapDeliveryState.DONE
            CreateImportResDto.Status.cancel -> result.state = MapDeliveryState.CANCEL
            CreateImportResDto.Status.error -> result.state = MapDeliveryState.ERROR
        }

        return result
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

}
