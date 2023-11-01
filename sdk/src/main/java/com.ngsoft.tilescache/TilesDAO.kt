package com.ngsoft.tilescache

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import com.ngsoft.tilescache.models.TilePkg
import com.ngsoft.tilescache.models.TilePkgUpdate


//see On @Query Functions here: https://commonsware.com/Room/pages/chap-dao-005.html

@Dao
interface TilesDAO {

    @Query("SELECT * FROM TilePkg")
    fun getAll(): List<TilePkg>

//    @Query("SELECT * FROM TilePckg WHERE id IN (:tilesIds)")
//    fun loadAllByIds(userIds: IntArray): List<TilePckg>
//
    @Query("SELECT * FROM TilePkg WHERE `left` = :left AND bottom = :bottom AND `right` = :right AND top = :top")
    fun getByBBox(left: Double, bottom: Double, right: Double, top: Double): List<TilePkg>

    fun getByBBox(bBox: BBox): List<TilePkg> {
        return getByBBox(bBox.left, bBox.bottom, bBox.right, bBox.top)
    }

    @Query("SELECT * FROM TilePkg WHERE x = :x AND y = :y AND `zoom` = :zoom")
    fun getByTile(x: Int, y: Int, zoom: Int): List<TilePkg>

    fun getByTile(tile: Tile): List<TilePkg>{
        return getByTile(tile.x, tile.y, tile.zoom)
    }

    @Insert
    fun insert(tile: TilePkg)

    @Insert
    fun insertAll(vararg tiles: TilePkg)

    @Update(entity = TilePkg::class)
    fun update(tile: TilePkgUpdate)

    @Delete
    fun delete(tile: TilePkg)

    @Query("DELETE FROM TilePkg")
    fun nukeTable()

}