package com.ngsoft.sharedtest

import com.ngsoft.getapp.sdk.models.MapProperties

class ImportTests : GetMapServiceTestBase() {
    fun testCreateImport(){

        val props = MapProperties(
            Settings.productId,
            Settings.productBBOX,
            false
        )

        val ret = api.createMapImport(props)
        assert(ret != null)
        assert(!ret?.importRequestId.isNullOrEmpty())

        println(ret?.importRequestId.toString())
        println(ret?.statusCode?.statusCode?.toString())
        println(ret?.state?.toString())

//        try {
//            val ret = api.createMapImport(props)
//            println(ret)
//        } catch (e: ClientException){
//            println(e.message)
//        }

    }

    fun testImportStatus(requestId: String) {

        val ret = api.getCreateMapImportStatus(requestId)

        assert(ret != null)
        assert(requestId == ret?.importRequestId)

        println(ret?.statusCode?.statusCode?.toString())
        println(ret?.state?.toString())

    }


}