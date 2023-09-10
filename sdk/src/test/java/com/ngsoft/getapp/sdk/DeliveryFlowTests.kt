package com.ngsoft.getapp.sdk

import com.ngsoft.sharedtest.DeliveryTests
import com.ngsoft.sharedtest.DiscoveryTests
import org.junit.Test

class DeliveryFlowTests {

    @Test
    fun testDeliveryStart() {
        DeliveryTests().testDeliveryStart(
            "1150410442175152128"
        )
    }

    @Test
    fun testDeliveryStatus() {
        DeliveryTests().testDeliveryStatus(
            "1150410442175152128"
        )
    }

}