package com.ngsoft.tilescache

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ngsoft.tilescache.models.TilePkg

@Dao
interface TilesDAO {
    @Query("SELECT * FROM TilePkg")
    fun getAll(): List<TilePkg>

//    @Query("SELECT * FROM TilePckg WHERE id IN (:tilesIds)")
//    fun loadAllByIds(userIds: IntArray): List<TilePckg>
//
//    @Query("SELECT * FROM TilePckg WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): TilePckg

    @Insert
    fun insertAll(vararg tiles: TilePkg)

    @Delete
    fun delete(tile: TilePkg)

}