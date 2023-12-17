package com.ngsoft.getappclient

import GetApp.Client.apis.LoginApi
import GetApp.Client.models.UserLoginDto
import android.util.Log
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
        Log.d(TAG, "Login")
        val tokens = LoginApi(config.baseUrl).loginControllerGetToken(
            UserLoginDto(config.user, config.password)
        )
        currentToken = tokens.accessToken.toString()
        expiredAt = tokens.expireAt
        Log.d(TAG,"Logged in, access token = $currentToken")
        Log.d(TAG,"Logged in, token expire at = $expiredAt")

    }


    private fun isTokenExpired(): Boolean {
        return expiredAt?.let {
            val isExpired = OffsetDateTime.now() > it
            if (isExpired) {
                Log.i(TAG, "Token expired, expireAt = $it")
            }
            isExpired
        } ?: false
    }
}