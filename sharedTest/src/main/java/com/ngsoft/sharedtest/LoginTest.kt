package com.ngsoft.sharedtest

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.TokensDto
import GetApp.Client.models.UserLoginDto

class LoginTest {
    fun login2GetApp() {
        val loginApi = LoginApi(Settings.baseUrl)
        val cred = UserLoginDto(Settings.user, Settings.password)
        val tokens: TokensDto = loginApi.loginControllerGetToken(cred)
        assert(!tokens.accessToken.isNullOrEmpty())
        assert(!tokens.refreshToken.isNullOrEmpty())
        println("access:\n${tokens.accessToken}\nrefresh:\n${tokens.refreshToken}")
    }

}