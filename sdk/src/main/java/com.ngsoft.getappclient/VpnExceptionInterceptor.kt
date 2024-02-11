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
            if (io.message.toString().lowercase().startsWith("unable to resolve host")) {
//                TODO find a way to get it from string resources
                ex = IOException("ודא שה-VPN פועל", io)
            }
            throw ex
        }
    }
}