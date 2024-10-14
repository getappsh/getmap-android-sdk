package com.ngsoft.getapp.sdk.helpers

import GetApp.Client.models.MapDto
import GetApp.Client.models.OfferingMapResDto
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
import com.ngsoft.getapp.sdk.MapFileManager
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.R
import com.ngsoft.getapp.sdk.downloader.FetchDownloader
import com.ngsoft.getapp.sdk.helpers.client.DiscoveryClient
import com.ngsoft.getapp.sdk.jobs.DeliveryForegroundService
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getappclient.ConnectionConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo
import com.ngsoft.tilescache.models.DeliveryFlowState
import com.ngsoft.tilescache.models.MapPkg
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

internal class DiscoveryManager(private val context: Context) {

    private val deviceInfo = DeviceInfoHelper.getInstance(context);
    private val pref = Pref.getInstance(context);
    private val client = GetAppClient(ConnectionConfig(pref.baseUrl, pref.username, pref.password))
    private val mapRepo = MapRepo(context)
    private val mapFileManager = MapFileManager(context)

    fun startDiscovery(userInitiated: Boolean): List<DiscoveryItem>{
        Timber.d("startDiscovery - userInitiated: $userInitiated")
        val offering = getOffering(userInitiated) ?: return emptyList()

        val thread = Thread{ this.handleMapPush(offering.push, userInitiated) }
        thread.start()

        val result = mutableListOf<DiscoveryItem>()
        if (offering.status == OfferingMapResDto.Status.Error){
            Timber.e("getDiscoveryCatalog: get-map offering error ${offering.error?.message}")
            throw Exception("get-map offering error ${offering.error?.message}")
        }
        offering.products?.forEach {
            result.add(
                DiscoveryItem(
                    it.id,
                    it.productId,
                    it.productName.toString(),
                    it.productVersion.toString(),
                    it.productType,
                    it.productSubType,
                    it.description,
                    it.imagingTimeBeginUTC,
                    it.imagingTimeEndUTC,
                    it.maxResolutionDeg!!,
                    it.footprint,
                    it.transparency.toString(),
                    it.region,
                    it.ingestionDate)
            )
        }

        if (!userInitiated){
            thread.join()
        }

        Timber.d("startDiscovery - number of products: ${result.size}")
        return result
    }


    private fun getOffering(userInitiated: Boolean): OfferingMapResDto?{
        repeat(3) {
            try {
                Timber.d("getOffering - retry $it")
                val offering = DiscoveryClient.deviceMapDiscovery(this.client, this.deviceInfo)
                return offering
            } catch (e: Exception) {
                Timber.e("getOffering - Failed to get Device Map offering error $e")
                if(userInitiated){
                    throw e
                }else{
                    TimeUnit.SECONDS.sleep(5L  * (it + 1))
                }
            }
        }

        Timber.e("getOffering - job failed, abort")
        if (!userInitiated){
            showDiscoveryFailedNotification()
        }

        return null
    }
    private fun handleMapPush(offering: List<MapDto>?, userInitiated: Boolean){
        Timber.i("handleMapPush - offering size: ${offering?.size}")
        offering ?: return

        val failedSet = mutableSetOf<String>()

        for (map in offering){
            val catalogId = map.catalogId ?: continue
            if (mapRepo.getByReqId(catalogId) == null){
                Timber.d("handleMapPush - new push map found: $catalogId")

                val mapPkg = map.toMapPkg() ?: continue
                val id = this.mapRepo.save(mapPkg)
//                TODO isEnoughSpace
                if (!isEnoughSpace(id)){
                    return
                }
                if (!userInitiated){
                    FetchDownloader.init(context)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        DeliveryForegroundService.startForId(context, id)
                    }catch (err: ForegroundServiceStartNotAllowedException){
                        Timber.e("handleMapPush - error starting service, probably app is closed: ${err.message}")
                        if (!userInitiated){
                            showOpenAppNotification()
                            failedSet.add(id)
                        }
                    }
                }else {
                    DeliveryForegroundService.startForId(context, id)
                }
            }
        }

        if (failedSet.isNotEmpty()){
            Timber.e("handleMapPush - write to delivery queue ${failedSet.size} maps")
            this.pref.mapOffering = failedSet;
        }
    }


    private fun showOpenAppNotification(){
        NotificationHelper(context).sendNotification(
            context.getString(R.string.notification_new_map_title),
            context.getString(R.string.notification_new_map_description),
            NotificationHelper.DISCOVERY_JOB_NEW_OFFERING_NTF_ID)
    }

    private fun showDiscoveryFailedNotification(){
        NotificationHelper(context).sendNotification(
            context.getString(R.string.notification_error_title),
            context.getString(R.string.notification_discovery_job_failed),
            NotificationHelper.DISCOVERY_JOB_FAILED_NTF_ID)
    }

    private fun MapDto.toMapPkg(): MapPkg?{
        val footprint = this.footprint ?: this.boundingBox ?: return null
        return MapPkg(
            pId = this.product?.productId ?: "dummy product",
            bBox = footprint,
            footprint = footprint,
            flowState = DeliveryFlowState.IMPORT_CREATE,
            reqId = catalogId,
            state = MapDeliveryState.START,
            statusMsg = this@DiscoveryManager.context.getString(R.string.delivery_status_queued),
            fileName = this.fileName,
            url = this.packageUrl,
            downloadStart = LocalDateTime.now(ZoneOffset.UTC)
        )
    }


//    TODO function already implemented in AsioSdkGetMapService
    private fun isEnoughSpace(id: String): Boolean{
        Timber.i("isEnoughSpace")
        val requiredSpace = pref.minAvailableSpaceMB * 1024 * 1024
        return try {
            this.mapFileManager.getAndValidateStorageDirByPolicy(requiredSpace)
            true
        }catch (io: IOException){
            Timber.e("isEnoughSpace - error: ${io.message}")
            this.mapRepo.update(
                id = id,
                state = MapDeliveryState.ERROR,
                statusMsg = context.getString(R.string.delivery_status_failed),
                statusDescr = io.message
            )
            false
        }
    }
}