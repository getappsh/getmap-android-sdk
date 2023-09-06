package com.ngsoft.getapp.sdk

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto
import org.junit.Test

class SdkTests {
    @Test
    fun login_ShouldSuccess() {
        val configuration = Configuration(
    "http://getapp-dev.getapp.sh:3000",
    "rony@example.com",
    "rony123",
    "todo"
        )

        val loginApi = LoginApi(configuration.baseUrl)
        val cred = UserLoginDto(
            configuration.user,
            configuration.password
        )

        val tokens: TokensDto = loginApi.loginControllerGetToken(cred)

        assert(!tokens.accessToken.isNullOrEmpty())
        println("\naccessToken:\n${tokens.accessToken}\n")
    }


}