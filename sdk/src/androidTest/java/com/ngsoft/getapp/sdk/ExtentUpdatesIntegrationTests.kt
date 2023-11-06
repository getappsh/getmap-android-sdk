package com.ngsoft.getapp.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngsoft.getapp.sdk.models.MapProperties
import com.ngsoft.tilescache.TilesCache
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.LocalDateTime

private const val PROD_NAME = "prod~test"
private const val TEST_BBOX = "34.73647075,31.94368473,34.74949962,31.95388123"

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExtentUpdatesIntegrationTests {
    companion object {
        private lateinit var extentUpdates: ExtentUpdates
        private lateinit var cache: TilesCache
        private val updateDate = LocalDateTime.of(2023, 10, 1, 1, 2, 3 )

        @BeforeClass
        @JvmStatic
        fun setup() {
            println("Test setup...")
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            extentUpdates = ExtentUpdates(appContext)
            cache = TilesCache(appContext)
        }
    }


    @Test
    fun a_InitCache(){
        cache.purge()

        for (i in 1..100){
            cache.registerTilePkg("prod~$i","dummy-pkg.gpkg",
                Tile(1 + i,2 + i,16),
                BBox(1.1, 2.2, 3.3, 4.4),
                LocalDateTime.of(2023, 9, 2, 1, 2, 3 )
            )
        }

        //matrix tiles for "34.73647075,31.94368473,34.74949962,31.95388123" zoom 16

//        result[0]: Tile(x=78183, y=21133, zoom=16) | BBox(left=34.736022949218295, bottom=31.95373535156262, right=34.738769531249545, top=31.95648193359387)
//        result[1]: Tile(x=78183, y=21134, zoom=16) | BBox(left=34.736022949218295, bottom=31.95098876953137, right=34.738769531249545, top=31.95373535156262)
//        result[2]: Tile(x=78183, y=21135, zoom=16) | BBox(left=34.736022949218295, bottom=31.94824218750012, right=34.738769531249545, top=31.95098876953137)
//        result[3]: Tile(x=78183, y=21136, zoom=16) | BBox(left=34.736022949218295, bottom=31.94549560546887, right=34.738769531249545, top=31.94824218750012)
//        result[4]: Tile(x=78183, y=21137, zoom=16) | BBox(left=34.736022949218295, bottom=31.94274902343762, right=34.738769531249545, top=31.94549560546887)
//        result[5]: Tile(x=78184, y=21133, zoom=16) | BBox(left=34.738769531249545, bottom=31.95373535156262, right=34.741516113280795, top=31.95648193359387)
//        result[6]: Tile(x=78184, y=21134, zoom=16) | BBox(left=34.738769531249545, bottom=31.95098876953137, right=34.741516113280795, top=31.95373535156262)
//        result[7]: Tile(x=78184, y=21135, zoom=16) | BBox(left=34.738769531249545, bottom=31.94824218750012, right=34.741516113280795, top=31.95098876953137)
//        result[8]: Tile(x=78184, y=21136, zoom=16) | BBox(left=34.738769531249545, bottom=31.94549560546887, right=34.741516113280795, top=31.94824218750012)
//        result[9]: Tile(x=78184, y=21137, zoom=16) | BBox(left=34.738769531249545, bottom=31.94274902343762, right=34.741516113280795, top=31.94549560546887)
//        result[10]: Tile(x=78185, y=21133, zoom=16) | BBox(left=34.741516113280795, bottom=31.95373535156262, right=34.744262695312045, top=31.95648193359387)
//        result[11]: Tile(x=78185, y=21134, zoom=16) | BBox(left=34.741516113280795, bottom=31.95098876953137, right=34.744262695312045, top=31.95373535156262)
//        result[12]: Tile(x=78185, y=21135, zoom=16) | BBox(left=34.741516113280795, bottom=31.94824218750012, right=34.744262695312045, top=31.95098876953137)
//        result[13]: Tile(x=78185, y=21136, zoom=16) | BBox(left=34.741516113280795, bottom=31.94549560546887, right=34.744262695312045, top=31.94824218750012)
//        result[14]: Tile(x=78185, y=21137, zoom=16) | BBox(left=34.741516113280795, bottom=31.94274902343762, right=34.744262695312045, top=31.94549560546887)
//        result[15]: Tile(x=78186, y=21133, zoom=16) | BBox(left=34.744262695312045, bottom=31.95373535156262, right=34.747009277343295, top=31.95648193359387)
//        result[16]: Tile(x=78186, y=21134, zoom=16) | BBox(left=34.744262695312045, bottom=31.95098876953137, right=34.747009277343295, top=31.95373535156262)
//        result[17]: Tile(x=78186, y=21135, zoom=16) | BBox(left=34.744262695312045, bottom=31.94824218750012, right=34.747009277343295, top=31.95098876953137)
//        result[18]: Tile(x=78186, y=21136, zoom=16) | BBox(left=34.744262695312045, bottom=31.94549560546887, right=34.747009277343295, top=31.94824218750012)
//        result[19]: Tile(x=78186, y=21137, zoom=16) | BBox(left=34.744262695312045, bottom=31.94274902343762, right=34.747009277343295, top=31.94549560546887)
//        result[20]: Tile(x=78187, y=21133, zoom=16) | BBox(left=34.747009277343295, bottom=31.95373535156262, right=34.749755859374545, top=31.95648193359387)
//        result[21]: Tile(x=78187, y=21134, zoom=16) | BBox(left=34.747009277343295, bottom=31.95098876953137, right=34.749755859374545, top=31.95373535156262)
//        result[22]: Tile(x=78187, y=21135, zoom=16) | BBox(left=34.747009277343295, bottom=31.94824218750012, right=34.749755859374545, top=31.95098876953137)
//        result[23]: Tile(x=78187, y=21136, zoom=16) | BBox(left=34.747009277343295, bottom=31.94549560546887, right=34.749755859374545, top=31.94824218750012)
//        result[24]: Tile(x=78187, y=21137, zoom=16) | BBox(left=34.747009277343295, bottom=31.94274902343762, right=34.749755859374545, top=31.94549560546887)


    }

    @Test
    fun b_ShouldBe_1_Tile_InCache() {

        cache.registerTilePkg(PROD_NAME,"dummy-pkg-1.gpkg",
            Tile(x=78186, y=21134, zoom=16),
            BBox(left=34.744262695312045, bottom=31.95098876953137, right=34.747009277343295, top=31.95373535156262),
            updateDate
        )

        val mapProps = MapProperties(PROD_NAME, TEST_BBOX,false)
        val updates = extentUpdates.getExtentUpdates(mapProps, 16, updateDate)

        assert(updates.isNotEmpty())

        val updatesCount = updates.count()
        println("got count = $updatesCount")

        assert(updatesCount == 24)

        updates.forEach{
            println(it)
        }

    }

    @Test
    fun c_ShouldBe_4_Tiles_InCache() {

        cache.registerTilePkg(PROD_NAME,"dummy-pkg-2.gpkg",
            Tile(x=78185, y=21137, zoom=16),
            BBox(left=34.741516113280795, bottom=31.94274902343762, right=34.744262695312045, top=31.94549560546887),
            updateDate
        )

        cache.registerTilePkg(PROD_NAME,"dummy-pkg-3.gpkg",
            Tile(x=78186, y=21133, zoom=16),
            BBox(left=34.744262695312045, bottom=31.95373535156262, right=34.747009277343295, top=31.95648193359387),
            updateDate
        )

        cache.registerTilePkg(PROD_NAME,"dummy-pkg-4.gpkg",
            Tile(x=78186, y=21135, zoom=16),
            BBox(left=34.744262695312045, bottom=31.94824218750012, right=34.747009277343295, top=31.95098876953137),
            updateDate
        )

        val mapProps = MapProperties(PROD_NAME, TEST_BBOX,false)
        val updates = extentUpdates.getExtentUpdates(mapProps, 16, updateDate)

        assert(updates.isNotEmpty())

        val updatesCount = updates.count()
        println("got count = $updatesCount")

        assert(updatesCount == 21)

        updates.forEach{
            println(it)
        }

    }

}