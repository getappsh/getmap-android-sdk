package com.ngsoft.sharedtest

import com.ngsoft.getapp.sdk.models.MapProperties

class DiscoveryTests : GetMapServiceTestBase() {
    fun testDiscoveryCatalog(){

        println("Executing method ${object{}.javaClass.enclosingMethod?.name}")

        val props = MapProperties(
            "dummy product",
            "1,2,3,4",
            false
        )

        val ret = api.getDiscoveryCatalog(props)

        println("Got discovery items:")
        ret.forEach {
            println(it)
        }

// e.responce data in debugger is convenient to see what's GetApp service complains
//        try {
//            val ret = api.getDiscoveryCatalog(query)
//            println(ret)
//        } catch (e: ClientException){
//            println(e.message)
//        }

    }
}