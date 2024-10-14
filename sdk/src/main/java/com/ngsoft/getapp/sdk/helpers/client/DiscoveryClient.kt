package com.ngsoft.getapp.sdk.helpers.client

import GetApp.Client.models.DiscoveryMapDto
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoverySoftwareDto
import GetApp.Client.models.GeneralDiscoveryDto
import GetApp.Client.models.OfferingMapResDto
import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.PlatformDto
import GetApp.Client.models.SituationalDiscoveryDto
import com.ngsoft.getapp.sdk.helpers.DeviceInfoHelper
import com.ngsoft.getappclient.GetAppClient
import timber.log.Timber
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal object DiscoveryClient {


    fun deviceMapDiscovery(client: GetAppClient, deviceInfo: DeviceInfoHelper): OfferingMapResDto{
        Timber.i("deviceMapDiscovery")
        val query = DiscoveryMessageDto(
            DiscoveryMessageDto.DiscoveryType.getMinusMap,
            GeneralDiscoveryDto(
                PersonalDiscoveryDto("user-1","idNumber-123","personalNumber-123"),
                SituationalDiscoveryDto(
                    bandwidth= deviceInfo.getBandwidthQuality()?.let { BigDecimal(it) },
                    time = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC),
                    operativeState = true,
                    power = deviceInfo.batteryPower().toBigDecimal(),
                    availableStorage = deviceInfo.getAvailableSpaceByPolicy().toString()
                ),
                PhysicalDiscoveryDto(
                    ID = deviceInfo.deviceId(),
                    OS = PhysicalDiscoveryDto.OSEnum.android,
                    serialNumber = deviceInfo.generatedDeviceId(),
                    possibleBandwidth = "Yes"
                )
            ),
            DiscoverySoftwareDto("cellular-device", PlatformDto("olar","1", BigDecimal("0"),
                emptyList())
            ),
            DiscoveryMapDto("dummy product","no-name","3","osm","bla-bla",
                "1,2,3,4",
                "WGS84", LocalDateTime.now().toString(), LocalDateTime.now().toString(), LocalDateTime.now().toString(),
                "DJI Mavic","raster","N/A","ME","CCD","3.14","0.12"
            )
        )

        Timber.v("deviceMapDiscovery - discovery object built")

        val offering = client.deviceApi.discoveryControllerDeviceMapDiscovery(query)
        Timber.d("deviceMapDiscovery -  offering results: $offering ")

        return offering
    }
}