package com.ngsoft.sharedtest

class DeliveryTests : GetMapServiceTestBase() {

    fun testDeliveryStart(requestId: String) {

        val ret = service.setMapImportDeliveryStart(requestId)

        assert(ret != null)
        assert(requestId == ret?.importRequestId)

        println(ret?.state?.toString())
    }

    fun testDeliveryStatus(requestId: String) {

        val ret = service.getMapImportDeliveryStatus(requestId)

        assert(ret != null)
        assert(requestId == ret?.importRequestId)

        println(ret?.state?.toString())

//        try {
//            val ret = DefaultGetMapService.getInstance().getMapImportDeliveryStatus(requestId)
//            println(ret)
//        } catch (e: ClientException){
//            println(e.message)
//        }

    }

}