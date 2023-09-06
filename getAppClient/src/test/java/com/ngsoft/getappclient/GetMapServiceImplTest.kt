package com.ngsoft.getapp.sdk

import org.junit.Test

import org.junit.Assert.*

class GetMapServiceImplTest {

    private lateinit var service: GetMapServiceImpl

    @BeforeEach
    fun setUp() {
        service = GetMapServiceImpl.instance!!
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