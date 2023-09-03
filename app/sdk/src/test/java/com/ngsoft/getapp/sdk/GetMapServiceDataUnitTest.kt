package com.ngsoft.getapp.sdk

import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.StatusCode
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */


class GetMapServiceDataUnitTest {
    companion object {
        init {
            println("init...")
        }

        private lateinit var api: GetMapService

        @BeforeClass @JvmStatic
        fun setup() {
            println("Test setup...")
            val cfg = Configuration()
            cfg.baseUrl = "http://getapp-dev.getapp.sh:3000"
            cfg.user = "rony@example.com"
            cfg.password = "rony123"

            api = GetMapServiceImpl(cfg)
        }

        @AfterClass @JvmStatic fun teardown() {
            println("Test teardown...")
        }
    }

    @Before fun prepareTest() {
        println("Test prepare...")
    }

    @After fun cleanupTest() {
        println("Test cleanup...")
    }

    @Test
    fun createMapImportStatus_isCorrect() {
        val reqId = "req-1234"
        val ret = api.getCreateMapImportStatus(reqId)

        assert(ret != null)
        assertEquals(reqId, ret?.importRequestId ?: "" )
        assertEquals(StatusCode.SUCCESS, ret!!.statusCode!!.statusCode )
    }

    @Test
    fun createMapImportStatus_shouldThrowOnInvalidReqId() {
        assertThrows(Exception::class.java
        ) {
            api.getCreateMapImportStatus("")
        }
    }

    @Test
    fun getMapImportDeliveryStatus_isCorrect() {
        val reqId = "req-1234"
        val ret = api.getMapImportDeliveryStatus(reqId)

        assert(ret != null)
        assertEquals(reqId, ret?.importRequestId ?: "" )
        assertEquals(MapDeliveryState.CONTINUE, ret!!.state )
        assertEquals(StatusCode.SUCCESS, ret.message!!.statusCode)
    }

}