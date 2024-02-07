package com.ngsoft.getappclient

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal class VpnExceptionInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            return chain.proceed(chain.request())
        } catch (io: IOException) {
//            TODO it's not good to check vpn error by string compare
            var ex = io
            if (io.message.toString().startsWith("failed to connect to")) {
//                TODO find a way to get it from string resources
                ex = IOException("נסה להדליק את ה-VPN", io)
            }
            throw ex
        }
    }
}