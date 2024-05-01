package com.ngsoft.getapp.sdk.delivery

import android.app.Application
import com.ngsoft.getapp.sdk.MapFileManager
import com.ngsoft.getapp.sdk.PackageDownloader
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.ServiceConfig
import com.ngsoft.getappclient.GetAppClient
import com.ngsoft.tilescache.MapRepo

internal data class DeliveryContext(
    val config: ServiceConfig,
    val mapRepo: MapRepo,
    val downloader: PackageDownloader,
    val mapFileManager: MapFileManager,
    val pref: Pref,
    val client: GetAppClient,
    val app: Application
)