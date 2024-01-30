package com.ngsoft.getappclient

import timber.log.Timber
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AccessTokenAuthenticator(
    private val tokenProvider: AccessTokenProvider
): Authenticator {

    private val TAG = "AccessTokenAuthenticator"
    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.e("authenticate response status: ${response.code}")
        // We need to have a token in order to refresh it.
        val token = tokenProvider.token()
        Timber.d("current token: $token")

        synchronized(this) {
            val newToken = tokenProvider.token()
            Timber.d("synchronized token: $newToken")

            // Check if the request made was previously made as an authenticated request.
            if (response.request.header("Authorization") != null) {

                // If the token has changed since the request was made, use the new token.
                if (newToken != token) {
                    return response.request
                        .newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $newToken")
                        .build()
                }

                val updatedToken = tokenProvider.refreshToken()
                Timber.d("updated token: $updatedToken")


                // Retry the request with the new token.
                return response.request
                    .newBuilder()
                    .header("Authorization", "Bearer $updatedToken")
                    .build()
            }
        }
        return null
    }
}