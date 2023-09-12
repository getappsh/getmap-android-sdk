package com.ngsoft.getapp.sdk

import com.ngsoft.sharedtest.ImportTests
import org.junit.Test

class ImportFlowTests {

    @Test
    fun testCreateImport(){
        ImportTests().testCreateImport()
    }

    @Test
    fun testImportStatus() {
        //values from my local VM's DB, that test is "interactive" one
        val requestId =
            //"1150781481702916096"

            //libot
            //"1150790646945021952"
            "1150831395237527552"

        ImportTests().testImportStatus(requestId)

    }

}