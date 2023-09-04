package com.ngsoft.getapp.sdk

import GetApp.Client.apis.LoginApi
import GetApp.Client.infrastructure.ClientException
import GetApp.Client.models.ComponentDto
import GetApp.Client.models.DiscoveryMapDto
import GetApp.Client.models.DiscoveryMessageDto
import GetApp.Client.models.DiscoverySoftwareDto
import GetApp.Client.models.GeneralDiscoveryDto
import GetApp.Client.models.GeoLocationDto
import GetApp.Client.models.PersonalDiscoveryDto
import GetApp.Client.models.PhysicalDiscoveryDto
import GetApp.Client.models.PlatformDto
import GetApp.Client.models.SituationalDiscoveryDto
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DiscoveryFlowTests : TestBase() {

    @Test
    fun testDiscoveryCatalog() {


        val query = DiscoveryMessageDto(DiscoveryMessageDto.DiscoveryType.app,
            GeneralDiscoveryDto(
                PersonalDiscoveryDto("tank","idNumber-123","personalNumber-123"),
                SituationalDiscoveryDto(BigDecimal("23"), BigDecimal("2"),
                    OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC), true, BigDecimal("34"),
                    GeoLocationDto("33.4","23.3", "344")
                    ),
                PhysicalDiscoveryDto(PhysicalDiscoveryDto.OSEnum.android, "00-B0-D0-63-C2-26","129.2.3.4",
                    "4", "13kb23", "12kb", "1212Mb")
            ),
            DiscoverySoftwareDto("yatush", PlatformDto("Merkava","106", BigDecimal("223"),
                listOf(ComponentDto(
                    "dummyCatId", "somename", "11", "N/A", BigDecimal("22"), "N/A"
                    ), ComponentDto(
                    "dummyCatId", "somename", "22", "N/A", BigDecimal("33"), "N/A"
                    ))
                )
            ),

            DiscoveryMapDto("if32","map4","3","osm","bla-bla",
                "1.1,2.2,3.3,4.4","WGS84", LocalDateTime.now().toString(), LocalDateTime.now().toString(), LocalDateTime.now().toString(),
                "DJI Mavic","raster","N/A","ME","CCD","3.14","0.12"
            )
        )

        val ret = api.getDiscoveryCatalog(query)

        ret.map?.forEach {
            println(it)
        }


//        try {
//            val ret = api.getDiscoveryCatalog(query)
//            println(ret)
//        } catch (e: ClientException){
//            println(e.message)
//            println(e.response.toString())
//        }
    }

//    @Test
//    fun login_isOk() {
//        val configuration = Configuration()
//        configuration.baseUrl = "http://getapp-dev.getapp.sh:3000"
//        configuration.user = "rony@example.com"
//        configuration.password = "rony123"
//
//        val loginApi = LoginApi(configuration.baseUrl ?: "localhost")
//        val cred = UserLoginDto(
//            configuration.user ?: "user",
//            configuration.password ?: "password"
//        )
//
//        val tokens: TokensDto = loginApi.loginControllerGetToken(cred)
//
//        assert(!tokens.accessToken.isNullOrEmpty())
//        println("\naccessToken:\n${tokens.accessToken}\n")
//    }

}