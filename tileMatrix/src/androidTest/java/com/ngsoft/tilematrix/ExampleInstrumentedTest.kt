package com.ngsoft.tilematrix

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ngsoft.tilematrix.test", appContext.packageName)

        val matrixGrid = TileMatrix(appContext)
        val tile = matrixGrid.getTile(34.65699535,31.77978378,12)

        assert(tile.isNotEmpty())
        println(tile)

        val bboxes = matrixGrid.getBBoxes(34.73647075, 31.94368473,
            34.74949962, 31.95388123, 16)

        assert(bboxes.isNotEmpty())
        println(bboxes)

    }
}