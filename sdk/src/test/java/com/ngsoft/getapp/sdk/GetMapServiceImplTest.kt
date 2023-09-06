package com.ngsoft.getapp.sdk

import com.ngsoft.getapp.sdk.models.MapDeployState
import com.ngsoft.getapp.sdk.models.MapImportState
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.getapp.sdk.models.StatusCode
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before


//keeps as guidelines and see what fits 4 my needs

class GetMapServiceImplTest {

    private lateinit var service: GetMapServiceImpl

    @Before
    fun setUp() {
        println("Test setup...")
        val cfg = Configuration()
        cfg.baseUrl = "http://getapp-dev.getapp.sh:3000"
        cfg.user = "rony@example.com"
        cfg.password = "rony123"
        service = GetMapServiceImpl(cfg)
    }

    @Test
    fun `test getCreateMapImportStatus with valid input`() {
        val result = service.getCreateMapImportStatus("testId")
        assertNotNull(result)
        assertEquals("testId", result?.importRequestId)
        assertEquals(StatusCode.SUCCESS, result?.statusCode?.statusCode)
        assertEquals(MapImportState.START, result?.state)
    }

    @Test
    fun `test getCreateMapImportStatus with null input`() {
        assertThrows(Exception::class.java) {
            service.getCreateMapImportStatus(null)
        }
    }

    @Test
    fun `test createMapImport with valid input`() {
        val inputProperties = MapProperties() // Assuming MapProperties has a default constructor
        val result = service.createMapImport(inputProperties)
        assertNotNull(result)
        assertEquals(StatusCode.SUCCESS, result?.statusCode?.statusCode)
        assertEquals(MapImportState.IN_PROGRESS, result?.state)
    }

    @Test
    fun `test createMapImport with null input`() {
        assertThrows(Exception::class.java) {
            service.createMapImport(null)
        }
    }

    @Test
    fun `test setMapImportDeploy with valid input`() {
        val result = service.setMapImportDeploy("testId", MapDeployState.DONE)
        assertNotNull(result)
        assertEquals(MapDeployState.DONE, result)
    }

    @Test
    fun `test setMapImportDeploy with null inputRequestId`() {
        assertThrows(Exception::class.java) {
            service.setMapImportDeploy(null, MapDeployState.DONE)
        }
    }

    // TODO Add tests for other edge cases and scenarios

}