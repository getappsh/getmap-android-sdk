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
            "1150410442175152128"
        //"1145694428514484224"
        //"1145699481723863040"
        //"1145670037365850112"
        //"aaa"

        ImportTests().testImportStatus(requestId)

    }

}