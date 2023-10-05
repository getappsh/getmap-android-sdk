package com.ngsoft.tilescache

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import com.ngsoft.tilescache.models.TilePkg
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)

class TilesCacheTests {
    companion object {

        @JvmStatic
        private lateinit var cache: TilesCache

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            cache = TilesCache(appContext)
        }

    }


    @Test
    fun registerTile() {
        cache.registerTilePkg(
            TilePkg(
                "prod #1", "pkg54.gpkg",
                Tile(23, 45, 14),
                BBox(1.1, 2.2, 3.3, 4.4),
                LocalDateTime.now().minusDays(18L),
                LocalDateTime.now()
            )
        )
    }

}