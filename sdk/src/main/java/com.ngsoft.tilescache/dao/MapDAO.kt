package com.ngsoft.tilescache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

import com.ngsoft.tilescache.models.MapPkg

@Dao
interface MapDAO {
    @Query("SELECT * FROM MapPkg")
    fun getAll(): List<MapPkg>

    @Query("SELECT * FROM MapPkg WHERE id = :id")
    fun getById(id: Int): MapPkg?

    @Insert
    fun insert(map: MapPkg): Long

    @Update
    fun update(mapPkg: MapPkg)

    @Query("DELETE FROM MapPkg WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT EXISTS (SELECT 1 FROM MapPkg WHERE fileName = :name)")
    fun doesMapFileExist(name: String): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM MapPkg WHERE jsonName = :name)")
    fun doesJsonFileExist(name: String): Boolean

    @Query("DELETE FROM MapPkg")
    fun nukeTable()
}