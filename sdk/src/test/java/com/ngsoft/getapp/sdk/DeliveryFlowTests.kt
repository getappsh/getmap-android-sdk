package com.ngsoft.getapp.sdk

import com.ngsoft.sharedtest.DeliveryTests
import org.junit.Test

class DeliveryFlowTests {

    private val requestId = "1140263853284655104"

    @Test
    fun testDeliveryStart() {
        DeliveryTests().testDeliveryStart(requestId)
    }

    @Test
    fun testDeliveryStatus() {
        DeliveryTests().testDeliveryStatus(requestId)
    }

}