package com.ngsoft.getappclient

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.UserLoginDto
import android.util.Log

class AccessTokenProvider constructor(private val config: ConnectionConfig) {

    private val TAG = "AccessTokenProvider"

    private var currentToken: String = ""

    fun token(): String {
        if (currentToken.isEmpty() ) {
            login()
        }
        return currentToken
    }

    fun refreshToken(): String {
        login()
        return currentToken
    }
    private fun login(){
        val tokens = LoginApi(config.baseUrl).loginControllerGetToken(
            UserLoginDto(config.user, config.password)
        )
        currentToken = tokens.accessToken.toString()
        Log.i(TAG,"Logged in, access token = $currentToken")

    }
}