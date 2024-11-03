package com.ngsoft.getapp.sdk.helpers.client

import com.ngsoft.getappclient.GetAppClient
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal object IMEIClient {


    fun getDeviceIMEI(serialNumber: String, client: GetAppClient): String? {
        Timber.i("Get device IMEI");
        for (i in 1..3) {
            try {
                val response = client.deviceApi.deviceControllerGetDeviceIMEI(serialNumber)
                return response.imei
            }catch (e: Exception){
                Timber.e("Get device IMEI failed, ${e}")
                if (i == 3) return null
                Timber.i("Retry get device IMEI, ${i}")
                TimeUnit.SECONDS.sleep(3 * i.toLong())
            }
        }
        return null
    }
}