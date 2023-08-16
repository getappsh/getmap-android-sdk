package com.ngsoft.getapp.getapp

import com.ngsoft.getapp.sdk.GetMapServiceImpl
import com.ngsoft.getapp.sdk.MapDeliveryState
import com.ngsoft.getapp.sdk.StatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */


class GetMapServiceDataUnitTest {

    @Test
    fun createMapImportStatus_isCorrect() {
        val reqId = "req-1234"
        val ret = GetMapServiceImpl.instance?.getCreateMapImportStatus(reqId)

        assert(ret != null)
        assertEquals(reqId, ret?.importRequestId ?: "" )
        assertEquals(StatusCode.SUCCESS, ret!!.statusCode!!.statusCode )
    }

    @Test
    fun createMapImportStatus_shouldThrowOnInvalidReqId() {
        assertThrows(Exception::class.java
        ) {
            GetMapServiceImpl.instance?.getCreateMapImportStatus("")
        }
    }

    @Test
    fun getMapImportDeliveryStatus_isCorrect() {
        val reqId = "req-1234"
        val ret = GetMapServiceImpl.instance?.getMapImportDeliveryStatus(reqId)

        assert(ret != null)
        assertEquals(reqId, ret?.importRequestId ?: "" )
        assertEquals(MapDeliveryState.CONTINUE, ret!!.state )
        assertEquals(StatusCode.SUCCESS, ret.message!!.statusCode)
    }

}