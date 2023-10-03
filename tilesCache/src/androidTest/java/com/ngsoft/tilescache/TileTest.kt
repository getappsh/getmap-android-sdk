package com.ngsoft.tilescache

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileTest {

    companion object {

        @JvmStatic
        private lateinit var dao: TilesDAO

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext

            val db = TilesDatabase.connect(appContext)

//                Room.databaseBuilder(appContext, TilesDatabase::class.java, "tiles-DB")
//                .fallbackToDestructiveMigration()
//                .build()

            dao = db.tilesDao()
        }

    }


    @Test
    fun addTiles() {

        dao.insertAll(
            TilePkg(
                "my product #3", "prod24.gpkg",
                BBox(1.11,1.21, 2.11,2.21),
                12
            )
        )

    }

    @Test
    fun getTiles() {
        val tiles = dao.getAll()
        assert(tiles.isNotEmpty())

        tiles.forEach{ t-> println(t) }

    }

}