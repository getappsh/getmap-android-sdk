package com.ngsoft.tilescache

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TilesCacheTests {

    companion object {
        private const val prodID = "prod #01"
        private const val tileX = 248
        private const val tileY = 458
        private const val tileZoom = 12
        private val updateDate = LocalDateTime.of(2023, 10, 1, 1, 2, 3 )

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
    fun a_registerTile() {
        cache.purge()
        cache.registerTilePkg(
            prodID,"dummy-tile.gpkg",
            Tile(tileX, tileY, tileZoom),
            BBox(1.1, 2.2, 3.3, 4.4),
            updateDate
        )
    }

    @Test
    fun b_isTileInCache() {
        val ret = cache.isTileInCache(prodID, tileX, tileY, tileZoom, updateDate)
        assert(ret)

        val tile = cache.getTileInCache(prodID, tileX, tileY, tileZoom)
        assert(tile != null)
        println("cached tile:")
        println(tile)
    }

    @Test
    fun c_updateTile() {
        val updFileName = "dummy-tile-22.gpkg"
        val updDate = updateDate.plusDays(5L)
        cache.registerTilePkg(
            prodID, updFileName,
            Tile(tileX, tileY, tileZoom),
            BBox(1.1, 2.2, 3.3, 4.4),
            updDate
        )

        val tile = cache.getTileInCache(prodID, tileX, tileY, tileZoom)
        assert(tile != null)
        assert(tile?.fileName == updFileName)
        assert(tile?.dateUpdated == updDate)

        println("updated tile:")
        println(tile)
    }

}