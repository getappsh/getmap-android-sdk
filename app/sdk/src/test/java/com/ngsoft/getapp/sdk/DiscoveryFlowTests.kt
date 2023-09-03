package com.ngsoft.getapp.sdk

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import org.junit.Test

class DiscoveryFlowTests : TestBase() {

    @Test
    fun login_isOk() {
        val configuration = Configuration()
        configuration.baseUrl = "http://getapp-dev.getapp.sh:3000"
        configuration.user = "rony@example.com"
        configuration.password = "rony123"

        val loginApi = LoginApi(configuration.baseUrl ?: "localhost")
        val cred = UserLoginDto(
            configuration.user ?: "user",
            configuration.password ?: "password"
        )

        val tokens: TokensDto = loginApi.loginControllerGetToken(cred)

        assert(!tokens.accessToken.isNullOrEmpty())
        println("\naccessToken:\n${tokens.accessToken}\n")
    }

}