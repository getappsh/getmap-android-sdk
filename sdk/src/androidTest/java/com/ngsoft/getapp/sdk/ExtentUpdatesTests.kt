package com.ngsoft.getapp.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapProperties
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ExtentUpdatesTests {
    companion object {

        private lateinit var extentUpdates: ExtentUpdates
        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext

            extentUpdates = ExtentUpdates(appContext)
        }
    }

    @Test
    fun extentUpdTest1() {
        val mapProps = MapProperties("prod #01",
            "34.73647075,31.94368473,34.74949962, 31.95388123",
            false)
        val updateDate = LocalDateTime.of(2023, 10, 1, 1, 2, 3 )
        val updates = extentUpdates.getExtentUpdates(mapProps, 16, updateDate)

        assert(updates.isNotEmpty())

        updates.forEach{
            println(it)
        }

    }

}