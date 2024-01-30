package com.ngsoft.getappclient

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.UserLoginDto
import timber.log.Timber
import java.time.OffsetDateTime

class AccessTokenProvider constructor(private val config: ConnectionConfig) {

    private val TAG = "AccessTokenProvider"

    private var currentToken: String = ""
    private var expiredAt: OffsetDateTime? = null

    fun token(): String {
        synchronized(this){
            if (currentToken.isEmpty() || isTokenExpired() ) {
                login()
            }
        }
        return currentToken
    }

    fun refreshToken(): String {
        login()
        return currentToken
    }
    private fun login(){
        Timber.d("Login")
        val tokens = LoginApi(config.baseUrl).loginControllerGetToken(
            UserLoginDto(config.user, config.password)
        )
        currentToken = tokens.accessToken.toString()
        expiredAt = tokens.expireAt
        Timber.d("Logged in, access token = $currentToken")
        Timber.d("Logged in, token expire at = $expiredAt")

    }


    private fun isTokenExpired(): Boolean {
        return expiredAt?.let {
            val isExpired = OffsetDateTime.now() > it
            if (isExpired) {
                Timber.i("Token expired, expireAt = $it")
            }
            isExpired
        } ?: false
    }
}