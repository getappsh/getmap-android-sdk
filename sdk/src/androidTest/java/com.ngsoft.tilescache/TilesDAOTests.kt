package com.ngsoft.tilescache

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.tilescache.dao.TilesDAO
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import com.ngsoft.tilescache.models.TilePkg
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TilesDAOTests {

    companion object {

        @JvmStatic
        private lateinit var dao: TilesDAO

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val db = TilesDatabase.connect(appContext)
            dao = db.tilesDao()
        }

    }


    @Test
    fun addTiles() {

        for (i in 1..500){
            dao.insertAll(
                TilePkg(
                    "my product #3", "prod245.gpkg",
                    Tile(2314,3145, 16),
                    BBox(1.11,1.21, 2.11,2.21),
                    LocalDateTime.now().minusDays(18L),
                    LocalDateTime.now().minusDays(10L),
                    LocalDateTime.now()
                ),
                TilePkg(
                    "my product #30", "prod215.gpkg",
                    Tile(2314,3145, 16),
                    BBox(1.11,1.21, 2.11,2.21),
                    LocalDateTime.now().minusDays(18L),
                    LocalDateTime.now().minusDays(10L),
                    LocalDateTime.now()
                ),
                TilePkg(
                    "my product #31", "prod246.gpkg",
                    Tile(2315,3146, 16),
                    BBox(1.12,1.212, 2.112,2.212),
                    LocalDateTime.now().minusDays(15L),
                    LocalDateTime.now().minusDays(10L),
                    LocalDateTime.now()
                ),
                TilePkg(
                    "my product #41", "prod546.gpkg",
                    Tile(2315,3146, 16),
                    BBox(1.12,1.212, 2.112,2.212),
                    LocalDateTime.now().minusDays(15L),
                    LocalDateTime.now().minusDays(10L),
                    LocalDateTime.now()
                )
            )

        }

    }

    @Test
    fun getTiles() {
        val tiles = dao.getAll()
        assert(tiles.isNotEmpty())

        tiles.forEach{ t -> println(t) }

    }

    @Test
    fun getByBBox() {
        val tiles =
            //dao.getByBBox(1.11,1.21, 2.11,2.21)
            dao.getByBBox(BBox(1.11,1.21, 2.11,2.21))

        assert(tiles.isNotEmpty())

        tiles.forEach{ t -> println(t) }
    }

    @Test
    fun getByTile() {
        val tiles =
            //dao.getByTile(2315,3146, 16)
            dao.getByTile(Tile(2315,3146, 16))

        assert(tiles.isNotEmpty())

        tiles.forEach{ t -> println(t) }
    }

}