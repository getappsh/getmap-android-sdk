package com.ngsoft.tilematrix

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.BeforeClass

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TileMatrixTests {

    companion object {
        private lateinit var matrixGrid: TileMatrix

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            matrixGrid = TileMatrix(appContext)
        }
    }

    private val bBoxLeft = 34.73647075
    private val bBoxBottom = 31.94368473
    private val bBoxRight = 34.74949962
    private val bBoxTop = 31.95388123
    private val zoomLevel = 16

    @Test
    fun getTilesAndBBoxesTest() {

        val tilesNBoxes = matrixGrid.getTilesAndBBoxes(
            bBoxLeft,
            bBoxBottom,
            bBoxRight,
            bBoxTop,
            zoomLevel
        )

        assert(tilesNBoxes.isNotEmpty())

        tilesNBoxes.forEachIndexed {
                index,
                tileNBBox -> println("result[$index]: ${tileNBBox.first} | ${tileNBBox.second}" )
        }
    }

}